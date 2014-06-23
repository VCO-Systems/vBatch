package com.vco;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import model.JobDefinition;

import org.supercsv.cellprocessor.ParseInt;

public class VBatchUtilities {

	// Jobs to be worked on
	public ArrayList<String> job_ids = new ArrayList<String>();  // String job ids passed in from CLI
	private ArrayList<Integer> job_ids_int = new ArrayList<Integer>();  // job_ids converted to int
	public String requested_job_date = new String();  // new effective date passed in from CLI
	
	// JPA/JDBC variables
	public EntityManager db;             // JPA object used to access vBatch tables
	private VBatchManager batch_manager;
	
	public void VBatchUtilities(VBatchManager batch_manager) {
		this.batch_manager = batch_manager;
		this.db=this.batch_manager.em;
	}
	
	public void init() {
		
	}
	
	/**
	 * Checks if requirements have been met to update one (or more) jobs'
	 * effective dates.  If so, passes request(s) to setJobDate() for each job
	 * 
	 * @throws Exception
	 */
	
	public void setJobDates() throws Exception {
		Date requested_job_date_dt;
		// check for required settings
		System.out.println("setJobDates()");
		if (this.job_ids.size() != 0  && this.requested_job_date.length()>0) {
			System.out.println("About to set eff date (" + requested_job_date + ") for jobs: " + job_ids.toString());
		}
		
		// Convert the job_date into a Java date object
		try {
			requested_job_date_dt = new SimpleDateFormat("MM/dd/yyyy-H:m:s", Locale.ENGLISH).parse(this.requested_job_date);
		}
		catch (Exception e) {
			throw(e);
		}
		
		// Job numbers are passed from command-line as strings
		// (because they can either be a number or "all").
		// From these strings, generate a list of job_ids as 
		// integers.
		if (this.job_ids.size()>0 ) {
			// TODO: build a list of job numbers
			if (this.job_ids.get(0).equals("all")) {
				// TODO: get list of all job numbers
				TypedQuery<JobDefinition> qryMyJobDef = this.db.createQuery(
						"SELECT j from JobDefinition j "
						+ "WHERE j.orderNum = :jobOrderNumber "
						+ "order by j.id desc", JobDefinition.class);
			}
			else {  
				// TODO: get list of job numbers that were passed in
				for (String each_job_id : this.job_ids) {
					this.job_ids_int.add(Integer.parseInt(each_job_id));
				}
			}
			
			// TODO: pass each job number to setJobDate() to be set
		}
		
		// Process each of these jobs with the requested effective date
		for (Integer job : job_ids_int) {
			this.setJobDate(job, requested_job_date_dt);
		}
	}
	
	/**
	 * Updates the given job's effective date to requested_date.
	 * 
	 * @param job_id
	 * @param requested_date
	 */
	private void setJobDate(Integer job_id, Date requested_date) {
		
	}
	
}
