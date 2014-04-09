package com.vco;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.StringUtils;

import model.BatchLog;
import model.BatchLogDtl;
import model.JobDefinition;
import model.JobStepsXref;
import model.BatchLogOkDtl;
import model.Step;

public class ExtractDBStep extends StepManager {

	private int record_pointer = 0;
	private BatchLogDtl log_dtl = null;
	
	private int recordsSoFar = 0;
	private String raw_sql = "";
	private int col_count = 0;
	
	// vars specific to this type of step
	private int max_rec = 5000;  // default max_records for extraction, if none specified
	
	// Keep track of ok-dtl entries
	private List<BatchLogOkDtl> tempOkDtlList = new ArrayList<BatchLogOkDtl>();
	private Map<Integer,String> columnsToSkip;
	private List<BatchLogOkDtl> previousJobOkDtls = new ArrayList<BatchLogOkDtl>();
	private String previousRunMaxOk1,previousRunMinOk1;
	private String OK1AtEndOfCurrentPage = new String();
	private int currentRowNum = 0;
	private int rowsIncludedInJob = 0;
	
	public ExtractDBStep(JobManager jm, JobStepsXref jobStepXref) {
		this.job_manager = jm;
		this.jobStepXref = jobStepXref;
		
		// Use hard-coded default for max_rec, unless one is defined in the step config table
		max_rec = this.jobStepXref.getStep().getExtractMaxRec().intValue();
	}
	
	/**
	 * Basic workflow methods
	 */
	
	@Override
	public boolean init() {
		
		return true;
	}
	
