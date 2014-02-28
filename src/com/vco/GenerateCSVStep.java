package com.vco;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.quote.QuoteMode;

import model.BatchLogDtl;
import model.Step;

public class GenerateCSVStep extends StepManager {

	// Keep track of rows and paging
	private int totalRowsGenerated = 0;
	private int totalRowsThisFile = 0;
	private int max_rec_per_file = 0;
	private int pageCount=0; // Pages of data sent in from another step (not necessarily db or CSV pages)
	
	// Track the generated CSV Files
	private int totalFilesGenerated = 0;
	private List<String> generatedFilenames;
	FileWriter currentOutputFile = null;
	private String defaultCSVFilename = "vbatch_{dt}_W914_{seq}.csv";
	
	// private vars
	private String startingDateTime;
	
	// CSV generation
	ICsvListWriter listWriter = null;
	private static final CsvPreference ALWAYS_QUOTE = 
		    new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE).useQuoteMode(new AlwaysQuoteMode()).build();
	
	
	public GenerateCSVStep(JobManager jm, Step step_record) {
		this.job_manager = jm;
		this.step_record = step_record;
		
		// If this step has a filename prefix defined, use that.  Otherwise use the default.
		if (this.step_record.getOutputFilenamePrefix() != "") {
			this.defaultCSVFilename = this.step_record.getOutputFilenamePrefix();
		}
		this.generatedFilenames = new ArrayList<String>();
		
		// Get extract_max_rec_per_file from the step config
		if (this.step_record.getExtractMaxRecPerFile() != null) {
			this.max_rec_per_file = this.step_record.getExtractMaxRecPerFile().intValue();
		}
			
		System.out.println("maxrec: " + this.max_rec_per_file);
	}
	
		/**
	 * Basic workflow methods
	 */
	
	@Override
	public boolean init() {
		try {
//			listWriter = new CsvListWriter(new FileWriter("output/vbatch_sample_out.csv"),
//					ALWAYS_QUOTE);
			
			// Configure csv writer to always quote every field
			
			// The startingDateTime should be the same for every file 
			// this step generates, so we create it during init.
			DateFormat df = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
			Date today = Calendar.getInstance().getTime();
			this.startingDateTime = df.format(today);
			
			// Create the first CSV file
//			this.generateNextCSVFile();
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

		// Prepare to process this page of data
		this.pageCount++;
		
		/*
		Iterator<Object> data = pageOfData.iterator();
		while (data.hasNext()) {
			Object item = data.next();
			listWriter.write()
			// GENERATE
		}
		*/
		
		System.out.println("\t[CSV] rows to process: " + pageOfData.size());
		
		try {
			/*
			Iterator<Object> rows = pageOfData.iterator();
			int rowcount=0;
			while (rows.hasNext()) {
				rowcount++;
				ArrayList<String> row = (ArrayList<String>)rows.next();
				//System.out.println(row.get(0));
				listWriter.write(row);
				// GENERATE
			}
			System.out.println("\t[CSV] row count: " + rowcount);
			//listWriter.write(pageOfData);
			 */
			
			// Loop over the rows of data in this page
			for (int i = 0; i < pageOfData.size(); i++) {
				
				
				// See if we need to close out this file
				if (this.totalRowsThisFile + 1 > this.max_rec_per_file) {
					// TODO: close out the file
					this.currentOutputFile.flush();
					this.currentOutputFile.close();
					this.currentOutputFile = null;
					this.totalRowsThisFile=0;
				}
				else {
					
					// TODO: Update the counters
				}
				
				// See if a new file needs to be created
				// this.currentOutputFile will be null if this is the first record for
				// the job, or if the previous file was closed out after writing the last row.
				if (this.currentOutputFile == null) {
					this.generateNextCSVFile();
				}
				
				// Write this row to the CSV
				List row = (List)pageOfData.get(i);
				//listWriter.write(row);
				String rowStr = this.generateCSVRow(row);
				this.currentOutputFile.append(rowStr);
				this.currentOutputFile.flush();
				
				// Update the counters
				this.totalRowsThisFile++;
				this.totalRowsGenerated++;
				
			}
			
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		finally {
			
		}
		
		return true;
	}
	
	/**
	 * Generate the next CSV file.
	 */
	
	private void generateNextCSVFile() {
		
		try {
			// Increment the counters
			this.totalFilesGenerated++;
			
			// Construct the filename for this output file
			String newFileName = this.defaultCSVFilename;
			newFileName = newFileName.replace("{dt}", this.startingDateTime);
			newFileName = newFileName.replace("{seq}", Integer.toString(this.totalFilesGenerated));
			newFileName = newFileName.replace("{batch_num}",  this.job_manager.batch_log.getBatchNum().toString());
			
			// Create the file
			this.currentOutputFile = new FileWriter("output/" + newFileName,true);
			this.generatedFilenames.add("output/" + newFileName);
			
			// Reset counters
			this.totalRowsThisFile=0;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Converts a row of data (list of strings) into a CSV-formatted String.
	 * @param row
	 * @return String
	 */
	
	private String generateCSVRow(List row) {
		//this.totalRowsGenerated++;
		String retval = "";
		for (int i=0; i < row.size(); i++) {
			String fieldVal = (String) row.get(i);
			retval += "\"";
			if (row.get(i) != null) {
				retval += row.get(i);
			}
			retval += "\"";
			// Add comma after field (unless last field)
			String comma = (i < row.size() - 1) ? "," : "";
			retval += comma;
		}
		retval += "\n";
		return retval;
	}
	
	@Override
	public boolean finish() {
		
		return true;
	}
	
	/************************
	 * Logging
	 * 
	 ************************/
	
	/**
	 * This Step is starting.  Make the appropriate log entries.
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
