package com.vco;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
	private JobDefinition job_definition;
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
		// Load the job definition
		this.job_definition = this.db.find(JobDefinition.class, (Object)this.job_id);
		
		// Load this job's steps xref entries
		String queryString = "SELECT a FROM JobStepsXref a " +
                "WHERE a.jobDefinition = :jid";	
		Query query = this.db.createQuery(queryString);
		query.setParameter("jid", this.job_definition);
		List<JobStepsXref> step_xrefs = query.getResultList();
		// Load the actual steps into this.steps
		if (step_xrefs.size() > 0) {
			for (JobStepsXref step_xref: step_xrefs) {
				Step s = step_xref.getStep();
				// Create the StepManager objects for each step
				try {
					// Get the classpath from the step table
					// and create it.
					Class<?> c = Class.forName(s.getClassPath());
					Constructor<?> cons = c.getConstructor(JobManager.class);
					Object step_manager = cons.newInstance(this);
				}
				catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					System.out.println(e);
				}
				
				
				System.out.println(s);
				this.steps.add(s);
				System.out.println(s.getLongDesc());
			}
				
		}
		else {
			// TODO:  Log that no steps were found, end job
			
		}
		
		/**
		 *  Start the job if:
		 *    - the job definition is found
		 *    - at least one steps is configured for the job
		 */
	
		if (job_definition != null && this.steps.size() > 0) {
			// Write log entries showing this job has started
			this.logStart(); 
			
		}
		else {
			// TODO: Log: Error loading this job (job or steps missing)
			System.out.println("Did not find job #: " + this.job_id);
			
		}
		
		// Once this Job has done its work, call complete() 
		// to make the appropriate log entries, etc
		this.logComplete();
		
	}
	
	/**
	 * Log the start of this job to the batchLogDtl table
	 */
	private void logStart() {
		
		this.db.getTransaction().begin();
		
		// Write "
		this.batch_log = new BatchLog();;
		this.batch_log.setJobDefinition(this.job_definition);
		this.batch_log.setBatchSeqNbr(new BigDecimal(this.job_id));
		this.batch_log.setStatus("Started");
		this.batch_log.setStartDt(new Date());
		
		
		this.db.persist(this.batch_log);
		// batch_num must = batch_log.id, but we have to wait until 
		// db.persist is called so ID is populated from sequence.
		// This means we have to write this record out twice, would
		// but don't know a better way currently.
		this.batch_log.setBatchNum(new BigDecimal(this.batch_log.getId()));
		this.batch_log.setLongDesc("Starting batch " + this.batch_log.getBatchNum() 
				+ ": (" + this.job_definition.getId() + ") " 
				+ " " + this.job_definition.getLongDesc());
		
		this.db.persist(this.batch_log);
		this.db.getTransaction().commit();
		
		// Create the batch_log_dtl entry showing this job started
		this.db.getTransaction().begin();
		BatchLogDtl log_dtl = new BatchLogDtl();
		log_dtl.setBatchLog(this.batch_log);
		String msg = "Starting batch " + this.batch_log.getBatchNum();
		msg += ": " + this.job_definition.getLongDesc();
		log_dtl.setLongDesc(msg);
		log_dtl.setStartDt(new Date());
		System.out.println("Job " + this.batch_log.getId() + " started.");
		
		// Commit the batch_log_dtl entry
		this.db.persist(log_dtl);
		this.db.getTransaction().commit();
	}
	
	/**
	 * The job has completed successfully.  make the appropriate log entries, 
	 * and pass control back to VBatchManager.
	 */
	public void logComplete() {
		this.db.getTransaction().begin();
		// Set the batch_log entry for this job to status = complete
		this.batch_log.setEndDt(new Date());
		this.batch_log.setStatus("Complete");
		
		// Show this job complete in the log_dtl table
		// Create the batch_log_dtl entry showing this job started
		BatchLogDtl log_dtl = new BatchLogDtl();
		log_dtl.setBatchLog(this.batch_log);
		String msg = "Completed batch " + this.batch_log.getBatchNum();
		msg += ", " + this.job_definition.getLongDesc();
		log_dtl.setLongDesc(msg);
		log_dtl.setEndDt(new Date());
		this.db.persist(log_dtl);
		
		this.db.getTransaction().commit();
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
