package com.vco;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.quote.QuoteMode;

import model.BatchLogDtl;
import model.Step;

public class GenerateCSVStep extends StepManager {

	ICsvListWriter listWriter = null;
	private int pageCount=0;
	private static final CsvPreference ALWAYS_QUOTE = 
		    new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE).useQuoteMode(new AlwaysQuoteMode()).build();
	FileWriter writer = null;
	
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
//			listWriter = new CsvListWriter(new FileWriter("output/vbatch_sample_out.csv"),
//					ALWAYS_QUOTE);
			
			// Configure csv writer to always quote every field
			writer = new FileWriter("output/vbatch.csv",true);
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
		this.pageCount++;
		System.out.println("\t[CSV] PageCount: " + this.pageCount);
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
			int rowcount=0;
			for (int i = 0; i < pageOfData.size(); i++) {
				rowcount++;
				List row = (List)pageOfData.get(i);
				//listWriter.write(row);
				String rowStr = this.generateCSVRow(row);
				this.writer.append(rowStr);
				this.writer.flush();
				if (this.rowOutputCounter >  8000) {
					System.out.println("[CSV] col0: " + pageOfData.get(0));
				}
				
			}
			System.out.println("\t[CSV] rows outputed: " + rowcount);
			System.out.println("\t[CSV] rowOutputCount: " + this.rowOutputCounter);
			
		}
		catch( Exception e) {
			e.printStackTrace();
			System.out.println("err");
		}
		finally {
			
		}
		
		return true;
	}
	
	private int rowOutputCounter = 0;
	
	private String generateCSVRow(List row) {
		rowOutputCounter++;
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
