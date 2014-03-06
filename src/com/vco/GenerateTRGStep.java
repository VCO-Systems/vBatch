package com.vco;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
	
	public GenerateTRGStep(JobManager jm, Step step_record) {
		System.out.println("\t[TRG] constructor");
		this.job_manager = jm;
		this.step_record = step_record;
		
		// Intialize vars
		this.csvFilenames = new ArrayList<String>();
			
	}
	
	public boolean start() {
		// Get the batch_log entry for this job
		BatchLog batchLogEntry = this.job_manager.batch_log;
		
		// Get list of CSV files generated by this Job
		List<String> csvFilenames = new ArrayList<String>();
		TypedQuery<BatchLogFileOutput> query = this.job_manager.db.createQuery(
		        "SELECT logs FROM BatchLogFileOutput logs WHERE logs.batchLog = :batchId "
		        , BatchLogFileOutput.class);  
		    List<BatchLogFileOutput> logs = query
		    		.setParameter("batchId", batchLogEntry)
		    		.getResultList();
		
		for (BatchLogFileOutput csvFile: logs) {
			csvFilenames.add(csvFile.getFilename());
		}
		System.out.println("\t[TRG] Generating " + csvFilenames.size() + " .trg files");
		if (csvFilenames.size() > 0 ) {
			for (String csvFilename : csvFilenames) {
				
				// Make sure filename ends in .csv
				if (csvFilename.toLowerCase().endsWith(".csv")) {
					
					csvFilename = csvFilename.toLowerCase().replace(".csv", ".trg");
					System.out.println("\t[TRG] filename: " + csvFilename);
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
			}
		}
		else { // 0 CSV files generated by this job
			
		}
		return true;  // tell JobManager we finished successfully
	}
}
