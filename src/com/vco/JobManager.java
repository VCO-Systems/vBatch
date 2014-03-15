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

import com.vco.*;

// file logging
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class JobManager {
	
	public VBatchManager batch_manager;
	public JobDefinition job_definition;
	public EntityManager db;
	private long job_id;
	public BatchLog batch_log;
	private BatchLogDtl log_dtl;
	private List steps = new ArrayList<Step>();
	private List stepManagers = new ArrayList<StepManager>();
	private boolean atLeastOneStepFailed = false;
	
	// file logging
	public static Logger log = Logger.getLogger("vBatch v0.1");
	private String defaultLogFile = "vbatch_{jobid}_{dt}.log";
	
	public  JobManager(VBatchManager batch_manager, Integer job_id) {
		this.batch_manager = batch_manager;
		this.db = batch_manager.em;
		this.job_id = job_id.longValue();
		
		// file logging
		System.out.println("Setting Log4J");
		//PropertyConfigurator.configure("../config/log4j.properties");
		defaultLogFile = defaultLogFile.replace("{jobid}", job_id.toString());
		defaultLogFile = defaultLogFile.replace("{dt}", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
		System.setProperty("logfile",defaultLogFile);
		System.out.println(this.defaultLogFile);
		log.info("Hello World");
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
		this.logStart();
		// Load the actual steps into this.steps
		if (step_xrefs.size() > 0) {
			for (JobStepsXref step_xref: step_xrefs) {
				Step s = step_xref.getStep();
				// Create the StepManager objects for each step
				try {
					// Get the classpath from the step table
					// and create it.
					Class<?> c = Class.forName(s.getClassPath());
					Constructor<?> cons = c.getConstructor(JobManager.class, Step.class);
					// Create new step manager, passing in this job manager, and the step record
					StepManager step_manager = (StepManager) cons.newInstance(this, s);
					// Add the step_manager to this.stepManagers
					this.stepManagers.add(step_manager);
					// Initialize the step
					step_manager.init();
				}
				catch ( SecurityException e) {
					System.out.println(e);
				}
				catch ( ClassNotFoundException e) {
					System.out.println(e);
				}
				catch ( IllegalAccessException e) {
					System.out.println(e);
				}
				catch ( InstantiationException e) {
					System.out.println(e);
				}
				catch ( InvocationTargetException e) {
					System.out.println(e);
				}
				catch ( NoSuchMethodException e) {
					System.out.println(e);
				}
				
				
				this.steps.add(s);
			} // end: looping over step_xref records for this job
			// This job is done.  Make appropriate log entries
			//this.logComplete();
		}
		else {
			// TODO:  Log that no steps were found, end job
			
		}
		
		/**
		 *  Run the job here, as long as:
		 *    - the job definition is found
		 *    - at least one steps is configured for the job
		 */
	
		if (job_definition != null && this.steps.size() > 0) {
			// Write log entries showing this job has started
			 
			// Call start() on each step, in order
			for (int stepNum = 0; stepNum < this.stepManagers.size(); stepNum++) {
				// Get the step manager to start
				StepManager stepToStart = (StepManager) this.stepManagers.get(stepNum);
				System.out.println("Starting step: " + stepNum);
				stepToStart.start();
				
			}
			
			
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
	 * Receive a page of data from a step, pass it on to the next step.
	 */
	
	public void submitPageOfData(List pageOfData, StepManager originatingStep) {
		// Find the next step, since that's where the data needs to be sent
		int nextStepId = this.stepManagers.indexOf(originatingStep) + 1;
		StepManager targetStep = (StepManager) this.stepManagers.get(nextStepId);
//		System.out.println("\t[JobManager] Sending page of data to step: " + targetStep.step_record.getLongDesc());
		targetStep.processPageOfData(pageOfData);
		
	}
	
	
	/**
	 * Log the start of this job to the batchLogDtl table
	 */
	private void logStart() {
		
		this.db.getTransaction().begin();
		
		// Create the main BatchLog entry
		// Note: we keep this in an object var because we'll
		// come back and update this record during and after the
		// job completes.
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
		
		String logMsg = "";
		logMsg += "Batch " + this.batch_log.getBatchNum();
		logMsg += ", Job " + this.job_definition.getId();
		logMsg += " (" + this.job_definition.getLongDesc() + ")";
		this.batch_log.setLongDesc(logMsg);
		
		this.db.persist(this.batch_log);
		this.db.getTransaction().commit();
		
		// Create the batch_log_dtl entry showing this job started
		this.db.getTransaction().begin();
		this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.batch_log);
		String msg = "Starting batch " + this.batch_log.getBatchNum();
		msg += ": " + this.job_definition.getLongDesc();
		this.log_dtl.setLongDesc(logMsg);
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus("Started");
		System.out.println("Batch " + this.batch_log.getBatchNum() + " started.");
		
		// Commit the batch_log_dtl entry
		this.db.persist(this.log_dtl);
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
		//this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.batch_log);
		this.log_dtl.setEndDt(new Date());
		this.log_dtl.setStatus("Completed");
		this.db.persist(this.log_dtl);
		
		this.db.getTransaction().commit();
//		
	}
	
	
	
	
}
