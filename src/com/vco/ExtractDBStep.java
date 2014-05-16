package com.vco;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
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
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import com.VBatchException;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;

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
	private int rowsInPreviousJobOutput = 0;
	public Logger log = null;
	// JDBC variables
	private Connection dbConnection = null;
	private Statement statement = null;
	private ResultSet rs = null;  // JDBC results of raw query
	
	// Row-centric properties
	String ok1ColName, pk1ColName, pk2ColName, pk3ColName;  // native column names for OK/PK column aliases
	
	public ExtractDBStep(JobManager jm, JobStepsXref jobStepXref) {
		this.job_manager = jm;
		this.jobStepXref = jobStepXref;
		log = Logger.getLogger(this.job_manager.batch_manager.vbatch_version);	
		
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
	public boolean start() throws Exception {
		try {
			/**  Initialize all the vars **/
			// Set this step to running
			this.running = true;
			// Get the raw sql to run
			this.raw_sql = this.jobStepXref.getStep().getExtractSql();
			String whereClause = new String();
			
			// For an extract run, these variables are used to keep track
			// of settings from the previous run of this job
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
			
			// Cleanup raw_sql and parse it for column names
			
			this.raw_sql = ExtractDBStep.cleanRawSql(this.raw_sql);
			
			/** For ok1 and pk1-3, we need to know the original, native column names
			 *  for these aliased columns.  JDBC doesn't reliably give us this information
			 *  in all cases, so we parse the raw sql for it instead.
			 */
			
			
			this.ok1ColName = null;
			for (String token : raw_sql.toLowerCase().split(",")) {
			    //Exs of tokens are "select OK1" and "F37 from (select ptt.create_date_time as OK1"
			    if ((this.ok1ColName == null) && token.trim().endsWith("ok1")) {
			        String[] reversedTokens = token.trim().split(" ");
			        Collections.reverse(Arrays.asList(reversedTokens));
			        for (String colname : reversedTokens) {
			            if (colname.trim().equalsIgnoreCase("ok1") || colname.trim().equalsIgnoreCase("as") || colname.trim().equals("") || colname.trim().equals("select")) { 
			                continue; 
			            } 
			            else { 
			                this.ok1ColName = colname.trim(); 
			                log.debug("OK1 column name: " + this.ok1ColName);
			                break;
			            }
			        }
			    }
			}
			
			// Look up original column name for PK1
			this.pk1ColName = null;
			for (String token : raw_sql.toLowerCase().split(",")) {
			    if ((this.pk1ColName == null) && token.trim().endsWith("pk1")) {
			    	String[] reversedPK1Tokens = token.trim().split(" ");
			    	Collections.reverse(Arrays.asList(reversedPK1Tokens));
			        for (String colname : reversedPK1Tokens) { 
			            if (colname.trim().equalsIgnoreCase("pk1") || colname.trim().equalsIgnoreCase("select") || colname.trim().equalsIgnoreCase("as") || colname.trim().equals("")) { 
			                continue; 
			            } 
			            else { 
			                this.pk1ColName = colname.trim(); 
			                log.debug("PK1 column name: " + this.pk1ColName);
			                break;
			            } 
			        }
			    }
			}
			
			// Look up original column name for PK1
			this.pk2ColName = null;
			for (String token : raw_sql.toLowerCase().split(",")) {
			    if ((this.pk2ColName == null) && token.trim().endsWith("pk2")) {
			    	String[] reversedPK2Tokens = token.trim().split(" ");
			    	Collections.reverse(Arrays.asList(reversedPK2Tokens));
			        for (String colname : reversedPK2Tokens) { 
			            if (colname.trim().equalsIgnoreCase("pk2") || colname.trim().equalsIgnoreCase("select") || colname.trim().equalsIgnoreCase("as") || colname.trim().equals("")) { 
			                continue; 
			            } 
			            else { 
			                this.pk2ColName = colname.trim(); 
			                log.debug("PK2 column name: " + this.pk2ColName);
			                break;
			            } 
			        }
			    }
			}
			
			// Look up original column name for PK1
			this.pk3ColName = null;
			for (String token : raw_sql.toLowerCase().split(",")) {
			    if ((this.pk3ColName == null) && token.trim().endsWith("pk3")) {
			    	String[] reversedPK3Tokens = token.trim().split(" ");
			    	Collections.reverse(Arrays.asList(reversedPK3Tokens));
			        for (String colname : reversedPK3Tokens) { 
			            if (colname.trim().equalsIgnoreCase("pk3") || colname.trim().equalsIgnoreCase("select") || colname.trim().equalsIgnoreCase("as") || colname.trim().equals("")) { 
			                continue; 
			            } 
			            else { 
			                this.pk3ColName = colname.trim(); 
			                log.debug("PK3 column name: " + this.pk3ColName);
			                break;
			            } 
			        }
			    }
			}
			
			
			if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_Repeat) {
				
				// vBatch was instantiated like this:  "vbatch -b 123" , where 123 
				//is a batch_number.  Look up the first run
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
						totalRows = initialRunLogDtl.getExtractMaxRecs().intValue();
						this.rowsInPreviousJobOutput = initialRunLogDtl.getNumRecords().intValue();
						log.debug("Rows in previous job: " + rowsInPreviousJobOutput);
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
				whereClause += this.ok1ColName + " >= to_date('" + previousRunMinOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
				whereClause += " AND " + this.ok1ColName + " <= to_date('" + previousRunMaxOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
							
				// replace the SQL TOKEN(s) ( /* where */ )
				int sqlTokensReplaced =  this.replaceSqlToken(this.raw_sql, whereClause); 
				// Run the query
				
				log.info("Rewritten query: " + this.raw_sql);
//				this.sqlQuery(this.raw_sql, totalRows);
				this.sqlQuery(this.raw_sql, totalRows+100);
				
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
					whereClause = this.ok1ColName + " >= to_date('" + previousRunMaxOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
					// replace the SQL TOKEN(s) ( /* where */ )
					int sqlTokensReplaced =  this.replaceSqlToken(this.raw_sql, whereClause); 
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
	
				log.info("Rewritten query: " + this.raw_sql);
				this.sqlQuery(this.raw_sql, totalRows+100);
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
			
			String previousRowOK1Value = new String();
			String previousRowPK1Value = new String();
			// As we load each row of the JDBC recordset,
			// use these vars to keep track of the OK/PK values.
			String currentRowPK1Value  = new String();
			String currentRowPK2Value  = new String();
			String currentRowPK3Value  = new String();
			String currentRowOK1Value  = new String();
			// As we hit isPageDataAlmostComplete, remember the PK1
			// at this row so we can keep comparing each new row
			// (rolling forward) until PK1 changes.
			String PK1AtEndOfCurrentPage = new String(); 
			
			
				// Go back to beginning of jdbc recordset,
			    // and return false if there are no records
				boolean recordsetHasItems = this.rs.first();
				
				// From all valid OK/PK columns, prepare a list of the columns
				// present in this raw query, because we need to make sure
				// none of these columns are exported in the data
				this.columnsToSkip = this.prepareSkipColumns(this.rs);
				
				// Iterate over all the rows of the ResultSet
				boolean endOfRecordset=false; 
				boolean endOfPage = false;
				
				// This gets set each time we reach a multiple of extract_max_recs_per_page
				boolean isPageDataAlmostComplete  =false;  
				// After isPageDataAlmostComplete is true, roll forward until pk1 changes, then set this to true.
				// This signals the Extract step to send page of data to the next step
				boolean isPageDataComplete        =false;
				// This gets set when rows processed reaches the max records for this job
				boolean isRecordsetAlmostComplete =false;
				// After isRecordsetAlmostComplete, this gets set to true once
				// we run out of records, or reach end of job.  
				boolean isRecordsetComplete       =false;
				boolean skipThisRecord            =false; //To skip records when it exists in previous batch run OK_DTL table
				
				// These two counters keep track of which row we're currently looking at
				// Current row in recordset, regardless of whether it's exported or not
				this.currentRowNum = 0; 
				// Only get incremented for rows that are exported
				this.rowsIncludedInJob = 0;
				
				// If there's at least one row, then begin processing row(s)
				if (recordsetHasItems) {
					// Display the start of the step in the logs
					String msg = "[" + this.jobStepXref.getStep().getType() 
							+ " : " + this.jobStepXref.getStep().getShortDesc()
							+ "]";
					this.log.info("Step started: " + msg);
					
					while (!endOfRecordset) {
						this.currentRowNum++;
						skipThisRecord=false;
						currentRowOK1Value = this.convertDateFieldToString(this.rs, "OK1");
						if (this.pk1ColName != null && !(this.pk1ColName.isEmpty())) {
							currentRowPK1Value = this.rs.getString("PK1");
						}
						if (this.pk2ColName != null && !(this.pk2ColName.isEmpty())) {
							currentRowPK2Value = this.rs.getString("PK2");
						}
						if (this.pk3ColName != null && !(this.pk3ColName.isEmpty())) {
							currentRowPK3Value = this.rs.getString("PK3");
						}
						
						boolean isLastRecord = this.rs.isLast();  // Is the last row in jdbc recordset
						String debugMsg1 = "Evaluating row # " + this.currentRowNum;
						debugMsg1 += ", OK1 [" + currentRowOK1Value + "]";
						debugMsg1 += ", PK1 [" + currentRowPK1Value + "]";
						
						if (this.pk2ColName != null && !(this.pk2ColName.isEmpty())) {
							debugMsg1 += ", PK2 [" + currentRowPK2Value + "]";
						}
						if (this.pk3ColName != null && !(this.pk3ColName.isEmpty())) {
							debugMsg1 += ", PK3 [" + currentRowPK3Value + "]";
						}
						log.debug(debugMsg1);
						
						/** Before processing this row, we set the state of the main boolean
						 *  flags that determine what to do with this row.   
						 *  **/
						
						
						/** If this row was already in the previous job,
						 *  completely skip processing this row.
						 */
						
						if (isRowInPreviousRunOkDtl(this.rs)) {
							skipThisRecord=true;
						}
						else {
							this.rowsIncludedInJob++;
						}
						
						/** If this row (number) is a multiple of extract_max_rec_per_file,
						 *  then set isPageDataAlmostComplete, and remember the PK1 of this
						 *  row.  Once this is set, we start 'rolling forward' and checking
						 *  eac row until PK1 changes.
						 */
						int jobMaxRecPerFile = this.job_manager.getMaxRecPerFile();
						if ( (rowsIncludedInJob != 0 ) && (rowsIncludedInJob % jobMaxRecPerFile == 0)) { 
							// If we hit this mark, but pageDataComplete is already set,
							// then we're still moving forward through records from a previous 
							// page when we hit this mark, so don't reset the PK1 marker
							if (!isPageDataAlmostComplete) {
								// Since this record marks the "end" of a set of records,
								// remember its PK1 value
								PK1AtEndOfCurrentPage = this.rs.getString("PK1");
							}
							isPageDataAlmostComplete=true;
						}
						
						/**  If isPageDataAlmostComplete is currently true, check each row
						 *   here to see if PK1 has changed.  If it has, set isPageDataComplete,
						 *   which means we now have a full page of data ready to be sent
						 *   to the next step for processing.
						 */
						if (isPageDataAlmostComplete && !isPageDataComplete) {
							boolean pk1IsTheSame = currentRowPK1Value.equals(PK1AtEndOfCurrentPage);
							boolean pk1IsBlank = StringUtils.isBlank(PK1AtEndOfCurrentPage);
							if ( !(pk1IsTheSame) && !(pk1IsBlank)) {
								isPageDataComplete=true;
								isPageDataAlmostComplete=false;
							}
						}
						
						/**
						 * If we've also exceeded extract_max_rec, then we know we're almost at
						 * the end of the job - we just need to roll forward until PK1 changes
						 * or we run out of data.
						 */
						
						if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_Repeat) {
							// For a re-run, compare actual rows in this job to the exact num rows in original run
							if (rowsIncludedInJob > this.rowsInPreviousJobOutput) {
								isRecordsetAlmostComplete=false;
								isRecordsetComplete = true;
							}
						}
						else if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_New) {
							// For a new run, compare actual rows in this job to extract_max_rec from job definition
							if (rowsIncludedInJob > totalRows) {
								isRecordsetAlmostComplete=true;
								if (isPageDataComplete) {
									isRecordsetComplete=true;
								}
							}
						}
						
						/**
						 * If we've run out of rows in the jdbc recordset, then treat this row as the last,
						 * regardless of the above state.  Set the flags, and remember the PK1/OK1.
						 */
						
						boolean queryExhausted = (isLastRecord);
						if (queryExhausted) {
							isPageDataAlmostComplete=false;
							isPageDataComplete=true;
							isRecordsetAlmostComplete=false;
							isRecordsetComplete=true;
							PK1AtEndOfCurrentPage = this.rs.getString("PK1");
							OK1AtEndOfCurrentPage = this.convertDateFieldToString(this.rs, "OK1");
							log.debug("queryExhausted = true");
						}
						
						// If this is a rerun, and we've reached the number of output row from
						// the original run, then make this the last row
						if (this.job_manager.batch_manager.batchMode==VBatchManager.BatchMode_Repeat
								&& rowsIncludedInJob == this.rowsInPreviousJobOutput) {
							isPageDataAlmostComplete=false;
							isPageDataComplete=true;
							isRecordsetAlmostComplete=false;
							isRecordsetComplete=true;
							PK1AtEndOfCurrentPage = this.rs.getString("PK1");
							isLastRecord=true;
							OK1AtEndOfCurrentPage = this.convertDateFieldToString(this.rs, "OK1");
							log.debug("Batch re-run row count has reach the total rows of the original run.");
						}
						
						
						/**
						 * Now that all state vars are set, begin the processing of this row.
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
								log.debug("Setting minOK1: " + currentRowOK1Value);
							}
							/** If this is the last jdbc row, force this row to be included in job.  **/
							if (isLastRecord) {
								if (!skipThisRecord) {
									log.debug("Process row (very last record).  OK1:" + currentRowOK1Value + ", PK1: " + currentRowPK1Value +
											", PK2: " +currentRowPK2Value + ", PK3: " + currentRowPK3Value);
									processRowOfData(this.rs);
									
								}
							}
							/**  Handle a complete page of data **/
							if (isPageDataComplete) {
								/**  Log the values that only get written at the end of each page. **/
								if (isRecordsetComplete && isLastRecord) {
									this.log_dtl.setMaxOk1(currentRowOK1Value);
									this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob));
									this.log.debug("End of recordset.");
								}
								else {
									this.log_dtl.setMaxOk1(previousRowOK1Value);
									this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob - 1));
									this.log.debug("End of page of data.");
								}
								
								
								/** At the end of the job, delete any ok-dtl entries for this batch.  **/
								if (isRecordsetComplete) {
									TypedQuery<BatchLogOkDtl> qryJobOkDtl = this.job_manager.db.createNamedQuery("BatchLogOkDtl.findByBatchLogId", BatchLogOkDtl.class);
									qryJobOkDtl.setParameter("batchLogId", this.job_manager.batch_log);
									List<BatchLogOkDtl> lstOkDtl = qryJobOkDtl.getResultList();
									for (BatchLogOkDtl oldDtl : lstOkDtl) {
										this.job_manager.db.remove(oldDtl);
									}
									
								}
								
								/** tempOkDtlList contains ok-dtl for every row of data.  Before persisting,
								 *  remove all except the ones for the last OK1.
								 */
								ListIterator<BatchLogOkDtl> it = tempOkDtlList.listIterator(tempOkDtlList.size());
								boolean deleteAllRemaining = false;
								
								while (it.hasPrevious()) {
							    	BatchLogOkDtl tempOkDtl = it.previous();
							    	String rowOK1 = new SimpleDateFormat("MM/d/y H:mm:ss").format(tempOkDtl.getOk1());  
//							    			outgoingDateFormat.format(tempOkDtl.getOk1());
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
							    
							    /**  Submit this page of data to the next step for processing. **/
								log.debug("Extract step is submitting page of data (" + dataPageOut.size() + " rows)");
							    this.job_manager.submitPageOfData(this.dataPageOut, this);
								
								// Clear out page-related variables
								isPageDataComplete=false; 
								this.dataPageOut = new ArrayList<Object>();
								
								
								
								
								/**  We successfully saved a page of data.  Commit the logs.  **/
								this.job_manager.db.getTransaction().commit();
								// Start a new transaction
								this.job_manager.db.getTransaction().begin();
								
								// todo: Page completed, update logs
							}
							
							/** Now that we saved a page of data, we want to process the row of data
							 *  that was being evaluated (but not included in that page) so it is
							 *  included in the next page.  Also remember ok1/pk1 values.
							 */
							
							if (!(isRecordsetComplete)) {
								// If this row was not in the ok-dtl log for previous run,
								// then add this data to the csv
								log.debug("Process row:  OK1:" + currentRowOK1Value + ", PK1: " + currentRowPK1Value +
										", PK2: " +currentRowPK2Value + ", PK3: " + currentRowPK3Value);
								processRowOfData(this.rs);
								// Remember pk1/ok1 to compare to the next row
								previousRowPK1Value = this.rs.getString("PK1");
								previousRowOK1Value = this.convertDateFieldToString(this.rs, "OK1");
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
								
								this.log_dtl.setEndDt(new Date());
								this.log_dtl.setStatus(BatchLog.statusComplete);
								
								if (isLastRecord) {
									this.log_dtl.setMaxOk1(currentRowOK1Value);
									this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob));
								}
								else {
									this.log_dtl.setMaxOk1(previousRowOK1Value);
									this.log_dtl.setNumRecords(new Long(this.rowsIncludedInJob-1));
								}
								
								
								this.job_manager.db.persist(this.log_dtl);
								// todo: break out of recordset while
								endOfRecordset=true;
							}
						}
						
						
						/**  Attempt to load the next jdbc record.  If false, we're out of data.
						 */
						if (this.rs.next() == false) {  // moved past the last record
							endOfRecordset=true;
							// If we didn't process any rows, log the fact that there was no records to process
							if (this.rowsIncludedInJob==0) {
								log.info("Skipping job (no new records found).");
							}
						}		
						
					}
				} // // end of main recordset loop
				else {  // initial recordset had 0 entries
					log.info("Aborting job (no records found).");
					this.running=false;
					this.failed=false;
					this.completed=true;
				}
		} 
		/** This was the main loop for this step.  If any exceptions were thrown from here,
		 *  catch them, gracefully abort the step, and re-throw so the JobManager can gracefully
		 *  abort the job.
		 */
		catch ( Exception e) {
			this.logFailed(e);  
		}
		
		// This step is done.  Clean up, write to logs,
		// and return control to JobManager.
		this.closeJDBCConnection();
		
		/** Return true, indicating to JobManager that this step is done. 
		 *  Note: it's the class vars like running, failed, completed that tell
		 *  JobManager whether this step was successful or not, this is just
		 *  to indicate to the JobManager that it can move on to executing
		 *  the next step.
		 */
		return true;
	}

	/**
	 * Clean up common errors in raw sql that can prevent
	 * it from executing properly in vBatch.
	 */
	public static String cleanRawSql(String raw_sql) {
		
		// Remove leading/trailing spaces that can cause sql parsing errors
		raw_sql = raw_sql.trim();
		// Remove trailing semicolon which is valid sql but confuses jdbc sometimes
		if (raw_sql.endsWith(";")){
			raw_sql = raw_sql.substring(0, raw_sql.length()-1);
		}
		return raw_sql;
	}

	
	/**
	 * Check whether the current row of the recordset
	 * is already listed in the previous Job's ok-dtl
	 * list.  This is used to determine whether to skip
	 * this row.
	 * 
	 * @param currentResultSet
	 * @return
	 * @throws Exception 
	 */
	private boolean isRowInPreviousRunOkDtl(ResultSet currentResultSet) throws Exception {
		boolean retval = false;
		try {
			if (this.previousJobOkDtls.size() > 0 ) {
				// We default all the vars to be compared to 0, to get consistent comparisons.
				Long rowPK1 = 0L;
				Long rowPK2 = 0L;
				Long rowPK3 = 0L;
				String rowOK1 = this.convertDateFieldToString(currentResultSet, "OK1");
				
				// PK1 must be provided in every row, in order to guarantee data integrity.
				// If not, abort job.
				if (currentResultSet.getString("PK1").isEmpty()) { 
					throw new VBatchException("PK1 value cannot be empty");
				}
				/** PK1-3 must be numeric.  If any of them cannot be cast to Long, then abort
				 *  the job.
				 */
				try {
					if (!(currentResultSet.getString("PK1").isEmpty())) { 
						rowPK1 = currentResultSet.getLong("PK1");
					}
					if ((this.pk2ColName!=null && !(this.pk2ColName.isEmpty())  )  && !(currentResultSet.getString("PK2").isEmpty()))
						rowPK2 = currentResultSet.getLong("PK2");
					if ((this.pk3ColName!=null &&  !(this.pk3ColName.isEmpty()) )&& !(currentResultSet.getString("PK3").isEmpty()))
						rowPK3 = currentResultSet.getLong("PK3");
				}
				catch (Exception e){
					throw new VBatchException("PK values must be numeric");
				}
				
				// Loop over ok-dtls from the previous job, comparing to the values from this row
				for (BatchLogOkDtl okDtlEntry : this.previousJobOkDtls) {
					
					Long thisPk1=0L, thisPk2=0L, thisPk3 = 0L;
					
					thisPk1 = okDtlEntry.getPk1();
					if ((this.pk2ColName!=null && !(this.pk2ColName.isEmpty())  ) && okDtlEntry.getPk2() != null) {
						thisPk2 = okDtlEntry.getPk2();
					}
					if ( (this.pk3ColName!=null && !(this.pk3ColName.isEmpty())  ) && okDtlEntry.getPk3() != null) {
						thisPk3 = okDtlEntry.getPk3();
					}
					String thisOk1 = new SimpleDateFormat("MM/d/y H:mm:ss").format(okDtlEntry.getOk1()); 
					
					// DEBUG Logging
					this.log.debug("[ok1:" + thisOk1 + "," + rowOK1 + "] [pk1:" + thisPk1 + ", " + rowPK1 + "] [pk2: "
							+ thisPk2 + ", " + rowPK2 + " [pk3:" + thisPk3 + "," + rowPK3 +  "]"); // + ",[OK1TS: " 
					this.log.debug("[ok: " + thisOk1.equals(rowOK1) + "], [pk1: " + (thisPk1.equals(rowPK1)) + "], "
							+ "[pk2: " + (thisPk2.equals(rowPK2)) + "], "  +" [pk3: " + (thisPk3.equals(rowPK3)) + "] ");
					
					// no need to check if column name is null
					if ( (thisPk1.equals(rowPK1)) && (thisPk2.equals(rowPK2)) && (thisPk3.equals(rowPK3)) && (thisOk1.equals(rowOK1))  )
					{
							retval=true;
							this.log.debug("Found duplicate row from previous run -  skipping row.");
							break;
					}
				}
			}
		} catch (Exception e1) {
			throw e1;
		}
		// TODO Auto-generated method stub
		return retval;
	}
	
	/**
	 * Check whether the current row of the recordset
	 * is already listed in this job's temp ok-dtl list.
	 * 
	 * @param currentRecordset
	 * @return
	 */
	private boolean isRowInTempOkDtl(ResultSet currentRecordset) throws Exception {
		boolean retval = false;
		try {
			if (this.tempOkDtlList.size() > 0 ) {
				if (currentRecordset.getString("PK1").isEmpty()) { 
					throw new VBatchException("PK1 value cannot be empty");
				}
				/** Default PK values to 0, to ensure consistent comparison
				 *  to PK values from tempOkDtl
				 */
				Long rowPK1 = 0L;
				Long rowPK2 = 0L;
				Long rowPK3 = 0L;
				String rowOK1 = this.convertDateFieldToString(currentRecordset, "OK1");
				/** PK values must be numeric.  If not, abort the job.  **/
				try {
					if (!(currentRecordset.getString("PK1").isEmpty())) { 
						rowPK1 = currentRecordset.getLong("PK1");
					}
					if ((this.pk2ColName!=null && !(this.pk2ColName.isEmpty()) ) && !(currentRecordset.getString("PK2").isEmpty()))
						rowPK2 = currentRecordset.getLong("PK2");
					if ((this.pk3ColName!=null && !(this.pk3ColName.isEmpty()) ) && !(currentRecordset.getString("PK3").isEmpty()))
						rowPK3 = currentRecordset.getLong("PK3");
				}
				catch (Exception e){
					// the idea is to catch if there is any error in converting from "ABC" to numeric
					throw new VBatchException("PK values must be numeric");
				}
				

				// Loop over tempOkDtlList
				for (BatchLogOkDtl okDtlEntry : this.tempOkDtlList) {
					/** Get the OK/PK values from this ok-dtl entry **/
//					String thisOk1 = this.convertDateStringToAnotherDateString(okDtlEntry.getOk1().toString(), "y-MM-d HH:mm:ss.S", "MM/d/y H:mm:ss");
					String thisOk1 = new SimpleDateFormat("MM/d/y H:mm:ss").format(okDtlEntry.getOk1()); 
					Long thisPk1=0L, thisPk2=0L, thisPk3 = 0L;
					thisPk1 = okDtlEntry.getPk1();
					if ((this.pk2ColName!=null&& !(this.pk2ColName.isEmpty())  ) && okDtlEntry.getPk2() != null) {
						thisPk2 = okDtlEntry.getPk2();
					}
					if ((this.pk3ColName!=null && !(this.pk3ColName.isEmpty()) ) && okDtlEntry.getPk3() != null) {
						thisPk3 = okDtlEntry.getPk3();
					}
					
					// DEBUG Logging
					this.log.debug("[ok1:" + thisOk1 + "," + rowOK1 + "] [pk1:" + thisPk1 + ", " + rowPK1 + "] [pk2: "
							+ thisPk2 + ", " + rowPK2 + " [pk3:" + thisPk3 + "," + rowPK3 +  "]");
					this.log.debug("[ok: " + thisOk1.equals(rowOK1) + "], [pk1: " + (thisPk1.equals(rowPK1)) + "], "
							+ "[pk2: " + (thisPk2.equals(rowPK2)) + "], "  +" [pk3: " + (thisPk3.equals(rowPK3)) + "] ");
					
					/** Compare the values from this row of data to the values from the ok-dtl entry  **/
					if ( (thisPk1.equals(rowPK1))
						&& ( thisPk2.equals(rowPK2))
						&& ( thisPk3.equals(rowPK3))
						&& ( thisOk1.equals(rowOK1))
					 ) {
						retval=true;
						this.log.debug("Not writing row to ok-dtl table (duplicate key)");
						break;
					}
				}
			}
		}
		catch (Exception e) {
			this.log.error(e.getMessage(),e);
			throw e;
		}
		// TODO Auto-generated method stub
		return retval;
	}

	/**
	 * - Add this row of data from the JDBC recordset to the cache (that will be sent
	 *   to the next step) 
     * - create a new ok-dtl entry for the row.
	 * 
	 * @param currentRowRecordset
	 * @throws SQLException
	 * @throws ParseException
	 */
	private void processRowOfData(ResultSet currentRowRecordset) throws Exception {
		try {
			/**
			 * Check to see if this row was in the previous run's ok-dtl
			 * list.  If so, we'll skip it.
			 */
			
			// Add this row to dataPageOut, to be sent to the next step
			List<Object> rowdata = new ArrayList<Object>();
			for (int ci = 1; ci <= this.col_count; ci++) {
				/** Add this column of data to the page cache, unless it's one of the
				 *  columns that's marked to be skipped (ie:  OK1, PK1-3)
				 */
				if (this.columnsToSkip.containsKey(ci)) {  
					// Do not export this column
				}
				else {
					// Add this column to the output data
					rowdata.add(currentRowRecordset.getString(ci));
				}
			}
			this.dataPageOut.add(rowdata);
			
			/** Log this row to the ok-dtl cache, as long as this job is a "new" run,
			 *  and not a re-run.
			 */
			if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_New) {
				// If this row already exists in ok-dtl table, don't write it again.
				if (!(isRowInTempOkDtl(currentRowRecordset))) {
					BatchLogOkDtl newOkDtl = new BatchLogOkDtl();
					newOkDtl.setBatchLog(this.job_manager.batch_log);
					newOkDtl.setOk1(currentRowRecordset.getTimestamp("OK1"));
					newOkDtl.setPk1(currentRowRecordset.getLong("PK1"));
					if ((this.pk2ColName != null) && !(pk2ColName.isEmpty()) && !(currentRowRecordset.getString("PK2").isEmpty())   ){
						newOkDtl.setPk2(currentRowRecordset.getLong("PK2"));
					}
					if ((this.pk3ColName != null) &&!(pk3ColName.isEmpty()) && !(currentRowRecordset.getString("PK3").isEmpty())   ){
						newOkDtl.setPk3(currentRowRecordset.getLong("PK3"));
					}
					
					this.tempOkDtlList.add(newOkDtl);
					this.OK1AtEndOfCurrentPage = this.convertDateFieldToString(currentRowRecordset, "OK1");
				}
			}
		}
		catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * Replace vBatch SQL token with the proper SQL statement for this run.
	 * 
	 * @param raw_sql
	 * @param tokenReplacement
	 * @return
	 * @throws Exception
	 */
	private int replaceSqlToken(String raw_sql, String tokenReplacement) throws Exception {
		int tokensReplaced = 0;
		// Count instances of /* where */ token
		Pattern p = Pattern.compile("\\/\\* where \\*\\/");
		Matcher m = p.matcher(raw_sql.toLowerCase());
		int whereTokenCount = 0;
		while (m.find()) {
			whereTokenCount += 1;
		}
		/** If we found 1+ SQL token(s), then replace them.  If not,
		 *  abort the job.
		 **/
		if (whereTokenCount > 0 ) {
			this.raw_sql = raw_sql.replaceAll("/\\* where \\*/", " AND " + tokenReplacement + " ");
			tokensReplaced++;
		}
		else {
			throw new VBatchException("No vBatch SQL token found.  Aborting job.");
		}
		return tokensReplaced;
	}

	/**
	 * vBatch requires certain columns to be present in the job query that are
	 * for data integrity purposes only, and should never be exported with job
	 * data.  This function returns a list of those internal columns that are
	 * present in the resultset, so they can be skipped during data export.
	 * 
	 * @param resultset
	 * @return
	 * @throws Exception 
	 */
	private Map<Integer,String> prepareSkipColumns(ResultSet resultset) throws Exception {
		Map<Integer, String> columnsToSkip = new HashMap<Integer,String>();
		try {
			// List of columns that should always be skipped
			List<String> namesOfColumnsToSkip = new ArrayList<String>();
			namesOfColumnsToSkip.add("pk1");
			namesOfColumnsToSkip.add("pk2");
			namesOfColumnsToSkip.add("pk3");
			namesOfColumnsToSkip.add("ok1");
			
			// Find the colNum/colName of each of the skip columns
			ResultSetMetaData meta = resultset.getMetaData();
			this.col_count = meta.getColumnCount();
			String colType, colName;
			
			// Look at each column in resultset, looking for matches
			for (int colIdx = 1; colIdx <= col_count; colIdx++) {
				colType = "";
				colName = "";
				colType  = meta.getColumnTypeName(colIdx);
				colName = meta.getColumnName(colIdx);
				String j = meta.getColumnLabel(colIdx);
				// When we find a match, add it to columnsToSkip
				if (namesOfColumnsToSkip.contains(colName.toLowerCase())) {
					columnsToSkip.put(colIdx,colName);
				}
			}
		} catch (Exception e) {
			this.log.error(e.getMessage(),e);
			throw e;
		}
		return columnsToSkip;
	}

	/**
	 * @param resultset
	 * @param columnName TODO
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	private String convertDateFieldToString(ResultSet resultset, String columnName) throws Exception {
		String newDs;
		try {
			SimpleDateFormat outgoingDateFormat = new SimpleDateFormat("MM/d/y H:mm:ss");
			Timestamp dt = resultset.getTimestamp(columnName);
			newDs = outgoingDateFormat.format(dt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			this.log.error(e.getMessage(), e);
			throw e;
		}
		
		return newDs;
	}
	
	/** 
	 * Converts a date string into a new datestring, applying the newDateFormat to it
	 * @param originalDateString
	 * @param newDateFormat
	 * @return
	 * @throws Exception 
	 */
	
	private String convertDateStringToAnotherDateString(String originalDateString, String incomingDateFormat, String outgoingDateFormat) throws Exception {
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
			this.log.error(e.getMessage(), e);
			throw e;
		}
		
		return newlyFormattedDateString;
		
	}
	
	/**
	 * Currently, ExtractDBStep only sends data, it doesn't receive it.  So
	 * this just returns true.
	 */
	@Override
	public boolean processPageOfData(List<Object> pageOfData) {
		
		return true;
	}
	
	@Override
	public boolean finish() {
		
		return true;
	}
	
	/**
	 * LOGGING
	 * --------
	 * 
	 */
	
	/**
	 * Log the start of this step.
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
		this.log_dtl.setLongDesc(StringUtils.left(msg,150));
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
		// Note:  we don't commit this now.  The main data processing loop handles that.
		
		
	}
	
	/**
	 * Perform a JDBC query, using this step's JDBC connection;
	 * 
	 * @param sql
	 * @param maxRecords
	 * @throws SQLException
	 */
	private void sqlQuery(String sql, int maxRecords) throws SQLException,Exception {
		this.getDBConnection();
		if (this.dbConnection == null) {
			this.log.info("ERROR: Could not connect to source database");
			throw new VBatchException("Could not connect to source database");
		}
		this.statement = this.dbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		if (maxRecords > 0) {
			this.statement.setMaxRows(maxRecords);
		}
		// execute select SQL statement
		this.rs = this.statement.executeQuery(sql);
	}
	
	/**
	 * Close the JDBC connection (to be called when this step is done)
	 */
	private void closeJDBCConnection() {
			try {
				if (this.rs != null) {
					this.rs.close();
				}
				if (this.statement != null) {
					this.statement.close();
				}
				if (this.dbConnection != null) {
					this.dbConnection.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				this.log.info(e.getMessage());
			}
			
	}
	/**
	 * Open a JDBC Connection.
	 */
	private void getDBConnection() {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		try {
			System.out.println("Connecting to oracle server: " + VBatchManager.source_db_connection.get("db"));
			this.dbConnection = DriverManager.getConnection(VBatchManager.source_db_connection.get("db"));
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Get a list of the max pk1/ok1 values from the last successful run 
	 * of this job, which will be used to determine whether to skip 
	 * exporting each row.
	 * 
	 * @param previousRunBatchLogId
	 */
	private void getPreviousJobOkDtls(BatchLog previousRunBatchLogId) {
		TypedQuery<BatchLogOkDtl> qryPrevOkDtl = this.job_manager.db.createNamedQuery("BatchLogOkDtl.findByBatchLogId", BatchLogOkDtl.class);
		qryPrevOkDtl.setParameter("batchLogId", previousRunBatchLogId);	  
		List<BatchLogOkDtl> lstPrevOkDtl = qryPrevOkDtl.getResultList();
		for (BatchLogOkDtl prevEntry : lstPrevOkDtl) {
			previousJobOkDtls.add(prevEntry);
			this.log.debug("Previous job ok-dtl id: " + prevEntry.getId());
		}
	}
	
	/**  
	 * An exception has been thrown.  Rather than just aborting the step, we need to gracefully
	 * end it by:
	 * - logging the exception
	 * - rolling back any logging that would have happened for the failed page of data
	 * - properly logging the final status of this step
	 * - re-throwing the exception so the JobManager can gracefully abort the job
	 * 
	 * @param e
	 * @throws Exception
	 */
	private void logFailed(Exception e) throws Exception {
		/**
		 * In order to avoid the Step and the Job duplicating the same Exception
		 * details in the log files for this job,
		 * we mark the exception as being logged, so that when we re-throw this
		 * exception to allow the JobManager to gracefully abort the job, the 
		 * JobManager won't repeat the exception details in the logs.
		 */
		if (e instanceof VBatchException && ((VBatchException) e).logged==true) {
			
		}
		else {
			// log this error 
			this.job_manager.log.error(e.getMessage(), e);
			// Let other objects know this exception has already been logged
			if (e instanceof VBatchException) {
				((VBatchException) e).logged=true;
			}
		}	
			
		/** Since the step has failed, we need to make sure that we don't
		 *  commit the any of the logs.  Here we roll back the transaction,
		 *  to allow us to create new log entries describing the failed job.
		 */
		if (this.job_manager.db.getTransaction().isActive()) {
			this.job_manager.db.getTransaction().rollback();
			if (!(this.job_manager.db.getTransaction().isActive())) {
				this.job_manager.db.getTransaction().begin();
			}
		}
		else {
			this.job_manager.db.getTransaction().begin();
		}
		
		
		// Write out log_dtl with error status
		if (this.log_dtl == null) {
			this.log_dtl = new BatchLogDtl();
		}
		this.log_dtl.setBatchLog(this.job_manager.batch_log);
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus(BatchLog.statusError);

		// required fields to be able to save this log_dtl
		this.log_dtl.setJobStepsXrefJobStepSeq(this.jobStepXref.getJobStepSeq());
		this.log_dtl.setStepsId(this.jobStepXref.getId());
		this.log_dtl.setStepType("Extract");
		String logfilename = this.job_manager.getJobLogFilename();
		String errormsg = " [" + logfilename + "] " + log_dtl.getErrorMsg();
	    // Limit errorMsg to 150 characters to fit in db field
		this.log_dtl.setErrorMsg(StringUtils.left(errormsg, 150));
		
		this.job_manager.db.persist(this.log_dtl);
		this.job_manager.db.getTransaction().commit();
		
		this.closeJDBCConnection();
		// Re-throw this exception so JobManager catches it
		throw e;
		
	}
}
