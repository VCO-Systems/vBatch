package com.vco;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import model.BatchLog;
import model.BatchLogDtl;
import model.JobDefinition;
import model.JobStepsXref;
import model.Step;

public class JobManager {
	
	private VBatchManager batch_manager;
	private EntityManager db;
	private long job_id;
	private BatchLog batch_log;
	private List steps = new ArrayList<>();
	
	public  JobManager(VBatchManager batch_manager, Integer job_id) {
		this.batch_manager = batch_manager;
		this.db = batch_manager.em;
		this.job_id = job_id.longValue();
	}
	
	public void init() {
		// Simple select query using straight JDBC
//		try{
//			selectRecordsFromDbUserTable();
//		}
//		catch (SQLException e) {
//			System.out.println(e);
//		}
		
		System.out.println("Look for job_id: " + this.job_id);
		
		// Load the job definition
		JobDefinition job_definition = this.db.find(JobDefinition.class, (Object)this.job_id);
		
		// Load this job's steps
		String queryString = "SELECT a FROM JobStepsXref a " +
                "WHERE a.jobDefinition = :jid";	
		Query query = this.db.createQuery(queryString);
		query.setParameter("jid", job_definition);
		List<JobStepsXref> step_xrefs = query.getResultList();
		// Get the steps
		if (step_xrefs.size() > 0) {
			for (JobStepsXref step_xref: step_xrefs) {
				Step s = step_xref.getStep();
				System.out.println(s);
				this.steps.add(s);
				System.out.println(s.getLongDesc());
			}
				
		}
		else {
			// ERROR:  No steps found for this job
			
		}
		
		/**
		 *  Start the job if:
		 *    - the job definition is found
		 *    - at least one steps is configured for the job
		 */
	
		if (job_definition != null && this.steps.size() > 0) {
			this.db.getTransaction().begin();
			// Log the start of this job in vbatch_log
			BatchLog batch_log =  new BatchLog();
			this.batch_log = batch_log;
			this.batch_log.setJobDefinition(job_definition);
			this.batch_log.setBatchSeqNbr(new BigDecimal(this.job_id));
			this.batch_log.setVbatchLogStatus("Started");
			this.batch_log.setStartDt(new Date());
			this.batch_log.setEndDt(new  Date());
			System.out.println(new Date());
			this.batch_log.setBatchNum(new BigDecimal(this.job_id));
			this.db.persist(this.batch_log);
			this.db.flush();
			
			System.out.println("ID: " + this.batch_log.getId());
			this.logStart(); 
			this.db.getTransaction().commit();
			// TODO: get the record we just comitted so we have its id
//			batch_log = this.db.find(BatchLog.class, batch_log.getId());
//			this.db.refresh(batch_log);
//			System.out.println(batch_log.getId());
			
			// TODO:  we just set batch_num = job_master.job_id.
			// Figure out how to set it to the VbatchLog.id in the same
			// transaction where the id will be set by a sequence/trigger
			//System.out.println(batch_log.getId());
			// Get the steps for this job
		}
		else {
			System.out.println("Did not find job #: " + this.job_id);
		}
		
		// Once this Job has done its work, call complete() 
		// to make the appropriate log entries, etc
		// this.complete();
		
	}
	
	private void logStart() {
		System.out.println("ID in logStart() : " + this.batch_log.getId());
		//this.db.refresh(this.batch_log);
		//System.out.println(this.batch_log.getId());
		//this.db.getTransaction().begin();
		//this.db.refresh(batch_log);
		BatchLogDtl log_dtl = new BatchLogDtl();
		log_dtl.setBatchLog(this.batch_log);
		log_dtl.setLongDesc("Starting job: " + batch_log.getId());
		this.db.persist(log_dtl);
		this.db.flush();
		//this.db.getTransaction().commit();
		//log_dtl.setStatus("Job " + this.batch_log.getId() + " started.");
		System.out.println("Job " + this.batch_log + " started.");
	}
	
	/**
	 * The job has completed successfully.  make the appropriate log entries, 
	 * and pass control back to VBatchManager.
	 */
	public void complete() {
		// Log job completion in vbatch_log_dtl
//		this.db.getTransaction().begin();
//		VbatchLogDtl dtl = new VbatchLogDtl();
//		dtl.setStatus("Completed");
//		dtl.setLogDtlEndDt(new Date());
//		// Get the latest vbatch_log entry here
//		Query q = this.db.createQuery("SELECT x FROM VbatchLog x ");
//		q.setMaxResults(1);
//		q.
//		VbatchLog results = (VbatchLog) q.getSingleResult();
//		dtl.setVbatchLog(results);
//		
//		this.db.persist(dtl);
//		this.db.getTransaction().commit();
//		
	}
	
	private  void selectRecordsFromDbUserTable() throws SQLException {
		 
		Connection dbConnection = null;
		Statement statement = null;
 
		String selectTableSQL = "SELECT * from JOB_DEFINITION ";
 
		try {
			dbConnection = getDBConnection();
			statement = dbConnection.createStatement();
 
			System.out.println(selectTableSQL);
 
			// execute select SQL stetement
			ResultSet rs = statement.executeQuery(selectTableSQL);
			
			while (rs.next()) {
 
				String long_Desc = rs.getString("LONG_DESC");
				System.out.println(long_Desc);
 
			}
 
		} catch (SQLException e) {
 
			System.out.println(e.getMessage());
 
		} finally {
 
			if (statement != null) {
				statement.close();
			}
 
			if (dbConnection != null) {
				dbConnection.close();
			}
 
		}
 
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
