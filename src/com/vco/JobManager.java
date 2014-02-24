package com.vco;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import model.JobMaster;
import model.Step;
import model.VbatchLog;
import model.VbatchLogDtl;

public class JobManager {
	
	private VBatchManager batch_manager;
	private EntityManager db;
	private long job_id;
	private VbatchLog batch_log;
	
	public  JobManager(VBatchManager batch_manager, Integer job_id) {
		// Make sure the job exists, and all necessary config information is available
		this.db = batch_manager.em;
		this.job_id = job_id.longValue();
		
		
		
	}
	
	public void init() {
		

		// Verify existence of the requested job definition
		JobMaster job_master = this.db.find(JobMaster.class, this.job_id);
		
		if (job_master != null) {
			this.db.getTransaction().begin();
			// Log the start of this job in vbatch_log
			VbatchLog batch_log = new VbatchLog();
			this.batch_log = batch_log;
			batch_log.setJobMaster(job_master);
			batch_log.setBatchSeqNbr(new BigDecimal(this.job_id));
			batch_log.setVbatchLogStatus("Started");
			batch_log.setVbatchLogStartDt(new Date());
			batch_log.setBatchNum(new BigDecimal(this.job_id));
			this.db.persist(batch_log);
			this.db.flush();
			this.db.getTransaction().commit();
			// TODO:  we just set batch_num = job_master.job_id.
			// Figure out how to set it to the VbatchLog.id in the same
			// transaction where the id will be set by a sequence/trigger
			//System.out.println(batch_log.getId());
			// Get the steps for this job
			Query steps_qry = this.db.createQuery("SELECT j FROM JobStepsXref j ");
			//steps_qry.setParameter(arg0, arg1)
			List<Step> steps = steps_qry.getResultList();
			System.out.println("Steps for this job: " + steps.size());
			this.db.getTransaction().commit();
		}
		else {
			System.out.println("Did not find job_master: " + this.job_id);
		}
		
		// Once this Job has done its work, call complete() 
		// to make the appropriate log entries, etc
		// this.complete();
		
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
}
