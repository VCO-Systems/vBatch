package com.vco;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import model.BatchLogDtl;
import model.Step;

public class ExtractDBStep extends StepManager {

	private int record_pointer = 0;
	
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
			
			while (rs.next()) {
				rownum++;
				// TODO: Get this to respect the page size, for now processing all records
				// irrespective of how many sql queries are being fired
				if (!rs.isLast()) { // not working
					
				}
				else {
					//System.out.println("\tLast row of this resultset: " + rownum);
				}
				
				// TODO: add the data from this row into the output object
				List<Object> rowdata = new ArrayList<Object>();
				for (int ci = 1; ci <= col_count; ci++) {
					rowdata.add(rs.getObject(ci));
					
				}
				this.dataPageOut.add(rowdata);
				if (rownum == commit_freq-1) {  // send this page of data to next step
					System.out.println("\t[DB step] Sending page of data");
					this.job_manager.submitPageOfData(this.dataPageOut, this);
					// Reset page data
					this.dataPageOut = new ArrayList<Object>();
					
				}
				
				
			}
		} catch (SQLException e) {
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
		BatchLogDtl log_dtl = new BatchLogDtl();
		log_dtl.setBatchLog(this.job_manager.batch_log);
		
		String msg = "[Step " + this.step_record.getType() 
				+ ":" + this.step_record.getShortDesc()
				+ "] " +  " Started";
		log_dtl.setLongDesc(msg);
		log_dtl.setStepsId(new BigDecimal(this.step_record.getId()));
		log_dtl.setStepsShortDesc(this.step_record.getShortDesc());
		log_dtl.setStepType(this.step_record.getType());
		log_dtl.setStartDt(new Date());
		
		// Commit log entry
		this.job_manager.db.persist(log_dtl);
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
			
			dbConnection.setAutoCommit(false);
			statement.setFetchSize(num_records);
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
