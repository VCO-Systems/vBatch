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
import java.util.Enumeration;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import model.BatchLog;
import model.BatchLogDtl;
import model.JobDefinition;
import model.JobStepsXref;
import model.Step;

import com.VBatchException;
import com.vco.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
//file logging
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;

import java.text.MessageFormat;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class JobManager {
	
	public VBatchManager batch_manager;  // the Batch manager that initiated this job
	public JobDefinition job_definition; // the job_definition record for this job
	public EntityManager db;             // JPA object used to perform logging transactions
	public long job_id;                  // The job id for this job
	public long batch_num;               // The batch number for this job
	public BatchLog batch_log;           // Log entry for this job in BATCH_LOG table
	private List steps = new ArrayList<Step>();
	private List stepManagers = new ArrayList<StepManager>();  // Managers for the steps of this job
	private boolean atLeastOneStepFailed = false;
	private int extract_max_rec_per_file = -1;  // Max # of rows per CSV file
	public static Logger log = null;     // Used to write log entries to the log file for this job
	private Date start_time;
	public String batchMode;             // New or Repeat job?
	
	/**
	 * JobManager is responsible for executing the steps of a job,
	 * passing data between the steps, and handling any errors that
	 * occur during execution of the job.
	 * 
	 * @param batch_manager
	 * @param job_id
	 */
	public  JobManager(VBatchManager batch_manager, Integer job_id)  {
		this.batch_manager = batch_manager;
		this.db = batch_manager.em;
		this.job_id = job_id.longValue();
		log = Logger.getLogger(this.batch_manager.vbatch_version);	
	}
	
	/**
	 * Called once by VBatchManager to begin processing of the job.
	 * @throws Exception
	 */
	public void init() throws Exception {
		try {
			this.start_time = new Date();
			Boolean breakOutOfThisJob = false;
			
			
			/** For a new run, look up the job_definition record, or (if not found)
			 *  abort the job and log it.
			 */
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
					// Log that no job with this id was found.  Abort job.
					log.error("Job # " + this.job_id + " not found.");
					breakOutOfThisJob=true;
				}
				else if (lstMyJobDefs.size() > 1 ) {  // Found more than one matching job
					// More than one job with this id has been defined,
					// so abort the job.
					breakOutOfThisJob=true;
				}
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
				// Found at least on prev run with this batch num
				if (lstBatches.size() > 0) {  
					// The list is sorted most recent first.  Grab the most recent.
					BatchLog prevRunOfThisBatch = lstBatches.get(0);
					this.job_definition = prevRunOfThisBatch.getJobDefinition();
				}
				else if (lstBatches.size() == 0) {
					breakOutOfThisJob=true;
					throw new VBatchException("Could not find any previous runs for batch # " + this.job_id + ".  Aborting job.");
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
		                "WHERE a.jobDefinition = :jid order by a.jobStepSeq";	
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
						
						catch (Exception e) {
							VBatchManager.log.fatal(e.getMessage(), e);
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
				
				// Once this Job has done its work, call logComplete() 
				// to make the appropriate log entries, etc
				this.logComplete();
			}
		}
		catch (Exception e) {
			// Gracefully abort this job.
			this.logFailed(e);
		}
	}
	
	/**
	 * Receive a pageOfData from a step, pass it on to originatingStep.
	 * This is the primary mechanism used to pass data between steps.
	 * @throws Exception 
	 */
	
	public void submitPageOfData(List pageOfData, StepManager originatingStep) throws Exception {
		// Find the next step, since that's where the data needs to be sent
		int nextStepId = this.stepManagers.indexOf(originatingStep) + 1;
		StepManager targetStep = (StepManager) this.stepManagers.get(nextStepId);
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
	 * @throws Exception 
	 */
	private void logStart() throws Exception {
		try {
			// Before opening transaction for batch_log, do any queries
			// necessary to look up batch_num and batch_seq_nbr
			
			Long tempBatchNum = -1L;
			Long tempBatchSeqNbr = -1L;
			// If this is a new run for this job, set batch_num to batch_log_id,
			// and batch_seq_nbr to 1
			if (this.batchMode == VBatchManager.BatchMode_New) {
				tempBatchSeqNbr = 1L;
				VBatchManager.log.info(MessageFormat.format("Initiating new run for Job # {0}", ((int)this.job_id)));
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
					VBatchManager.log.info(MessageFormat.format("Initiating re-run of Batch # {0}, Job # {1}", ((int)this.job_id),latestRun.getJobDefinition().getOrderNum()));
				}
				else {  // Abort this job: no previous runs with this batch number exist
					throw new VBatchException("VBatch error:  No previous runs found for batch_id: " + this.job_id);
					
				}
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
		}
		catch(Exception e) {
			this.logFailed(e);
		}
	}
	
	/**
	 * The job has completed successfully.  make the appropriate log entries, 
	 * and the control will be passed back to VBatchManager.
	 */
	public void logComplete() {
		if (!(this.db.getTransaction().isActive())) {
			this.db.getTransaction().begin();
		}
		
		// Set the batch_log entry for this job to status = complete
		this.batch_log.setEndDt(new Date());
		this.batch_log.setStatus(BatchLog.statusComplete);
		
		this.db.getTransaction().commit();
		this.log.info("Job # " + this.job_id + " completed.");
//		
	}
	
	/**
	 * Mark this job as failed.  Log appropriately.
	 * 
	 * @param e
	 * @throws Exception 
	 */
	private void logFailed(Exception e) throws Exception {
		// If this VBatchException has already been logged, don't repeat it
		if (e instanceof VBatchException && ((VBatchException) e).logged==true) {
			
		}
		else {
			this.log.error(e.getMessage(), e);
			// Mark this exception as having been logged
			if (e instanceof VBatchException) {
				((VBatchException) e).logged=true;
			}
		}
			
		if (!(this.db.getTransaction().isActive())) {
			this.db.getTransaction().begin();
		}
		
		this.batch_log.setStatus(BatchLog.statusError);
		String logfilename = this.getJobLogFilename();
        String errormsg = " [" + logfilename + "] " + e.getMessage();
        // Limit errorMsg to 150 characters to fit in db field
        this.batch_log.setErrorMsg(StringUtils.left(errormsg, 150));
        this.batch_log.setStatus(BatchLog.statusError);
        
	    this.db.persist(batch_log);
	    this.db.getTransaction().commit();
		
	    throw e;
	}
	
	/**
	 * Returns the filename of the log4j file.
	 * 
	 * @return
	 */
	public String getJobLogFilename() {
		String retval = "";
		// Get the name of the log file where user can go for more details about error
		Enumeration en = Logger.getRootLogger().getAllAppenders();
	    while ( en.hasMoreElements() ){
	      Appender app = (Appender)en.nextElement();
	      if ( app instanceof FileAppender ){
	        retval = ((FileAppender) app).getFile();
	      }
	    }
	    return retval;
	}
}