	@Override
	public boolean start() {
		
		/**  Initialize all the vars **/
		
		// Set this step to running
		this.running = true;
		// Log the start of this step
		// this.logStart();
		// Get the raw sql to run
		this.raw_sql = this.jobStepXref.getStep().getExtractSql();
		String whereClause = new String();
		int commit_freq = this.jobStepXref.getStep().getExtractCommitFreq().intValue();
		ResultSet rs = null;
		// For an extract run, these values must be set
		String previousRunPK1, previousRunExtractSql;
		int previousRunMaxRec, previousRunMaxRecPerFile;
		BatchLog previousRunBatchLogHdr = null;
		int previousRunTotalRecordsExtracted;
		int totalRows = 0;
		
		/**
		 * Prepare to re-run of previous batch, based on batch_num  
		 *   - look up minOk1/maxOk1 from first run of batch (ie: batch_seq_nbr 1)
		 *   - look up total rows from previous job
		 *   - look up ok dtls from the run *before* the previous run
		 * 
		 */
		
		if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_Repeat) {
			
			// The user called -b 123, where 123 is a batch_num.  Look up the first run
			// for that batch Number (ie: with same batchNum, seqNbr=1)
			
			// get the initial run of this batch, and pull out the job config info we need
			TypedQuery<BatchLog> qryInitialRunForThisBatch = this.job_manager.db.createQuery(
					"SELECT log from BatchLog log WHERE log.batchNum = :batchNumber and log.batchSeqNbr = :batchSeq order by log.id asc", BatchLog.class);
			List<BatchLog> lstInitialRunForThisBatch = qryInitialRunForThisBatch
		    		.setParameter("batchNumber", this.job_manager.job_id)
		    		.setParameter("batchSeq", 1)
		    		.setMaxResults(1)
		    		.getResultList();
			if (lstInitialRunForThisBatch.size() == 0 ) {
				System.out.println("vbatch Error: Batch " + this.job_manager.job_id + " not found.");
				return false;
			}
			BatchLog batchLogForInitialRun = lstInitialRunForThisBatch.get(0);
			// Grab the batch_log_dtls so we can retrieve job settings used whent that job was run
			TypedQuery<BatchLogDtl> qryInitialRunLogDtl = this.job_manager.db.createQuery(
					"SELECT dtl from BatchLogDtl dtl WHERE dtl.batchLog = :batchLog order by dtl.id asc", BatchLogDtl.class);
			List<BatchLogDtl> lstInitialRunLogDtl = qryInitialRunLogDtl
		    		.setParameter("batchLog", batchLogForInitialRun)
		    		.getResultList();
			// Loop over log_dtls for this job, grabbing job parameters
			for (BatchLogDtl initialRunLogDtl : lstInitialRunLogDtl) {
				// Depending on step type, grab the job params needed to re-run this job
				if (initialRunLogDtl.getStepType().equals("Extract")) {
					// Get the vars we need for min/max to re-run this job with the same records
					previousRunMinOk1 = initialRunLogDtl.getMinOk1();
					previousRunMinOk1 = this.convertDateStringToAnotherDateString(previousRunMinOk1, "MM/d/yy H:mm:ss", "MM/dd/yyyy H:mm:ss");
					previousRunMaxOk1 = initialRunLogDtl.getMaxOk1();
					previousRunMaxOk1 = this.convertDateStringToAnotherDateString(previousRunMaxOk1, "MM/d/yy H:mm:ss", "MM/dd/yyyy H:mm:ss");
					totalRows = initialRunLogDtl.getNumRecords().intValue();
					previousRunExtractSql = initialRunLogDtl.getExtractSql();
				}
				else if (initialRunLogDtl.getStepType().equals("CSV")) {
					previousRunMaxRecPerFile = initialRunLogDtl.getExtractMaxRecsPerFile().intValue();
				}
			}

			// Todo: If available, get the ok-dtl records from the last successful run
			// of this job *prior* to the initial run of this batch
			JobDefinition jd = batchLogForInitialRun.getJobDefinition();
			// Find the most recent successful run for this jobDefinition prior to the initial run for this batch
			TypedQuery<BatchLog> qryRunBeforeThat = this.job_manager.db.createQuery(
					"SELECT log from BatchLog log WHERE log.jobDefinition = :jd AND log.status = :jobStatus AND log.id < :prevJobId ORDER BY log.id desc", BatchLog.class);
			List<BatchLog> lstRunBeforeThat = qryRunBeforeThat
		    		.setParameter("jd", jd)
		    		.setParameter("jobStatus", "Complete")
		    		.setParameter("prevJobId", batchLogForInitialRun.getId())
		    		.setMaxResults(1)
		    		.getResultList();
			
			if (lstRunBeforeThat.size() > 0 ) {
				previousRunBatchLogHdr = lstRunBeforeThat.get(0);
			}
			else {
				// No runs of this job found prior to the "previous" run
				previousRunBatchLogHdr = null;
			}

			// Add minOk1 and maxOk1 to the whereClause
			whereClause += " OK1 >= to_date('" + previousRunMinOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
			whereClause += " AND OK1 <= to_date('" + previousRunMaxOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
			
			// Remove trailing semicolon which is valid sql but confuses jdbc sometimes
			if (this.raw_sql.indexOf(";", this.raw_sql.length()-1) != -1) {
				this.raw_sql = this.raw_sql.substring(0, this.raw_sql.length()-1);
			}
			// replace the SQL TOKEN(s) ( /* where */ )
			int sqlTokensReplaced =  this.replaceSqlToken(this.raw_sql, whereClause); 
			if (sqlTokensReplaced == 0) {
				VBatchManager.log.debug(MessageFormat.format("[Extract] QUERY : {0}", this.raw_sql));
			}
			// Run the query
			try {
				rs = this.sqlQuery(this.raw_sql, totalRows+100);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  // limit query to max_rec rows
			
		}
		
		
		/**
		 * Run this logic if we're running a new job. 
		 * 
		 * The main goal here is to populate these vars:
		 *   - previousRunMinOk1 (if available)
		 *   - previousRunMaxOk1 (if available)
		 *   - totalRows (based on roll-back logic)
		 * 
		 */
		
		if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_New) {
			// Look for previous Complete runs of this particular Job
			TypedQuery<BatchLogDtl> query = this.job_manager.db.createQuery(
		        "SELECT dtl FROM BatchLogDtl dtl WHERE dtl.stepType = :stepType  and dtl.stepsId = :stepId "  // must match this step's type and id
				+ " and dtl.batchLog.orderNum = :orderNum "  // the job # passed in at command line
				+ " and dtl.batchLog.batchSeqNbr = 1 "  // only initial runs, no re-runs
				+ " and dtl.batchLog.status = :status" //  only runs that completed successfully
		        + " and dtl.maxOk1 is not null "  // must have max_ok1 value
		        + " order by dtl.id desc", BatchLogDtl.class);  // most recent match comes first
		    List<BatchLogDtl> dtls = query
		    		.setParameter("stepType", "Extract")
		    		.setParameter("stepId", (int) this.jobStepXref.getId())
		    		.setParameter("orderNum", this.jobStepXref.getJobDefinition().getOrderNum())
		    		.setParameter("status", BatchLog.statusComplete)
		    		.getResultList();
			
			// dtls.size()  is the number of previous executions of this step from log_dtl
		    
			// Remove trailing semicolon which is valid sql but confuses jdbc sometimes
			if (this.raw_sql.indexOf(";", this.raw_sql.length()-1) != -1) {
				this.raw_sql = this.raw_sql.substring(0, this.raw_sql.length()-1);
			}
			
			// If there's at least one previous run with valid maxOK1
			if (dtls.size() > 0) {
				BatchLogDtl lastRun = dtls.get(0);  // Most recent successful run of this job

				// Get maxOk1 from previous run
				previousRunMaxOk1 = lastRun.getMaxOk1();  // Oracle date in string format
				previousRunBatchLogHdr = lastRun.getBatchLog();
				// Since we're relying on JDBC to convert a date object to a string,
				// force it into the specific date format that Oracle will be able to use in
				// a query (for instance, forcing the year from 2-digit to 4-digit)
				previousRunMaxOk1 = this.convertDateStringToAnotherDateString(previousRunMaxOk1, "MM/dd/yy H:mm:ss", "MM/dd/yyyy H:mm:ss");
				
				// replace the /* where */ token with our dynamic where clause
				// however, make sure that if there isn't a WHERE clause that
				// we add that as well
				
				// Create a WHERE clause that starts after lastOk1
				whereClause = " OK1 >= to_date('" + previousRunMaxOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
				// replace the SQL TOKEN(s) ( /* where */ )
				int sqlTokensReplaced =  this.replaceSqlToken(this.raw_sql, whereClause); 
				if (sqlTokensReplaced == 0) {
					VBatchManager.log.debug(MessageFormat.format("[Extract] QUERY : {0}", this.raw_sql));
				}
			}
			
			// Get total_rows from the previous run (if set in record)
			float jobMaxRecs = this.jobStepXref.getStep().getExtractMaxRec().floatValue();
			if (jobMaxRecs > 0) {
//				double j = Math.floor(1.1 * jobMaxRecs);
//				totalRows = (int)j;
				totalRows = (int)jobMaxRecs;
			}
			else {
				totalRows = this.max_rec;
			}
			
			/**
			 * 
			 */
			
			// lastOk1 = '07-May-2013 07:15:21'
			int endRowsToSkip = 0;
			int rowCount = 0;

			System.out.println("[Extract] REWRITTEN QUERY: " + this.raw_sql);
			try {
				rs = this.sqlQuery(this.raw_sql, totalRows+100);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  // limit query to max_rec rows
		}
		
		// If available, store ok-dtls from previous job in this.previousJobOkDtls
		if (previousRunBatchLogHdr != null) {
			this.getPreviousJobOkDtls(previousRunBatchLogHdr);
		}
		
		/**
		 * Now that we have all variables needed, and the data in 
		 * this.rs, move forward through the records gathering the
		 * data and passing it to the next step.
		 */
		
		// Vars for keeping track of ok/pk values for each row
		
		String previousRowPK1Value = new String();
		String currentRowPK1Value  = new String();
		String previousRowOK1Value = new String();
		String currentRowOK1Value  = new String();
		String PK1AtEndOfCurrentPage = new String();
		
		try {
			// go back to beginning of recordset 
			boolean recordsetHasItems = rs.first();
			
			this.columnsToSkip = this.prepareSkipColumns(rs);
			
			// TODO:  Make sure our required columns are in the query:
			// ok1, pk1 [pk2-pk3]
			
			// Todo:  Check which of the OK1 and PK1-3 columns are present in the query
			
			// Iterate over all the rows of the ResultSet
			boolean endOfRecordset=false;
			boolean endOfPage = false;
			
			boolean isPageDataAlmostComplete  =false;
			boolean isPageDataComplete        =false;
			boolean isRecordsetAlmostComplete =false;
			boolean isRecordsetComplete       =false;
			boolean foundRecordThatEndPage    =false;
			boolean skipThisRecord            =false;
			
			if (recordsetHasItems) {
				// Process the records in this recordset
				while (!endOfRecordset) {
					this.currentRowNum++;
					skipThisRecord=false;
					currentRowOK1Value = this.convertDateFieldToString(rs, "OK1");
					currentRowPK1Value = rs.getString("PK1");
					boolean isLastRecord = rs.isLast();
					
					/** If this row was already in the previous job,
					 *  completely skip processing this row.
					 */
					
					if (isRowInPreviousRunOkDtl(rs)) {
						skipThisRecord=true;
					}
					else {
						this.rowsIncludedInJob++;  // Note: rownum starts at 1
					}
					/** Set flags that determine how to handle this row **/
					
					int jobMaxRecPerFile = this.job_manager.getMaxRecPerFile();
					if ( (rowsIncludedInJob != 0 ) && (rowsIncludedInJob % jobMaxRecPerFile == 0)) { 
						// If we hit this mark, but pageDataComplete is already set,
						// then we're still moving forward through records from a previous 
						// page when we hit this mark, so don't reset the PK1 marker
						if (!isPageDataAlmostComplete) {
							// Since this record marks the "end" of a set of records,
							// remember its PK1 value
							PK1AtEndOfCurrentPage = rs.getString("PK1");
						}
						isPageDataAlmostComplete=true;
					}
					
					// todo: Set isPageDataComplete
					if (isPageDataAlmostComplete && !isPageDataComplete) {
						boolean pk1IsTheSame = currentRowPK1Value.equals(PK1AtEndOfCurrentPage);
						boolean pk1IsBlank = StringUtils.isBlank(PK1AtEndOfCurrentPage);
						if ( !(pk1IsTheSame) && !(pk1IsBlank)) {
							isPageDataComplete=true;
							isPageDataAlmostComplete=false;  // reset until next time we hit commit_freq
						}
					}
					
					// todo: Set isRecordsetAlmostComplete
					if (rowsIncludedInJob > totalRows) {
						// For a re-run, we always stop when we reach totalRows
						if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_Repeat) {
							isRecordsetAlmostComplete=false;
							isRecordsetComplete = true;
						}
						else {
							isRecordsetAlmostComplete=true;
							if (isPageDataComplete) {
								isRecordsetComplete=true;
							}
						}
						
							
					}
					
					// In addition to the above checks, if we're at the end of the recordset,
					// force the "is..Complete" flags true so all data gets written
					
					// Ran out of records before reaching the job limit
					boolean queryExhausted = (isLastRecord);
					if (queryExhausted) {
						isPageDataAlmostComplete=false;
						isPageDataComplete=true;
						isRecordsetAlmostComplete=false;
						isRecordsetComplete=true;
						PK1AtEndOfCurrentPage = rs.getString("PK1");
						OK1AtEndOfCurrentPage = this.convertDateFieldToString(rs, "OK1");
					}
					
					
					// Todo: for above, only do the pk1-3 that are in the query
					
					
					/**
					 * Process this row
					 */
					
					/** 
					 * Handle full page of data (which might also be end
					 * of recordset)
					 */
					if (!skipThisRecord) {
						// If first row, log start of job
						if (rowsIncludedInJob==1) {
							this.logStart();
							this.log_dtl.setMinOk1(currentRowOK1Value);
						}
						// If last row, make sure this row gets persisted
//						if (isPageDataComplete && isRecordsetComplete) {
//							// Since this is the last page of the recordset, 
//							// make sure that last row gets saved as well
//							if (!skipThisRecord) {
//								processRowOfData(rs);
//							}
//						}
						if (isLastRecord) {
							if (!skipThisRecord) {
								processRowOfData(rs);
							}
						}
						
						if (isPageDataComplete) {
							// todo: Mark job started
							
							// todo: Start step log_dtl, if not started
							if (isRecordsetComplete) {
								this.log_dtl.setMaxOk1(currentRowOK1Value);
								this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob));
							}
							else {
								this.log_dtl.setMaxOk1(previousRowOK1Value);
								this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob - 1));
							}
							
							
							// Todo: If this is end of recordset, remove ok-dtl entries from 
							// previous pages
							if (isRecordsetComplete) {
								TypedQuery<BatchLogOkDtl> qryJobOkDtl = this.job_manager.db.createNamedQuery("BatchLogOkDtl.findByBatchLogId", BatchLogOkDtl.class);
								qryJobOkDtl.setParameter("batchLogId", this.job_manager.batch_log);	  
								List<BatchLogOkDtl> lstOkDtl = qryJobOkDtl.getResultList();
								for (BatchLogOkDtl oldDtl : lstOkDtl) {
									this.job_manager.db.remove(oldDtl);
								}
								
							}
							
							// Todo: roll backwards in this list of ok-dtls until pk1 changes
							ListIterator<BatchLogOkDtl> it = tempOkDtlList.listIterator(tempOkDtlList.size());
							boolean deleteAllRemaining = false;
							
							while (it.hasPrevious()) {
						    	BatchLogOkDtl tempOkDtl = it.previous();
						    	String rowOK1 =  tempOkDtl.getOk1();
						    	// Get the PK1 to compare this row's pk1 against,
						    	// to see if it has changed
						    	// Once the PK1 changes as we go backwards
						    	if (!(rowOK1.equals(OK1AtEndOfCurrentPage))) {
						    		deleteAllRemaining=true;
						    	}
						    	
						    	if (deleteAllRemaining) {
									// Todo: delete this entry from the array so it doesn't get persisted
									it.remove();
								}
						    	else {
						    		this.job_manager.db.persist(tempOkDtl);
						    	}
						    }
						    tempOkDtlList = new ArrayList<BatchLogOkDtl>();
						    
						    // todo: submit page of data
						    this.job_manager.submitPageOfData(this.dataPageOut, this);
							
							// todo: clear dataPageOut
							isPageDataComplete=false; 
							this.dataPageOut = new ArrayList<Object>();
							
							// Update logs for this step, since we just successfully wrote some data
							
							
							// We successfully saved some data, commit the logs for all steps and job manager
							this.job_manager.db.getTransaction().commit();
							// Start a new transaction
							this.job_manager.db.getTransaction().begin();
							
							// todo: Page completed, update logs
						}
						
