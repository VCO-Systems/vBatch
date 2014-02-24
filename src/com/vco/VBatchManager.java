package com.vco;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import model.JobMaster;

public class VBatchManager {

	private static final String PERSISTENCE_UNIT_NAME = "vbatch";
	private static EntityManagerFactory factory;
	protected EntityManager em;
	
	public VBatchManager() {
		// Set up db connection
		factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		this.em = factory.createEntityManager();
	}
	
	public void init_old() {
		// Set up db connection
		factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		this.em = factory.createEntityManager();
		this.em.getTransaction().begin();
		// Create new job_master record
		JobMaster job = new JobMaster();
		job.setName("Dummy Job");
		job.setDescription("Simple test job.");
		this.em.persist(job);
		this.em.getTransaction().commit();
		this.em.close();
		factory.close();
	}
	
	/**
	 * Constructor that takes a list of job_ids, and manages the execution
	 * of each job.
	 * 
	 * @param job_ids
	 */
	public void init(ArrayList<Integer> job_ids) {
		// Make sure we have all necessary configuration information for each job,
		// 
		for (Integer job_id : job_ids) {
			JobManager job_manager = new JobManager(this, job_id);
			job_manager.init();
		}
	}
	
	public void getRequestedJobs() {
		
	}
}
