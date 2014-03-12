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

import model.BatchLogDtl;
import model.Step;

public class ExtractDBStep extends StepManager {

	private int record_pointer = 0;
	private BatchLogDtl log_dtl = null;
	
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
		System.out.println("\t[Extract] max_rec: " + max_rec);
		
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
		// Start db transaction
		//this.job_manager.db.getTransaction().begin();
		
		// Set this step to running
		this.running = true;
		// Log the start of this step
		this.logStart();
		int commit_freq = this.step_record.getExtractCommitFreq().intValue();
		
		/*  
		 * TODO: Lookup the maxOk1 from the last time this job ran
		 */
		
		String previousRunOK1;
		
		// This steps' step_id
		int my_step_id = (int) this.step_record.getId();
		
		// Find the last time this job ran, so we can get its max_ok1
		System.out.println("\t[Extract] About to look up the last create_date_time matching job_id: " + my_step_id);
		
		TypedQuery<BatchLogDtl> query = this.job_manager.db.createQuery(
		        "SELECT dtl FROM BatchLogDtl dtl WHERE dtl.stepType = :stepType  and dtl.stepsId = :stepId "  // must match this step's type and id
				+ " and dtl.maxOk1 is not null "  // must have  min_ok1 and max_ok1 value
		        + "order by dtl.id desc", BatchLogDtl.class);  // most recent match comes first
		    List<BatchLogDtl> dtls = query
		    		.setParameter("stepType", "Extract")
		    		.setParameter("stepId", my_step_id)
		    		.getResultList();
		System.out.println("\t[Extract] Found matches in log_dtl table: " + dtls.size());
		
		// Get the raw sql to run
		String raw_sql = this.step_record.getExtractSql();
		// Remove trailing semicolon which is valid sql but confuses jdbc sometimes
		if (raw_sql.indexOf(";", raw_sql.length()-1) != -1) {
			raw_sql = raw_sql.substring(0, raw_sql.length()-1);
		}
		
		// If there's at least one previous run with valid maxOK1
		if (dtls.size() > 0) {
			BatchLogDtl lastRun = dtls.get(0);  // Most recent successful run of the same type
			previousRunOK1 = lastRun.getMaxOk1();  // Oracle date in string format
			
			// Since we're relying on JDBC to convert a date object to a string,
			// force it into the specific date format that Oracle will be able to use in
			// a query (for instance, forcing the year from 2-digit to 4-digit)
			previousRunOK1 = this.convertDateStringToAnotherDateString(previousRunOK1, "MM/d/yy k:mm:ss", "MM/dd/yyyy k:mm:ss");
			
			// Create a WHERE clause that starts after lastOk1
			String startClause = " ptt.create_date_time > to_date('" + previousRunOK1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
			
			// replace the /* where */ token with our dynamic where clause
			// however, make sure that if there isn't a WHERE clause that
			// we add that as well
			
			// Count instances of /* where */ token
			Pattern p = Pattern.compile("\\/\\* where \\*\\/");
			Matcher m = p.matcher(raw_sql.toLowerCase());
			int whereTokenCount = 0;
			while (m.find()) {
				whereTokenCount += 1;
			}
			// Count instances of WHERE clause
			Pattern wherePattern = Pattern.compile(" where ");
			Matcher whereMatcher = wherePattern.matcher(raw_sql.toLowerCase());
			int whereCount = 0;
			while (whereMatcher.find()) {
				whereCount += 1;
			}
			
			// Replace the where token (and add a WHERE clause, if missing)
			if (whereTokenCount == 1 ) {
				if (whereCount > 1) { // There is an existing where clause
					raw_sql = raw_sql.replace("/* where */", " AND " + startClause);
				}
				else {  // No WHERE clause, add it
					raw_sql = raw_sql.replace("/* where */", " WHERE " + startClause);
				}
			}
			else {
				// TODO:  fail this job, and log the fact that the /* where */ token was missing
				this.job_manager.db.getTransaction().begin();
				this.log_dtl.setStatus("Failed");
				this.log_dtl.setErrorMsg("Missing /* where */ token in SQL statement.");
				this.job_manager.db.persist(this.log_dtl);
				this.job_manager.db.getTransaction().commit();
				this.running=false;
				this.completed=true;
				this.failed=true;
				return false;  // abort this step
			}
			
			
			
		}
		
		// Limit this query to the first [max_rec] rows
//		if (this.max_rec > 0) {
//			String new_query = "SELECT * FROM ( "
//					+ raw_sql
//					+ " ) WHERE ROWNUM <= "
//					+ this.max_rec;
//			raw_sql = new_query;
//			System.out.println(raw_sql);
//		}
		
		/**
		 * To find
		 */
		// Work backwards from the last row, looking for the first row whose
		// create_date_time and pk1 (tran_nbr) are different from last row
		// lastOk1 = '07-May-2013 07:15:21'
		int endRowsToSkip = 0;
		int rowCount = 0;
		
		ResultSet rs = null;
		
