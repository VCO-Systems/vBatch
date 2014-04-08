package com.vco;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import model.BatchLog;
import model.BatchLogDtl;
import model.BatchLogFileOutput;
import model.JobStepsXref;
import model.Step;

public class GenerateCSVStep extends StepManager {
	
	// Step configuration
	public Boolean quoteAllOutputFields = false;

	/**
	 * This step uses the optional mode_special flag
	 * to indicate that it does not output data for
	 * each page to the next step.  Instead, this step
	 * adds data to alternateOutputData during processing,
	 * and once this step completes, later steps may
	 * request the resulting data from that variable.
	 */
	
	Boolean outputsAlternateData = true;
	
	public List<String> alternateOutputData;
	
	/**
	 * This step waits until completed, and then makes available
	 * a custom List of values.  In this case, it's a list of
	 * filenames for generated CSVs.
	 */
	
	// Keep track of rows and paging

	private Long totalRowsGenerated = 0L;
	private Long totalRowsThisFile = 0L;
	public Long max_rec_per_file = 0L;
	private Long pageCount=0L; // Pages of data sent in from another step (not necessarily db or CSV pages)

	private BatchLogDtl log_dtl;
	
	// Track the generated CSV Files
	private int totalFilesGenerated = 0;
	
	FileWriter currentOutputFile = null;
	String currentOutputFilename = new String();
	private String defaultCSVFilename = "vbatch_{dt}_W914_{seq}.csv";
	
	// private vars
	private String startingDateTimeStr;
	private Date startingDateTime;
	
