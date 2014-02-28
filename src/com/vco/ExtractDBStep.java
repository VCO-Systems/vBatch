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

import javax.persistence.Query;

import model.BatchLogDtl;
import model.Step;

public class ExtractDBStep extends StepManager {

	private int record_pointer = 0;
	private BatchLogDtl log_dtl = null;
	
	// vars specific to this type of step
	
	
	public ExtractDBStep(JobManager jm, Step step_record) {
		this.job_manager = jm;
		this.step_record = step_record;
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
		
		// TODO: Loop over pages of data

		// Get the raw sql to run
		String raw_sql = this.step_record.getExtractSql();
		// System.out.println(raw_sql);
		
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
				rownum++;  // Note: First rownum is 1
				
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
					this.job_manager.submitPageOfData(this.dataPageOut, this);
					// Reset page data
					this.dataPageOut = new ArrayList<Object>();
					
				}
				
				// Any special processing for the last row goes here
				if (rs.isLast()) {
					// rownum = (the last row number)
					
					
					// Get the current OK1 value
					String lastRowOK1Value = this.convertDateFieldToString(rs, "OK1");
					System.out.println("\t{}: " + lastRowOK1Value);
					// Write maxOK1 to the log_dtl record for this extraction step
					this.job_manager.db.getTransaction().begin();
					this.log_dtl.setMaxOk1(lastRowOK1Value);
					this.job_manager.db.persist(this.log_dtl);
					this.job_manager.db.getTransaction().commit();
				}
			}
			// Send any remaining records to the next step
			if (this.dataPageOut.size() > 0) {
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
		SimpleDateFormat incomingDateFormat  = new SimpleDateFormat("y-M-d HH:mm:ss.S");
		SimpleDateFormat outgoingDateFormat = new SimpleDateFormat("M/d/y HH:mm:ss a");
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
		
		String msg = "[Step " + this.step_record.getType() 
				+ ":" + this.step_record.getShortDesc()
				+ "] " +  " Started";
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
			//statement = dbConnection.createStatement();
			//dbConnection.setAutoCommit(false);
			//statement.setFetchSize(num_records);
			//System.out.println(selectTableSQL);
 
			// execute select SQL stetement
			rs = statement.executeQuery(sql);
			
			
 
		} catch (SQLException e) {
 
			System.out.println(e.getMessage());
 
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
