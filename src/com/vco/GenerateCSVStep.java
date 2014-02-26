package com.vco;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import model.BatchLogDtl;
import model.Step;

public class GenerateCSVStep extends StepManager {

	ICsvListWriter listWriter = null;
	
	public GenerateCSVStep(JobManager jm, Step step_record) {
		this.job_manager = jm;
		this.step_record = step_record;
	}
	
	/**
	 * Basic workflow methods
	 */
	
	@Override
	public boolean init() {
		try {
			listWriter = new CsvListWriter(new FileWriter("output/vbatch_sample_out.csv"),
                    CsvPreference.STANDARD_PREFERENCE);
		}
		catch(IOException e) {
			System.out.println(e);
		}
		finally {
			/**
			if( this.listWriter != null ) {
                try{
                	this.listWriter.close();
                }
                catch (IOException e) {
                	
                }
                
                }
              **/
				
			
		}
		return true;
	}
	
	@Override
	public boolean start() {
		// Set this step to running
		this.running = true;
		// Log the start of this step
		this.logStart();
		
		// TODO: Loop over pages of data
		// STUB: Fake a page of data
		List<Object> lst = new ArrayList<>();
		
		// TODO: Send each page of data to JobManager
		//this.job_manager.submitPageOfData(lst, this);
		return true;
		
	}
	
	@Override
	public boolean processPageOfData(List<Object> pageOfData) {
		/*
		Iterator<Object> data = pageOfData.iterator();
		while (data.hasNext()) {
			Object item = data.next();
			listWriter.write()
			// GENERATE
		}
		*/
		
		try {
			Iterator<Object> rows = pageOfData.iterator();
			while (rows.hasNext()) {
				Object row = rows.next();
				//System.out.println(row);
				listWriter.write(row);
				// GENERATE
			}
			//listWriter.write(pageOfData);
		}
		catch(IOException e) {
			
		}
		
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
		
		String msg = "[StepManager] Step started: " + this.step_record.getLongDesc();
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
}