	// CSV generation
	ICsvListWriter listWriter = null;
	private static final CsvPreference ALWAYS_QUOTE = 
		    new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE).useQuoteMode(new AlwaysQuoteMode()).build();
	
	
	public GenerateCSVStep(JobManager jm, JobStepsXref jobStepXref) {
		this.job_manager = jm;
		this.jobStepXref = jobStepXref;
		
		// If this step has a filename prefix defined, use that.  Otherwise use the default.
		if (this.jobStepXref.getStep().getOutputFilenamePrefix() != "") {
			this.defaultCSVFilename = this.jobStepXref.getStep().getOutputFilenamePrefix();
		}
		this.alternateOutputData = new ArrayList<String>();
		
		// Get extract_max_rec_per_file from the step config
		if (this.jobStepXref.getStep().getExtractMaxRecPerFile() != null) {
			this.max_rec_per_file = this.jobStepXref.getStep().getExtractMaxRecPerFile();
		}
			
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
			this.startingDateTime = Calendar.getInstance().getTime();
			this.startingDateTimeStr = df.format(this.startingDateTime);
			
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
		// Previous steps have already passed in data,
		// even though start() is just now being called.
		if (this.running) {
			// This means the CSV's have been generated, except
			// perhaps the last one.
			
			// Write out the last CSV
//			try {
//				this.closeCurrentOutputFile();
//				String j = "Do Nothing";
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			// Mark this step as complete
			this.completed=true;
			this.failed=false;
			
			// Mark this step complete in the logs
			this.logComplete();
			
		}
		else {
			// Log the start of this step
			this.running = true;
			//this.logStart();
		}
		
		// TODO: Loop over pages of data
		// STUB: Fake a page of data
		List<Object> lst = new ArrayList<Object>();
		
		// Return execution to JobManager
		return true;
		
	}
	
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
		
		// System.out.println("\t[CSV] rows to process: " + pageOfData.size());
		
		try {
			// Loop over the rows of data in this page
			for (int i = 0; i < pageOfData.size(); i++) {
				// Create the csv file before outputting the first row
				if (this.currentOutputFile == null && i == 0) {
					this.currentOutputFilename = this.generateNextCSVFile();
				}
				
				// Write this row to the CSV
				List row = (List)pageOfData.get(i);
				String rowStr = this.generateCSVRow(row);
				this.currentOutputFile.append(rowStr);
				this.currentOutputFile.flush();
				
				// Update the counters
				this.totalRowsThisFile++;
				this.totalRowsGenerated++;
			}
			
			int pos = this.currentOutputFilename.length()-5;
			String newFilename = this.currentOutputFilename.substring(0,pos) + this.currentOutputFilename.substring(pos).replaceFirst(".tmp", ".csv");
			System.out.println("\t[CSV] Generated CSV file: " + newFilename 
					+ " (" + this.totalRowsThisFile + " rows)");
			
			// Close the output file
			closeCurrentOutputFile();
			// Rename the .tmp to .csv
			renameTMPToCSV();
			
			// Send the .tmp file to GenerateTRGStep
			this.job_manager.submitPageOfData(this.alternateOutputData, this);
			// Clear the list of generated csvs since we've generated the .tmp
			this.alternateOutputData = new ArrayList<String>();
			
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		finally {
			
		}
		
		return true;
	}

	/**
	 * 
	 */
	private boolean renameTMPToCSV() {
		int pos = this.currentOutputFilename.length()-5;
		String newFilename = this.currentOutputFilename.substring(0,pos) + this.currentOutputFilename.substring(pos).replaceFirst(".tmp", ".csv");
		// Open the tmp file
		File tmpFile = new File("output/" + this.currentOutputFilename);
		File csvFile = new File("output/" + newFilename);
		// Make sure the csv file doesn't already exist
		if (csvFile.exists())
			try {
				throw new java.io.IOException("Cannot rename .tmp to .csv (file already exists): " + this.currentOutputFilename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		boolean success = tmpFile.renameTo(csvFile);
		
		// Update this filename in this.alternateOutputData as well,
		// since that will be sent to the trg step
		this.alternateOutputData.set(0,	this.alternateOutputData.get(0).replace(".tmp", ".csv"));
		
		return success;
		
	}

	/**
	 * Close an existing CSV file and log it
	 * in batch_log_file_output table.
	 * @throws IOException
	 */
	private void closeCurrentOutputFile() throws IOException {
		// rows
		Long rows = this.totalRowsThisFile;
		
		// Close the output file
		if (this.currentOutputFile != null) {
			this.currentOutputFile.flush();
			this.currentOutputFile.close();
			this.currentOutputFile = null;
		}
		
		
		// Update counters
		this.totalRowsThisFile=0L;
		
		// Create new entry in batch_log_file_output log table for this file
//		this.job_manager.db.getTransaction().begin();
		BatchLogFileOutput log_entry = new BatchLogFileOutput();
		log_entry.setBatchLog(this.job_manager.batch_log);
		log_entry.setFilename(this.alternateOutputData.get(this.alternateOutputData.size()-1));
		log_entry.setNumRecords(rows);
		log_entry.setCreateDt(this.startingDateTime);
		this.job_manager.db.persist(log_entry);
//		this.job_manager.db.getTransaction().commit();
		
		
	}
	
	/**
	 * Generate the next CSV file.
	 * 
	 * Returns the name of the generated file
	 */
	
	private String generateNextCSVFile() {
		String newFileName = this.defaultCSVFilename;
		try {
			// Increment the counters
			this.totalFilesGenerated++;
			
			// Construct the filename for this output file
			
			newFileName = newFileName.replace("{dt}", this.startingDateTimeStr);
			newFileName = newFileName.replace("{seq}", Integer.toString(this.totalFilesGenerated));
			newFileName = newFileName.replace("{batch_num}",  this.job_manager.batch_log.getBatchNum().toString());
			
			
			// Replace ".csv" with ".tmp"
			String tmpFilename = new String();
			if (newFileName.toLowerCase().endsWith(".csv")) {
				int pos = newFileName.length()-5;
				tmpFilename = newFileName.substring(0,pos) + newFileName.substring(pos).replaceFirst(".csv", ".tmp");
				newFileName = tmpFilename;
			}
			// Create the file
			this.currentOutputFile = new FileWriter("output/" + tmpFilename,true);
			this.alternateOutputData.add("output/" + tmpFilename);
			
			// Reset counters
			this.totalRowsThisFile=0L;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newFileName;
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
			if (this.quoteAllOutputFields)
				retval += "\"";
			if (row.get(i) != null) {
				retval += row.get(i);
			}
			if (this.quoteAllOutputFields)
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
		
//		this.job_manager.db.getTransaction().begin();
		// Create entry in batch_log_dtl
		this.log_dtl = new BatchLogDtl();
		this.log_dtl.setBatchLog(this.job_manager.batch_log);
		
		String msg = "Step [" 
				+ this.jobStepXref.getStep().getType() + " : " + this.jobStepXref.getStep().getLongDesc() 
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
		this.log_dtl.setExtractMaxRecsPerFile(this.jobStepXref.getStep().getExtractMaxRecPerFile());
		this.log_dtl.setOutputFileFormat(this.jobStepXref.getStep().getOutputFileFormat());
		this.log_dtl.setOutputFilenamePrefix(this.jobStepXref.getStep().getOutputFilenamePrefix());
		this.log_dtl.setOutputFilenameSuffix(this.jobStepXref.getStep().getOutputFilenamePostfix());
		
		// Commit log entry
		this.job_manager.db.persist(this.log_dtl);
//		this.job_manager.db.getTransaction().commit();
		
		System.out.println("\t[TRG] Step completed.");
	}
	
private void logComplete() {
		
		if (!this.job_manager.db.getTransaction().isActive()) {
			this.job_manager.db.getTransaction().begin();
		}
		
		// Create entry in batch_log_dtl
		
		this.log_dtl.setStartDt(new Date());
		this.log_dtl.setStatus(BatchLog.statusComplete);
		
		// Commit log entry
		this.job_manager.db.persist(this.log_dtl);
		this.job_manager.db.getTransaction().commit();
		System.out.println("\t[CSV] Step completed.");
	}
}
