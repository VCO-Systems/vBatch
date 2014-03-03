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
import java.util.List;
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
	private int max_rec = 0;  // default max_records for extraction, if none specified
	
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
		
		/*  
		 * TODO: Make sure at least one previous log_dtl entry exists
		 * for this step_id, and use its max_ok1 value to determine
		 * where to start off this query.
		 */
		
		// This steps' step_id
		int my_step_id = (int) this.step_record.getId();
		
		// Find the last time this job ran, so we can get its max_ok1
		System.out.println("\t[Extract] About to look up the last create_date_time matching job_id: " + my_step_id);
		
		TypedQuery<BatchLogDtl> query = this.job_manager.db.createQuery(
		        "SELECT dtl FROM BatchLogDtl dtl WHERE dtl.stepType = :stepType  and dtl.stepsId = :stepId "  // must match this step's type and id
				+ " and dtl.minOk1 is not null "  // must have  min_ok1 and max_ok1 value
		        + "order by dtl.id desc", BatchLogDtl.class);  // most recent match comes first
		    List<BatchLogDtl> dtls = query
		    		.setParameter("stepType", "Extract")
		    		.setParameter("stepId", my_step_id)
		    		.getResultList();
		System.out.println("\t[Extract] Found matches in log_dtl table: " + dtls.size());
		
		// Get the raw sql to run
		String raw_sql = this.step_record.getExtractSql();
		
		// If there's at least one previous run with valid min/maxOK1
		if (dtls.size() > 0) {
			BatchLogDtl lastRun = dtls.get(0);  // Most recent successful run of the same type
			String lastOk1 = lastRun.getMaxOk1();  // Oracle date in string format
			
			// Create a WHERE clause that starts after lastOk1
			String startClause = " p.create_date_time > to_date('" + lastOk1 + "', 'mm/dd/yyyy hh24:mi:ss') ";
			
			// replace the /* where */ token with our dynamic where clause
			// however, make sure that if there isn't a WHERE clause that
			// we add that as well
			Pattern p = Pattern.compile("where");
			Matcher m = p.matcher(raw_sql);
			int whereMatches = 0;
			while (m.find()) {
				whereMatches += 1;
			}
			if (whereMatches <=1 ) {
				raw_sql = raw_sql.replace("/* where */", " WHERE " + startClause);
			}
			else {
				raw_sql = raw_sql.replace("/* where */", startClause);
			}
		}
		
		
		
		
		System.out.println(raw_sql);
		
		// Do we need to break this into pages?
		int commit_freq = this.step_record.getExtractCommitFreq().intValue();
		
		ResultSet rs;
		try {
			rs = this.sqlQuery(raw_sql, commit_freq);
			
			int rownum = 0;
			
			// Columns to skip
			List<String> skipColNames = new ArrayList<String>();
			skipColNames.add("pk1");
			skipColNames.add("pk2");
			skipColNames.add("pk3");
			skipColNames.add("ok1");
			
			// Get column names
			ResultSetMetaData meta = rs.getMetaData();
			int col_count = meta.getColumnCount();
			String colType, colName;
			List<String> colNames = new ArrayList<String>();
			for (int colIdx = 1; colIdx <= col_count; colIdx++) {
				colType = "";
				colName = "";
				colType  = meta.getColumnTypeName(colIdx);
				colName = meta.getColumnName(colIdx);
				colNames.add(colName);
			}
			
			// TODO:  Make sure our required columns are in the query:
			// ok1, pk1 [pk2-pk3]
			
			// Iterate over all the rows of the ResultSet
			while (rs.next()) {
				rownum++;  // Note: rownum starts at 1
				
				// If this row would exceed step.max_rec,
				// stop processing records here.
				if (rownum > this.max_rec) {
					// Get the current OK1 value
					String lastRowOK1Value = this.convertDateFieldToString(rs, "OK1");
					// Write maxOK1 to the log_dtl record for this extraction step
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMaxOk1(lastRowOK1Value);
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
					break;
				}
				
				// add the data from this row into the output object
				List<Object> rowdata = new ArrayList<Object>();
				for (int ci = 1; ci <= col_count; ci++) {
					rowdata.add(rs.getString(ci));
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
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
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
			
		} catch (SQLException | ParseException e) {
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
			rs = this.sqlQuery(sql, 0);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
	
	private ResultSet sqlQuery(String sql, int num_records) throws SQLException {
		 
		Connection dbConnection = null;
		Statement statement = null;
		ResultSet rs = null;
		//String selectTableSQL = "SELECT * from JOB_DEFINITION ";
 
		try {
			dbConnection = getDBConnection();
			statement = dbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
 
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
			dbConnection = DriverManager.getConnection("jdbc:oracle:thin:@192.168.56.1:1522:xe", "vbatch",
					"vbatch");
			return dbConnection;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return dbConnection;
	}
}
