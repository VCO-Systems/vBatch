package com.vco;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.TypedQuery;

import model.BatchLog;
import model.BatchLogDtl;
import model.BatchLogFileOutput;
import model.JobStepsXref;
import model.Step;

/**
 * For each CSV file generated from this job, output .trg files
 * with matching names.
 * 
 * @author vco
 *
 */
public class GenerateTRGStep extends StepManager {

	// Keep track of generated TRG files
	private List<String> csvFilenames;
	
	// Logging
	private BatchLogDtl log_dtl;
	
	public GenerateTRGStep(JobManager jm, JobStepsXref jobStepXref) {
		this.job_manager = jm;
		this.jobStepXref = jobStepXref;
		
		// Intialize vars
		this.csvFilenames = new ArrayList<String>();
			
	}
	
	public boolean start() {
		// If this is the first page of data we've received,
		// and log the start of the step
		if (!this.running) {
			// Set this.running = true so we know this step has been
			// started, even if start() has not been called yet.
			this.running=true;
			this.logStart();
		}
		else {  // This step has already processed data
			// Just write out the completed logs and this step is finished
			
		}
		
		// Mark this job complete
		this.running=false;
		this.completed=true;
		this.failed=false;
		
		this.logComplete();
		return true;  // tell JobManager we finished successfully
	}
	
	/**
	 * Some other step has passed in a list of .csv files, 
	 * via the job manager, and we will generate matching
	 * .trg files.
	 * 
	 */
	@Override
	public boolean processPageOfData(List<Object> pageOfData) {
		// If this is the first page of data we've received,
		// and log the start of the job
		if (!this.running) {
			// Set this.running = true so we know this step has been
			// started, even if start() has not been called yet.
			this.running=true;
			this.logStart();
		}
		
		for (int i = 0; i < pageOfData.size(); i++) {
			String csvFilename = (String)pageOfData.get(i);
			generateTRGFile(csvFilename);
		}
		
		// Tell job_manager there were no errors
		return true;
	}

	/**
	 * @param csvFilename
	 * @return
	 */
	private String generateTRGFile(String csvFilename) {
		// If this is the first time this step has processed data,
		// mark the step as started
		if (!this.running){
			this.running=true;
			this.logStart();
		}
		// Make sure filename ends in .csv
		if (csvFilename.toLowerCase().endsWith(".csv")) {
			// Replace .csv and the end of file with .trg
			int cnt = 0;
			int idx = csvFilename.toLowerCase().indexOf(".csv");
			while (idx >= 0) {
				cnt++;
				idx = csvFilename.toLowerCase().indexOf(".csv", idx+1);
			}
			if (cnt > 0) {  // we found .csv at least once
				// replace the last occurence with .trg
				csvFilename = csvFilename.substring(0, csvFilename.length() - 4 ).concat(".trg");
			}
			
			// Generate the .trg file
			File newFile = new File(csvFilename);
			try {
				newFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		else {
			// LOG:  skipping file, does not end with ".csv"
		}
		return csvFilename;
	}
	
	private void logStart() {
//		this.job_manager.db.getTransaction().begin();
		// Create entry in batch_log_dtl
		this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.job_manager.batch_log);
		
		String msg = "Step [" + this.jobStepXref.getStep().getType() 
				+ " : " + this.jobStepXref.getStep().getShortDesc()
				+ "]";
		this.log_dtl.setLongDesc(msg);
		this.log_dtl.setStepsId((Long)this.jobStepXref.getId());
		this.log_dtl.setStepsShortDesc(this.jobStepXref.getStep().getShortDesc());
		this.log_dtl.setStepType(this.jobStepXref.getStep().getType());
		this.log_dtl.setJobStepsXrefJobStepSeq(this.jobStepXref.getJobStepSeq());
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus("Started");
		
		// Log job settings used to run this job
		this.log_dtl.setClassPath(this.jobStepXref.getStep().getClassPath());
		
		// Commit log entry
		this.job_manager.db.persist(this.log_dtl);
//		this.job_manager.db.getTransaction().commit();
		
		System.out.println("\t" + msg);
	}
	
	
	
	private void logComplete() {
//		this.job_manager.db.getTransaction().begin();
		
		String msg = "Step [" + this.jobStepXref.getStep().getType() 
				+ " : " + this.jobStepXref.getStep().getShortDesc()
				+ "]";
		this.log_dtl.setEndDt(new Date());
		this.log_dtl.setStatus(BatchLog.statusComplete);
		
		// Commit log entry
		this.job_manager.db.persist(this.log_dtl);
//		this.job_manager.db.getTransaction().commit();
		
//		System.out.println("\t" + msg);
	}
}
