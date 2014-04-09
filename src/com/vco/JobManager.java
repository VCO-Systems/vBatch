package com.vco;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import javax.persistence.TypedQuery;

import model.BatchLog;
import model.BatchLogDtl;
import model.JobDefinition;
import model.JobStepsXref;
import model.Step;

import com.vco.*;

//file logging
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;
import java.text.MessageFormat;

import java.util.Calendar;
import java.text.SimpleDateFormat;

public class JobManager {
	
	public VBatchManager batch_manager;
	public JobDefinition job_definition;
	public EntityManager db;
	public long job_id;
	public long batch_num;
	public BatchLog batch_log;
	private BatchLogDtl log_dtl;
	private List steps = new ArrayList<Step>();
	private List stepManagers = new ArrayList<StepManager>();
	private boolean atLeastOneStepFailed = false;
	private int extract_max_rec_per_file = -1;
	
	private Date start_time;
	

	public String batchMode;  // New or Repeat job?
	
	public  JobManager(VBatchManager batch_manager, Integer job_id) {
		this.batch_manager = batch_manager;
		this.db = batch_manager.em;
		this.job_id = job_id.longValue();
		
	}
	
	
	public void init() {
		this.start_time = new Date();
		Boolean breakOutOfThisJob = false;
		
		/** Populate this.job_definition with the correct job def record**/
		
		// We're doing a new run by job id, so just look up the job def by job #
		if (this.batchMode == VBatchManager.BatchMode_New) {  // new run by job id
			
			// Look up the job definition by JobDefinition.order_num (not id)
			TypedQuery<JobDefinition> qryMyJobDef = this.db.createQuery(
					"SELECT j from JobDefinition j "
					+ "WHERE j.orderNum = :jobOrderNumber "
					+ "order by j.id desc", JobDefinition.class);
			List<JobDefinition> lstMyJobDefs = qryMyJobDef
		    		.setParameter("jobOrderNumber", this.job_id)
		    		.getResultList();
			if (lstMyJobDefs.size() == 1) {  // If we found the job definition 
				this.job_definition = lstMyJobDefs.get(0);
			}
			else if (lstMyJobDefs.size() == 0) { // Didn't find any matching job
				// TODO: Report that no job with this id was found.  Abort job.
				System.out.println("Job # " + this.job_id + " not found.");
				breakOutOfThisJob=true;
			}
			else if (lstMyJobDefs.size() > 1 ) {  // Found more than one matching job
				// TODO:  Report that more than one job with this id has been defined,
				// inform user that job will not run.  Abort job.
				
				breakOutOfThisJob=true;
			}
//			this.job_definition = this.db.find(JobDefinition.class, (Object)this.job_id);
			VBatchManager.log.debug(MessageFormat.format("Initiating new run for Job Id: {0}", ((int)this.job_id)));
		}
		// Here we're re-running a previous batch by batch num, so get the last run
		// of this batch from the logs, and use its job_definition
		else if (this.batchMode == VBatchManager.BatchMode_Repeat) {  // re-run by batch_num
			// Look up the job_definition by batch_num
			TypedQuery<BatchLog> qryPreviousBatch = this.db.createQuery(
					"SELECT log from BatchLog log "
					+ "WHERE log.batchNum = :batchNumber "
					+ "order by log.id desc", BatchLog.class);
			List<BatchLog> lstBatches = qryPreviousBatch
		    		.setParameter("batchNumber", this.job_id)
		    		.getResultList();
			if (lstBatches.size() > 0) {  // Found at least on prev run with this batch num
				// The list is sorted most recent first.  Grab the most recent.
				BatchLog prevRunOfThisBatch = lstBatches.get(0);
				this.job_definition = prevRunOfThisBatch.getJobDefinition();
				VBatchManager.log.debug(MessageFormat.format("Initiating re-run of Batch # {0}, Job # {1}", ((int)this.job_id)));
			}
			else if (lstBatches.size() == 0) {
				System.out.println("Could not find any previous runs for batch # " + this.job_id + ".  Aborting job.");
				breakOutOfThisJob=true;
			}
		}
		
		/** Load the steps_xref entries for this job,
		 *  create new instance of each Step's StepManager class,
		 *  add them to this.stepManagers, and call init() on 
		 *  each step.
		 */
		if (!breakOutOfThisJob) {
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
						Constructor<?> cons = c.getConstructor(JobManager.class, JobStepsXref.class);
						// Create new step manager, passing in this job manager, and the step record
						StepManager step_manager = (StepManager) cons.newInstance(this, step_xref);
						// Add the step_manager to this.stepManagers
						this.stepManagers.add(step_manager);
						
						// Initialize the step
						step_manager.init();
						// If this is the first CSV step for this job, get the
						// extract_max_rec_per_file
						Class j = step_manager.getClass();
						if (step_manager instanceof GenerateCSVStep && this.extract_max_rec_per_file == -1) {
							GenerateCSVStep g = (GenerateCSVStep)step_manager;
							this.extract_max_rec_per_file = g.max_rec_per_file.intValue();
						}
					}
					catch ( SecurityException e) {
						VBatchManager.log.fatal(e);
						//TODO : Exception Stack Trace should be logged
						e.printStackTrace();
					}
					catch ( ClassNotFoundException e) {
						VBatchManager.log.fatal(e);
					}
					catch ( IllegalAccessException e) {
						VBatchManager.log.fatal(e);
					}
					catch ( InstantiationException e) {
						VBatchManager.log.fatal(e);
					}
					catch ( InvocationTargetException e) {
						VBatchManager.log.fatal(e);
					}
					catch ( NoSuchMethodException e) {
						VBatchManager.log.fatal(e);
					}
					
					
					this.steps.add(s);
				} // end: looping over step_xref records for this job
				// This job is done.  Make appropriate log entries
				//this.logComplete();
			}
			else {
				// TODO:  Log that no steps were found, end job
			}
		}
		/**
		 *  Run the job here, as long as:
		 *    - the job definition is found
		 *    - at least one steps is configured for the job
		 */
	
		if (!breakOutOfThisJob && job_definition != null && this.steps.size() > 0) {
			// Write log entries showing this job has started
			 
			// Call start() on each step, in order
			for (int stepNum = 0; stepNum < this.stepManagers.size(); stepNum++) {
				// Get the step manager to start
				StepManager stepToStart = (StepManager) this.stepManagers.get(stepNum);
				stepToStart.start();
				
			}
			
			
		}
		else {
			// TODO: Log: Error loading this job (job or steps missing)
			VBatchManager.log.error(MessageFormat.format("Did not find job #: {0,number,long}", this.job_id));
			
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
	 * Returns the step.extract_max_recs_per_file for the first CSV step in this job.
	 */
	public int getMaxRecPerFile() {
		return this.extract_max_rec_per_file;
	}
	
	
	
	/**
	 * Log the start of this job to the batchLogDtl table
	 */
	private void logStart() {
		// Before opening transaction for batch_log, do any queries
		// necessary to look up batch_num and batch_seq_nbr
		
		Long tempBatchNum = -1L;
		Long tempBatchSeqNbr = -1L;
		// If this is a new run for this job, set batch_num to batch_log_id,
		// and batch_seq_nbr to 1
		if (this.batchMode == VBatchManager.BatchMode_New) {
//					this.batch_log.setBatchNum(new BigDecimal(this.batch_log.getId()));
			//tempBatchNum = new BigDecimal(this.job_id);
			//this.batch_log.setBatchSeqNbr(new BigDecimal(1));
			tempBatchSeqNbr = 1L;
		}
		// Look up last run of this batch, to get batch/seq nbr for this run.
		if (this.batchMode == VBatchManager.BatchMode_Repeat) {
			int highestBatchSeqNbr = 0;
			BatchLog latestRun = null;
			TypedQuery<BatchLog> qryRunsOfThisBatch = this.db.createQuery(
					"SELECT log from BatchLog log WHERE log.batchNum = :bNumber order by log.batchSeqNbr desc", BatchLog.class);
			List<BatchLog> lstRunsOfThisBatch = qryRunsOfThisBatch
		    		.setParameter("bNumber", this.job_id)
		    		.getResultList();
			// If there's at least one previous run with this batch number
			if (lstRunsOfThisBatch.size() > 0 ) {
				// Get the batch_seq_nbr or the latest run
				latestRun = lstRunsOfThisBatch.get(0);
				highestBatchSeqNbr = latestRun.getBatchSeqNbr().intValue();
			}
			else {  // Abort this job: no previous runs with this batch number exist
				System.out.println("VBatch error:  No previous runs found for batch_id: " + this.job_id);
				
			}
//					this.batch_log.setBatchNum(new BigDecimal(this.batch_log.getId()));
//					this.batch_log.setBatchSeqNbr(new BigDecimal(highestBatchSeqNbr+1));
			tempBatchNum = latestRun.getBatchNum();
			tempBatchSeqNbr = highestBatchSeqNbr+1L;
 		}
				
				
		this.db.getTransaction().begin();
		
		// Create the main BatchLog entry
		this.batch_log = new BatchLog();
		this.batch_log.setJobDefinition(this.job_definition);
		
		this.batch_log.setOrderNum(this.job_definition.getOrderNum());
		this.batch_log.setStatus("Started");
		this.batch_log.setStartDt(this.start_time);
		this.batch_log.setShortDesc(this.job_definition.getShortDesc());
		
		//batch_num should be set from sequence BATCH_LOG_BATCH_NUM_SEQ
		String queryString = "SELECT BATCH_LOG_BATCH_NUM_SEQ.NEXTVAL FROM DUAL";	
		Query query = null;
		
		if (this.batchMode == VBatchManager.BatchMode_New) {
			query = this.db.createNativeQuery(queryString);
			List<Number> batchNumList = (List<Number>)query.getResultList();
			this.batch_log.setBatchNum(batchNumList.get(0).longValue());
			
			this.batch_log.setBatchSeqNbr(1L);
		}
		else {
			this.batch_log.setBatchNum(tempBatchNum);
			
			//batch_seq_nbr should be set to max + 1
			this.batch_log.setBatchSeqNbr(tempBatchSeqNbr);
		}
		
		String logMsg = "";
		logMsg += "Batch " + this.batch_log.getBatchNum();
		logMsg += " (seq " + this.batch_log.getBatchSeqNbr() + ")";
		logMsg += ", Job " + this.job_definition.getOrderNum();
		logMsg += " (" + this.job_definition.getLongDesc() + ")";
		this.batch_log.setLongDesc(logMsg);
		
		this.db.persist(this.batch_log);
		this.db.getTransaction().commit();
		
		/* This Logging for BATCHLogDtl will be done for each Step
		// Create the batch_log_dtl entry showing this job started
		// TODO : This should really be logged in StepManager
		this.db.getTransaction().begin();
		this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.batch_log);
		String msg = "Starting batch " + this.batch_log.getBatchNum();
		msg += ": " + this.job_definition.getLongDesc();
		this.log_dtl.setLongDesc(logMsg);
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus("Started");
		//this.log_dtl.setJobStepsXrefJobStepSeq(this.job_definition.getJobStepsXrefs());
		System.out.println("Batch " + this.batch_log.getBatchNum() + " started.");
		
		// Commit the batch_log_dtl entry
		this.db.persist(this.log_dtl);
		this.db.getTransaction().commit();
		*/
	}
	
	/**
	 * The job has completed successfully.  make the appropriate log entries, 
	 * and pass control back to VBatchManager.
	 */
	public void logComplete() {
		this.db.getTransaction().begin();
		// Set the batch_log entry for this job to status = complete
		this.batch_log.setEndDt(new Date());
		this.batch_log.setStatus(BatchLog.statusComplete);
		
		// Show this job complete in the log_dtl table
		// Create the batch_log_dtl entry showing this job started
		//this.log_dtl = new BatchLogDtl();
		/*
		this.log_dtl.setBatchLog(this.batch_log);
		this.log_dtl.setEndDt(new Date());
		this.log_dtl.setStatus(BatchLog.statusComplete);
		this.db.persist(this.log_dtl);
		*/
		
		this.db.getTransaction().commit();
//		
	}
	
	public static void main(String[] args) {
		System.out.println("log4j testing");
		//BasicConfigurator.resetConfiguration();//enough for configuring log4j
		//BasicConfigurator.configure();
	      
        //Logger.getRootLogger().setLevel(Level.TRACE); //changing log level

		System.setProperty("logfile","SAMPLE1.log");
		VBatchManager.log = Logger.getLogger("vBatch v0.1");
		
		//PropertyConfigurator.configure("/tmp/log4j.properties");
		VBatchManager.log.error("Critical message, almost fatal");
		VBatchManager.log.warn("Warnings, which may lead to system impact");
		VBatchManager.log.info("Information");
		VBatchManager.log.debug("Debugging information ");
		VBatchManager.log.debug(MessageFormat.format("TEST {0,number,long}, string {1}", 1,"Chris"));

		//log.info("hello world!");
		System.out.println ("log4j done");
	}
	
	
}