						/**  
						 * Do these things for every row 
						 ***/
						
						if (!(isRecordsetComplete)) {
							// If this row was not in the ok-dtl log for previous run,
							// then add this data to the csv
							
							processRowOfData(rs);
							// Remember pk1/ok1 to compare to the next row
							previousRowPK1Value = rs.getString("PK1");
							previousRowOK1Value = this.convertDateFieldToString(rs, "OK1");
						}	
		
						
						
						/**
						 * We've sent the final page of data, now close out the step.
						 */
						if (isRecordsetComplete) {
							// todo: mark step completed
							// Send any remaining records to the next step
							if (this.dataPageOut.size() > 0) {
								// Mark this step as complete
								this.running=false;
								this.completed=true;
								this.failed=false;
								// Reset page data
								this.dataPageOut = new ArrayList<Object>();
							}
							// Update the log_dtl record for this step to show it's completed
							this.log_dtl.setMaxOk1(previousRowOK1Value);
							this.log_dtl.setEndDt(new Date());
							this.log_dtl.setStatus(BatchLog.statusComplete);
							if (!isLastRecord) {
								this.rowsIncludedInJob--;
							}
							this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob));
							this.job_manager.db.persist(this.log_dtl);
							// todo: break out of recordset while
							endOfRecordset=true;
						}
					}
					
					
					// Move to the next record (or abort if we're past the last row)
					if (rs.next() == false) {  // moved past the last record
						endOfRecordset=true;
						if (this.rowsIncludedInJob==0) {
							System.out.println("[Extract] Skipping job (no new records found).");
						}
					}		
					
					// todo: Is endOfRecordset
						// todo: Abandon remaining records
						// 
					
				}
			} // // end of recordset
			else {  // initial recordset had 0 entries
				System.out.println("[Extract] Aborting job (no records found).");
				this.running=false;
				this.failed=false;
				this.completed=true;
			}
			
			
			
			
			
		} catch (SQLException  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		// This step is done.  Clean up, write to logs,
		// and return control to JobManager.
		
		
		// TODO:  empty data variables
		// TODO:  log completion of this step
		
		return true;
		
	}

	
	/**
	 * Check whether the current row of the recordset
	 * is already listed in the previous Job's ok-dtl
	 * list.
	 * 
	 * @param rs
	 * @return
	 */
	private boolean isRowInPreviousRunOkDtl(ResultSet rs) {
		boolean retval = false;
		try {
			if (this.previousJobOkDtls.size() > 0 ) {
				// Get the ok1, pk1 from this row
				String rowPK1;
					rowPK1 = rs.getString("PK1");
				
				String rowPK2;
					rowPK2 = rs.getString("PK2");
				String rowOK1 = this.convertDateFieldToString(rs, "OK1");
				
				String previousJobOK1 = this.previousJobOkDtls.get(0).getOk1();
				String previousJobPK1 = this.previousJobOkDtls.get(0).getPk1().toString();
				for (BatchLogOkDtl okDtlEntry : this.previousJobOkDtls) {
					String thisPk1 = okDtlEntry.getPk1().toString();
					String thisPk2 = okDtlEntry.getPk2().toString();
					String thisOk1 = okDtlEntry.getOk1();
					if ( (thisPk1.equals(rowPK1))
							&& (thisPk2.equals(rowPK2))
							&& (thisOk1.equals(rowOK1))
							 ) {
						retval=true;
					}
				}
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    catch (ParseException e1) {
	    	// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// TODO Auto-generated method stub
		return retval;
	}
	
	/**
	 * Check whether the current row of the recordset
	 * is already listed in this job's temp ok-dtl list
	 * 
	 * @param rs
	 * @return
	 */
	private boolean isRowInTempOkDtl(ResultSet rs) {
		boolean retval = false;
		try {
			if (this.tempOkDtlList.size() > 0 ) {
				// Get the ok1, pk1 from this row
				String rowPK1;
					rowPK1 = rs.getString("PK1");
				
				String rowPK2;
					rowPK2 = rs.getString("PK2");
				String rowOK1 = this.convertDateFieldToString(rs, "OK1");
				
				String previousJobOK1 = this.tempOkDtlList.get(0).getOk1();
				String previousJobPK1 = this.tempOkDtlList.get(0).getPk1().toString();
				for (BatchLogOkDtl okDtlEntry : this.tempOkDtlList) {
					String thisPk1 = okDtlEntry.getPk1().toString();
					String thisPk2 = okDtlEntry.getPk2().toString();
					if ( (thisPk1.equals(rowPK1))
							&& (thisPk2.equals(rowPK2))
							
							 ) {
						retval=true;
					}
				}
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    catch (ParseException e1) {
	    	// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// TODO Auto-generated method stub
		return retval;
	}

	/**
	 * @param rs
	 * @throws SQLException
	 * @throws ParseException
	 */
	private void processRowOfData(ResultSet rs) throws SQLException,
			ParseException {
		
		/**
		 * Check to see if this row was in the previous run's ok-dtl
		 * list.  If so, we'll skip it.
		 */
		boolean skipThisRow = false;
		skipThisRow=isRowInPreviousRunOkDtl(rs);
		
		if (!skipThisRow) {
		
			// Add this row to dataPageOut, to be sent to the next step
			List<Object> rowdata = new ArrayList<Object>();
			for (int ci = 1; ci <= this.col_count; ci++) {
				if (this.columnsToSkip.containsKey(ci)) {  // is this column in columnsToSkip?
					// Do not export this column
				}
				else {
					// Add this column to the output data
					rowdata.add(rs.getString(ci));
				}
			}
			this.dataPageOut.add(rowdata);
			
			// log ok-dtl for every row (some will be deleted prior to log commit)
			// Unless we're repeating a previous job (ie: -b)
			if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_New) {
				// If this ok-dtl already exists (based on PK1-3) in this job's temp ok dtl 
				// list, don't try to write it 
				// because constraint will be violated, and because the record isn't 
				// necessarily a dup just because pk1-3 is dup
				if (!(isRowInTempOkDtl(rs))) {
					BatchLogOkDtl newOkDtl = new BatchLogOkDtl();
					newOkDtl.setBatchLog(this.job_manager.batch_log);
					newOkDtl.setOk1(this.convertDateFieldToString(rs, "OK1"));
					newOkDtl.setPk1(rs.getLong("PK1"));
					newOkDtl.setPk2(rs.getLong("PK2"));
					this.tempOkDtlList.add(newOkDtl);
					this.OK1AtEndOfCurrentPage = this.convertDateFieldToString(rs, "OK1");
				}
			}
		}
	}

	private int replaceSqlToken(String raw_sql, String tokenReplacement) {
		int tokensReplaced = 0;
		// Count instances of /* where */ token
		Pattern p = Pattern.compile("\\/\\* where \\*\\/");
		Matcher m = p.matcher(raw_sql.toLowerCase());
		int whereTokenCount = 0;
		while (m.find()) {
			whereTokenCount += 1;
		}
		// Replace all /* where */ tokens with the startClause
		if (whereTokenCount > 0 ) {
			this.raw_sql = raw_sql.replaceAll("/\\* where \\*/", " AND " + tokenReplacement);
			tokensReplaced++;
		}
		return tokensReplaced;
	}

	private Map<Integer,String> prepareSkipColumns(ResultSet rs) {
		Map<Integer, String> columnsToSkip = new HashMap<Integer,String>();
		try {
			// List of columns to suppress in output data
			List<String> namesOfColumnsToSkip = new ArrayList<String>();
			namesOfColumnsToSkip.add("pk1");
			namesOfColumnsToSkip.add("pk2");
			namesOfColumnsToSkip.add("pk3");
			namesOfColumnsToSkip.add("ok1");
			
			// Find the colNum/colName of each of the skip columns
			ResultSetMetaData meta = rs.getMetaData();
			this.col_count = meta.getColumnCount();
			String colType, colName;
			
			for (int colIdx = 1; colIdx <= col_count; colIdx++) {
				colType = "";
				colName = "";
				colType  = meta.getColumnTypeName(colIdx);
				colName = meta.getColumnName(colIdx);
				String j = meta.getColumnLabel(colIdx);
				if (namesOfColumnsToSkip.contains(colName.toLowerCase())) {
					columnsToSkip.put(colIdx,colName);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return columnsToSkip;
	}

	/**
	 * @param rs
	 * @param columnName TODO
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	private String convertDateFieldToString(ResultSet rs, String columnName) throws SQLException,
			ParseException {
		SimpleDateFormat incomingDateFormat  = new SimpleDateFormat("y-MM-d HH:mm:ss.S");
		SimpleDateFormat outgoingDateFormat = new SimpleDateFormat("MM/d/y H:mm:ss");
		String ds = rs.getString(columnName);
		//System.out.println("\t[Extract] Original String dt: " + ds);
		Date dt = incomingDateFormat.parse(ds);
		String newDs = outgoingDateFormat.format(dt);
		return newDs;
	}
	
	/** 
	 * Converts a date string into a new datestring, applying the newDateFormat to it
	 * @param originalDateString
	 * @param newDateFormat
	 * @return
	 */
	
	private String convertDateStringToAnotherDateString(String originalDateString, String incomingDateFormat, String outgoingDateFormat) {
		String newlyFormattedDateString = new String();
		SimpleDateFormat incomingDateFormatter  = new SimpleDateFormat(incomingDateFormat);
		SimpleDateFormat outgoingDateFormatter = new SimpleDateFormat(outgoingDateFormat);
		try {
			// create a new Date object from the original date string
			Date tempDateObject = incomingDateFormatter.parse(originalDateString);
			// convert that date to a string, formatted with outgoingDateFormat
			newlyFormattedDateString = outgoingDateFormatter.format(tempDateObject);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return newlyFormattedDateString;
		
	}
	
	@Override
	public boolean processPageOfData(List<Object> pageOfData) {
		
		return true;
	}
	
	@Override
	public boolean finish() {
		
		return true;
	}
	
	/**
	 * Logging
	 * 
	 */
	
	private void logStart() {
		if (!this.job_manager.db.getTransaction().isActive()) {
			this.job_manager.db.getTransaction().begin();
		}
		
		// Create entry in batch_log_dtl
		this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.job_manager.batch_log);
		
		String msg = "Step [" + this.jobStepXref.getStep().getType() 
				+ " : " + this.jobStepXref.getStep().getShortDesc()
				+ "]";
		this.log_dtl.setLongDesc(msg);
		this.log_dtl.setStepsId(this.jobStepXref.getId());
		this.log_dtl.setJobStepsXrefJobStepSeq(this.jobStepXref.getJobStepSeq());
		this.log_dtl.setStepsShortDesc(this.jobStepXref.getStep().getShortDesc());
		this.log_dtl.setStepType(this.jobStepXref.getStep().getType());
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus("Started");
		
		// Log the job settings used to run this job
		this.log_dtl.setExtractSql(this.jobStepXref.getStep().getExtractSql());
		this.log_dtl.setExtractMaxRecs(this.jobStepXref.getStep().getExtractMaxRec());
		this.log_dtl.setExtractCommitFreq(this.jobStepXref.getStep().getExtractCommitFreq());
		this.log_dtl.setClassPath(this.jobStepXref.getStep().getClassPath());
		// TODO:  job steps xref id
		this.log_dtl.setStepsId(this.jobStepXref.getStep().getId());
		
		// Commit log entry
		this.job_manager.db.persist(this.log_dtl);
//		this.job_manager.db.getTransaction().commit();
		
		System.out.println("\t" + msg);
	}
	
	// Finished page of data
	// Finished processing
	// Step complete
	
	
	/**
	 * Returns a ResultSet for a raw SQL query.
	 * @param sql
	 * @return
	 */
	private ResultSet sqlQuery(String sql) {
		ResultSet rs = null;
		try {
			rs = this.sqlQuery(sql, -1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
	
	private ResultSet sqlQuery(String sql, int maxRecords) throws SQLException {
		 
		Connection dbConnection = null;
		Statement statement = null;
		ResultSet rs = null;
 
		try {
			dbConnection = getDBConnection();
			if (dbConnection == null) {
				System.out.println("ERROR: Could not connect to source database");
				System.exit(1);
			}
			statement = dbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			// 
			if (maxRecords > 0) {
				statement.setMaxRows(maxRecords);
			}
			
			// execute select SQL statement
			rs = statement.executeQuery(sql);
			
			
 
		} catch (SQLException e) {
			e.printStackTrace(System.out);
			//System.out.println(e.getMessage());
 
		} finally {
 
			if (statement != null) {
				//statement.close();
			}
 
			if (dbConnection != null) {
				//dbConnection.close();
			}
 
		}
		return rs;
 
	}
	
	private Connection getDBConnection() {
		 
		Connection dbConnection = null;
 
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		try {
//			dbConnection = DriverManager.getConnection(VBatchManager.source_db_connection.get("db")
//					, VBatchManager.source_db_connection.get("user"),
//					VBatchManager.source_db_connection.get("password"));
			System.out.println("\t[Extract] Connecting to oracle server: " + VBatchManager.source_db_connection.get("db"));
			dbConnection = DriverManager.getConnection(VBatchManager.source_db_connection.get("db"));
			return dbConnection;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return dbConnection;
	}
	
	/**
	 * Get a list of the pk1/ok1 values from the last successful run 
	 * of this job, so we can skip those records in this run.
	 * 
	 * @param previousRunBatchLogId
	 */
	private void getPreviousJobOkDtls(BatchLog previousRunBatchLogId) {
		TypedQuery<BatchLogOkDtl> qryPrevOkDtl = this.job_manager.db.createNamedQuery("BatchLogOkDtl.findByBatchLogId", BatchLogOkDtl.class);
		qryPrevOkDtl.setParameter("batchLogId", previousRunBatchLogId);	  
		List<BatchLogOkDtl> lstPrevOkDtl = qryPrevOkDtl.getResultList();
		for (BatchLogOkDtl prevEntry : lstPrevOkDtl) {
			previousJobOkDtls.add(prevEntry);
		}
	}
}