		// go to end of recordset
		int finalRowNum = 0;  // this will be the final row # for this job
		try {
			System.out.println("\tAbout to query " + this.max_rec + " records");
			System.out.println("REWRITTEN QUERY: " + raw_sql);
			rs = this.sqlQuery(raw_sql, commit_freq, this.max_rec);  // limit query to max_rec rows
			
			
			rs.last();
			
			int startingRowNum = rs.getRow();
			
			// System.out.println("\tLast row #: " + startingRowNum);
			String lastRowOK1, lastRowPK1;
			try {
				lastRowOK1 = this.convertDateFieldToString(rs, "OK1");
				lastRowPK1 = rs.getString("tran_nbr");
				
				String currentRowOK1, currentRowPK1;
				// STUB:  comparison here should match
				currentRowOK1 = this.convertDateFieldToString(rs, "OK1");
				currentRowPK1 = rs.getString("tran_nbr");
				System.out.println("\tLast row: " + currentRowOK1 + " / " + currentRowPK1);
				// Go backwards until pk1 and ok1 are different from last row
				while (rs.previous()) {
					endRowsToSkip++;
					currentRowOK1 = this.convertDateFieldToString(rs, "OK1");
					currentRowPK1 = rs.getString("tran_nbr");
					if (!currentRowOK1.equals(lastRowOK1) && !currentRowPK1.equals(lastRowPK1)) {
						
						finalRowNum = startingRowNum - endRowsToSkip;
						System.out.println("\tExported " + finalRowNum + " rows (skipped " + endRowsToSkip + ")");
						System.out.println("\tFinal row: " + currentRowOK1 + " / " + currentRowPK1);
						break;
					}
					else {
						System.out.println("\tSkipping row: " + currentRowOK1 + " / " + currentRowPK1);
					}
					
				}
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(rowOK1);
			
		}
		catch (SQLException e) {
			e.printStackTrace(System.out);
		}
		
		
		
		
		// System.out.println(raw_sql);
		
		// Do we need to break this into pages?
		
		
		
		
		/*
		 * Before performing the data extraction, move the ending point backwards
		 * until both the ok1 value and the create_date_time have changed.
		 */
		
		
		try {
			// rs = this.sqlQuery(raw_sql, commit_freq, -1);
			
			// go back to beginning of recordset 
			rs.first();
			
			
			int rownum = 0;
			
			// Columns to skip
			List<String> namesOfColumnsToSkip = new ArrayList<String>();
			namesOfColumnsToSkip.add("pk1");
			namesOfColumnsToSkip.add("pk2");
			namesOfColumnsToSkip.add("pk3");
			namesOfColumnsToSkip.add("ok1");
			
			// Get column names
			ResultSetMetaData meta = rs.getMetaData();
			int col_count = meta.getColumnCount();
			String colType, colName;
			Map<Integer, String> columnsToSkip = new HashMap<Integer, String>();
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
			
			// TODO:  Make sure our required columns are in the query:
			// ok1, pk1 [pk2-pk3]
			String previousRowOK1Value = new String();
			// Iterate over all the rows of the ResultSet
			boolean endOfRecordset=false;
			while (!endOfRecordset) {
				rownum++;  // Note: rownum starts at 1
				
				// If this row would exceed step.max_rec,
				// stop processing records here.
				if (rownum > finalRowNum) {
					// Get the current OK1 value
					String lastRowOK1Value = this.convertDateFieldToString(rs, "OK1");
					// Write maxOK1 to the log_dtl record for this extraction step
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMaxOk1(previousRowOK1Value);
					this.log_dtl.setStatus("Completed");
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
				
				// If this is the first row, write batch_log_dtl.min_ok1
				if (rownum==1 && this.log_dtl != null) { // make sure the log_dtl exists for this step
					// 
					String newDs = convertDateFieldToString(rs, "OK1");
					//System.out.println("\t[Extract] ds: " + newDs);
					
					// Write minOK1 to db
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMinOk1(newDs);
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
				}
				
				if (rownum % commit_freq == 0) {  // send this page of data to next step
					// add these rows to log_dtl.num_records
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setNumRecords(new BigDecimal(rownum));
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
					
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
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMaxOk1(lastRowOK1Value);
					this.log_dtl.setEndDt(new Date());
					this.log_dtl.setStatus("Completed");
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
				}
				
				previousRowOK1Value = this.convertDateFieldToString(rs, "OK1");
				
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
			
		
		/*
		List lst;
		while (this.record_pointer != -1) {  // Keep loading more pages of data
			Query qry = this.job_manager.db.createNativeQuery(raw_sql)
				    .setFirstResult(this.record_pointer)
					.setMaxResults(commit_freq);
			lst = qry.getResultList();
			System.out.println("\tLoaded " + lst.size() + " records");
			if (lst.size() < commit_freq) {  // this is the last page of data
				// Update record pointers for next query
				this.record_pointer = -1;
				this.job_manager.submitPageOfData(lst, this);
			}
			else {  // This won't be the last page of data
				this.record_pointer += commit_freq;
				this.job_manager.submitPageOfData(lst, this);
			}
		}
		
		*/
		// This step is done.  Clean up, write to logs,
		// and return control to JobManager.
		this.running=false;
		this.completed=true;
		this.failed=false;
		
		// TODO:  empty data variables
		// TODO:  log completion of this step
		
		return true;
		
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
		SimpleDateFormat outgoingDateFormat = new SimpleDateFormat("MM/d/y k:mm:ss");
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
