package com.vco;

import java.math.BigDecimal;
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
}
