package com.vco;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import model.BatchLog;
import model.BatchLogDtl;
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
	
	public ExtractDBStep(JobManager jm, Step step_record) {
		this.job_manager = jm;
		this.step_record = step_record;
		
		// Use hard-coded default for max_rec, unless one is defined in the step config table
		int max_rec = this.step_record.getExtractMaxRec().intValue();
		if (max_rec > 0 ) {
			this.max_rec = this.step_record.getExtractMaxRec().intValue();
		}
		
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
		this.logStart();
		// Get the raw sql to run
		this.raw_sql = this.step_record.getExtractSql();
		String whereClause = new String();
		int commit_freq = this.step_record.getExtractCommitFreq().intValue();
		ResultSet rs = null;
		// For an extract run, these values must be set
		String previousRunMinOk1, previousRunMaxOk1;
		int previousRunTotalRecordsExtracted;
		int totalRows = 0;
		
		/**
		 * Prepare to re-run of previous batch, based on batch_num  
		 *   - look up minOk1/maxOk1 from last run of batch
		 *   - look up total rows from previous job
		 * 
		 */
		
		if (this.job_manager.batch_manager.batchMode == VBatchManager.BatchMode_Repeat) {
			
			// The user called -b 123, where 123 is a batch_num.  Look up that job,
			// to get the min/maxOk1 values
			
			// Get the BatchLog entry by batch_num
			TypedQuery<BatchLog> qryPreviousBatch = this.job_manager.db.createQuery(
					"SELECT log from BatchLog log WHERE log.batchNum = :batchNumber order by log.id asc", BatchLog.class);
			List<BatchLog> lstPreviousBatch = qryPreviousBatch
		    		.setParameter("batchNumber", this.job_manager.job_id)
		    		.setMaxResults(1)
		    		.getResultList();
			if (lstPreviousBatch.size() == 0 ) {
				System.out.println("vbatch Error: Batch " + this.job_manager.job_id + " not found.");
				System.exit(1);
			}
			// Use the BatchLog entry to look up Extract steps from this previous run
			TypedQuery<BatchLogDtl> qryMatchingLogDtl = this.job_manager.db.createQuery(
					"SELECT dtl from BatchLogDtl dtl WHERE dtl.batchLog = :batchLog AND dtl.stepType = 'Extract' order by dtl.id asc", BatchLogDtl.class);
			List<BatchLogDtl> lstMatchingLogDtl = qryMatchingLogDtl
		    		.setParameter("batchLog", lstPreviousBatch.get(0))
		    		.getResultList();
			// lstMatchingLogDtl.size() = number of extract steps for this batch
			BatchLogDtl extract_log = lstMatchingLogDtl.get(0);
			// Get the vars we need for min/max to re-run this job with the same records
			previousRunMinOk1 = extract_log.getMinOk1();
			previousRunMinOk1 = this.convertDateStringToAnotherDateString(previousRunMinOk1, "MM/d/yy H:mm:ss", "MM/dd/yyyy H:mm:ss");
			previousRunMaxOk1 = extract_log.getMaxOk1();
			previousRunMaxOk1 = this.convertDateStringToAnotherDateString(previousRunMaxOk1, "MM/d/yy H:mm:ss", "MM/dd/yyyy H:mm:ss");
			BigDecimal numRecs = extract_log.getNumRecords();
			totalRows = numRecs.intValue();
			
			
			// Add minOk1 and maxOk1 to startClause to the whereClause
			whereClause += " AND ptt.create_date_time >= to_date('" + previousRunMinOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
			whereClause += " AND ptt.create_date_time <= to_date('" + previousRunMaxOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
			
			// Run the query
			try {
				rs = this.sqlQuery(this.raw_sql, commit_freq, totalRows);
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
			TypedQuery<BatchLogDtl> query = this.job_manager.db.createQuery(
		        "SELECT dtl FROM BatchLogDtl dtl WHERE dtl.stepType = :stepType  and dtl.stepsId = :stepId "  // must match this step's type and id
				+ " and dtl.maxOk1 is not null "  // must have  min_ok1 and max_ok1 value
		        + "order by dtl.id desc", BatchLogDtl.class);  // most recent match comes first
		    List<BatchLogDtl> dtls = query
		    		.setParameter("stepType", "Extract")
		    		.setParameter("stepId", (int) this.step_record.getId())
		    		.getResultList();
			
			// dtls.size()  is the number of previous executions of this step from log_dtl
		    
			// Remove trailing semicolon which is valid sql but confuses jdbc sometimes
			if (this.raw_sql.indexOf(";", this.raw_sql.length()-1) != -1) {
				this.raw_sql = this.raw_sql.substring(0, this.raw_sql.length()-1);
			}
			
			// If there's at least one previous run with valid maxOK1
			if (dtls.size() > 0) {
				BatchLogDtl lastRun = dtls.get(0);  // Most recent successful run of the same type
				
				
				
				// Get maxOk1 from previous run
				previousRunMaxOk1 = lastRun.getMaxOk1();  // Oracle date in string format
				
				// Since we're relying on JDBC to convert a date object to a string,
				// force it into the specific date format that Oracle will be able to use in
				// a query (for instance, forcing the year from 2-digit to 4-digit)
				previousRunMaxOk1 = this.convertDateStringToAnotherDateString(previousRunMaxOk1, "MM/dd/yy H:mm:ss", "MM/dd/yyyy H:mm:ss");
				
				
				
				// replace the /* where */ token with our dynamic where clause
				// however, make sure that if there isn't a WHERE clause that
				// we add that as well
				
				// Create a WHERE clause that starts after lastOk1
				whereClause = " ptt.create_date_time >= to_date('" + previousRunMaxOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
				// replace the SQL TOKEN(s) ( /* where */ )
				int sqlTokensReplaced =  this.replaceSqlToken(this.raw_sql, whereClause); 
				if (sqlTokensReplaced == 0) {
					// TODO:  Log the fact that no sql tokens were found
				}
				
				// Set totalrows for this job
				
				
			}
			
			// Get total_rows from the previous run (if set in record)
			float jobMaxRecs = this.step_record.getExtractMaxRec().floatValue();
			if (jobMaxRecs > 0) {
				double j = Math.floor(1.1 * jobMaxRecs);
				totalRows = (int)j;
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
				rs = this.sqlQuery(this.raw_sql, commit_freq, totalRows);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  // limit query to max_rec rows
			
			
		}
		
		/**
		 * Now that we have all variables needed, and the data in 
		 * this.rs, move forward through the records gathering the
		 * data and passing it to the next step.
		 */
		
		// Vars for keeping track of ok/pk values for each row
		List<BatchLogOkDtl> tempOkDtlList = new ArrayList<BatchLogOkDtl>();
		String previousRowPK1Value = new String();
		String currentRowPK1Value  = new String();
		String previousRowOK1Value = new String();
		String currentRowOK1Value  = new String();
		try {
			// go back to beginning of recordset 
			rs.first();
			
			int rownum = 0;
			
			Map<Integer,String> columnsToSkip = this.prepareSkipColumns(rs);
			
			// TODO:  Make sure our required columns are in the query:
			// ok1, pk1 [pk2-pk3]
			
			// Iterate over all the rows of the ResultSet
			boolean endOfRecordset=false;
			
			while (!endOfRecordset) {
				rownum++;  // Note: rownum starts at 1
				this.records_processed++;
				// If this row would exceed step.max_rec,
				// stop processing records here.
				if (rownum > totalRows) {
					// Get the current OK1 value
					String lastRowOK1Value = this.convertDateFieldToString(rs, "OK1");
					// Write maxOK1 to the log_dtl record for this extraction step
//					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMaxOk1(previousRowOK1Value);
					this.log_dtl.setStatus("Completed");
					this.log_dtl.setNumRecords(new BigDecimal(this.records_processed-1));
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
					break;
				}
				
				// add the data from this row into the output object
				List<Object> rowdata = new ArrayList<Object>();
				for (int ci = 1; ci <= col_count; ci++) {
					if (columnsToSkip.containsKey(ci)) {  // is this column in columnsToSkip?
						// Do not export this column
					}
					else {
						// Add this column to the output data
						rowdata.add(rs.getString(ci));
					}
					
				}
				// Add this row to dataPageOut, to be sent to the next step
				this.dataPageOut.add(rowdata);

				// Add the ok/pk from this row to the temp list
				BatchLogOkDtl newOkDtl = new BatchLogOkDtl();
				newOkDtl.setBatchLog(this.job_manager.batch_log);
				newOkDtl.setOk1(this.convertDateFieldToString(rs, "OK1"));
				currentRowPK1Value = rs.getString("PK1");
				// If PK1 has changed, remove previous entries from tempOkDtlList
				if (!currentRowPK1Value.equals(previousRowPK1Value)) {
					tempOkDtlList.clear();
				}
				
				tempOkDtlList.add(newOkDtl);
				
				// Remember pk1/ok1 to compare to the next row
				previousRowPK1Value = rs.getString("PK1");
				previousRowOK1Value = this.convertDateFieldToString(rs, "OK1");
				
				// If this is the first row, write batch_log_dtl.min_ok1
				if (rownum==1 && this.log_dtl != null) { // make sure the log_dtl exists for this step
					// 
					String newDs = convertDateFieldToString(rs, "OK1");
					//System.out.println("\t[Extract] ds: " + newDs);
					
					// Write minOK1 to db
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMinOk1(newDs);
//					this.job_manager.db.persist(this.log_dtl);
//					this.job_manager.db.getTransaction().commit();
				}
				
				if (rownum % commit_freq == 0) {  // send this page of data to next step
					// add these rows to log_dtl.num_records
//					this.job_manager.db.getTransaction().begin();
					//this.records_processed = rownum;
					
					// Write out the logs for this step so far
					this.log_dtl.setNumRecords(new BigDecimal(this.records_processed));
					this.job_manager.db.persist(this.log_dtl);
					
					// Write okdtl logs for this step
					for (BatchLogOkDtl okdtl : tempOkDtlList) {
						this.job_manager.db.persist(okdtl);
					}
					// Clear out ok_dtl list
					tempOkDtlList.clear();
					
					this.job_manager.db.getTransaction().commit();
					this.job_manager.db.getTransaction().begin();
					
					// Submit this page of data to job manager
					this.job_manager.submitPageOfData(this.dataPageOut, this);
					
					
					
					
					// Reset page data
					this.dataPageOut = new ArrayList<Object>();
					
				}
				
				// Any special processing for the last row goes here
				if (rs.isLast()) {
					// rownum = (the last row number)
					
					// Get the current OK1 value
					String lastRowOK1Value = this.convertDateFieldToString(rs, "OK1");
					// Write maxOK1 to the log_dtl record for this extraction step
//					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMaxOk1(lastRowOK1Value);
					this.log_dtl.setEndDt(new Date());
					this.log_dtl.setStatus("Completed");
					this.log_dtl.setNumRecords(new BigDecimal(this.records_processed));
					// Write okdtl logs for this step
					for (BatchLogOkDtl okdtl : tempOkDtlList) {
						this.job_manager.db.persist(okdtl);
					}
					// Clear out ok_dtl list
					tempOkDtlList.clear();
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
				}
//				previousRowOK1Value = this.convertDateFieldToString(rs, "OK1");
				
				// Move to the next record (or abort if we're past the last row
				if (rs.next() == false) {  // moved past the last record
					endOfRecordset=true;
				}
			} // end: looping over rows of data
			
			// Send any remaining records to the next step
			if (this.dataPageOut.size() > 0) {
				// Mark this step as complete
				this.completed = true;
				this.job_manager.submitPageOfData(this.dataPageOut, this);
				// Reset page data
				this.dataPageOut = new ArrayList<Object>();
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
		this.running=false;
		this.completed=true;
		this.failed=false;
		
		// TODO:  empty data variables
		// TODO:  log completion of this step
		
		return true;
		
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
		this.job_manager.db.getTransaction().begin();
		// Create entry in batch_log_dtl
		this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.job_manager.batch_log);
		
		String msg = "Step [" + this.step_record.getType() 
				+ " : " + this.step_record.getShortDesc()
				+ "]";
		this.log_dtl.setLongDesc(msg);
		this.log_dtl.setStepsId(new BigDecimal(this.step_record.getId()));
		this.log_dtl.setStepsShortDesc(this.step_record.getShortDesc());
		this.log_dtl.setStepType(this.step_record.getType());
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus("Started");
		
		// Log the job settings used to run this job
		this.log_dtl.setExtractSql(this.step_record.getExtractSql());
		this.log_dtl.setExtractMaxRecs(this.step_record.getExtractMaxRec());
		this.log_dtl.setExtractCommitFreq(this.step_record.getExtractCommitFreq());
		this.log_dtl.setClassPath(this.step_record.getClassPath());
		// TODO:  job steps xref id
		this.log_dtl.setStepsId(new BigDecimal(this.step_record.getId()));
		
		// Commit log entry
		this.job_manager.db.persist(this.log_dtl);
		this.job_manager.db.getTransaction().commit();
		
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
			rs = this.sqlQuery(sql, 0, -1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
	
	private ResultSet sqlQuery(String sql, int num_records, int maxRecords) throws SQLException {
		 
		Connection dbConnection = null;
		Statement statement = null;
		ResultSet rs = null;
		//String selectTableSQL = "SELECT * from JOB_DEFINITION ";
 
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
}
