package com.vco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import model.JobDefinition;

public class VBatchManager {

	private static final String PERSISTENCE_UNIT_NAME = "vbatch";
	private static EntityManagerFactory factory;
	protected EntityManager em;
	
	public static HashMap<String, String> source_db_connection = new HashMap<String,String>(1);

	// Is this a new job, a re-run of an existing batch?
	public static String BatchMode_New = "BatchModeNew";  // Start a new job
	public static String BatchMode_Repeat = "BatchModeRepeat";  // Repeat an existing job (by batch_num)
	public String batchMode = BatchMode_New;
	
	public VBatchManager() {
		// Set up db connection
		//factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		
		this.em = this.createEntityManager();
	}
	
	/**
	 * Takes a list of job_ids, and manages the execution
	 * of each job.
	 * 
	 * @param job_ids
	 */
	public void init(ArrayList<Integer> job_ids) {
		// Make sure we have all necessary configuration information for each job,
		// 
		for (Integer job_id : job_ids) {
			JobManager job_manager = new JobManager(this, job_id);
			// Tell the job manager whether new or existing job is being run
			job_manager.batchMode = this.batchMode;  
			job_manager.init();
		}
	}

	/**
	 * Create a JPA entity manager, overriding the db_connection, user, and password
	 * with settings from the config/vbatch.ini file
	 */
	private EntityManager createEntityManager() {
		// Settings to read from ini file
		String db_connect_string, user, password;
		
		// Load JPA connection settings from ini file
		String db_connect_string_file_path = "config/vbatch.ini";
		InputStream inp = null;
		try {
			inp = new FileInputStream(db_connect_string_file_path);
		}
		catch (FileNotFoundException e) {
			System.out.println("ERROR: JPA DB settings file not found: " + db_connect_string_file_path);
			System.exit(1);
		}
		
		Properties prop = new Properties();
		try {
			
			prop.load(inp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		// Load individual properties
		db_connect_string = prop.getProperty("vbatch.db");
		if (db_connect_string == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.db");
			System.exit(1);
		}
		user = prop.getProperty("vbatch.user");
		if (user == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.user");
			System.exit(1);
		}
		password = prop.getProperty("vbatch.password");
		if (password == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.password");
			System.exit(1);
		}
		Properties properties = new Properties();
		//properties.put("eclipselink.jdbc.batch-writing", "JDBC");
		properties.put("javax.persistence.jdbc.url", db_connect_string);
		properties.put("javax.persistence.jdbc.user", user);
		properties.put("javax.persistence.jdbc.password", password);
		//properties.put("javax.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
		
		
		this.factory = Persistence.createEntityManagerFactory("vbatch", properties);
		EntityManager em = this.factory.createEntityManager();
		
		return em;
	}
	
	public void init_old() {
		// Set up db connection
		factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		this.em = factory.createEntityManager();
		this.em.getTransaction().begin();
		// Create new job_master record
		JobDefinition job = new JobDefinition();
		job.setShortDesc("Dummy Job");
		job.setLongDesc("Simple test job.");
		this.em.persist(job);
		this.em.getTransaction().commit();
		this.em.close();
		factory.close();
	}
	
	
	
	public void getRequestedJobs() {
		
	}
	
	public static void main(String[] args) {
		
		// If required directories don't exist, create them
		File outputDir = new File("output");
		outputDir.mkdir();
		File configDir = new File("config");
		configDir.mkdir();
		
		Options options = new Options();
		options.addOption("h", false, "Display list of vBatch commands.");
		options.addOption("db", true, "Source db connection string filepath");
		//options.addOption("test_create", false, "Create a sample WMOS source database.");
		options.addOption("j", true, "Specify one or more jobs to start.");
		options.addOption("b", true, "Re-run an earlier batch.");
		

		CommandLineParser parser = new BasicParser();
		
		try {
			
			// main vBatch variable
			VBatchManager man = null;
			
			// Read config options from vbatch.config file
//			config_file_input = new FileInputStream("vbatch.properties");
//	    	
//	    	config_properties.load(config_file_input);
//	    	testing_db_name = config_properties.getProperty("testing_db_name");
//	    	System.out.println(testing_db_name);
	    	
			CommandLine cmd = parser.parse(options, args);
			HelpFormatter help = new HelpFormatter();
			if (cmd.hasOption("h")) {
			    
			    help.printHelp("vBatch v0.1", options );
			}
			
			/**
			 * Before processing particular commands, check to make sure the
			 * vbatch command's basic requirements have been met.
			 * 
			 */
			
			if ( !cmd.hasOption("j")  && !cmd.hasOption("b") ) {
				System.out.println("vbatch Error: Either -j or -b option must be specified.");
				help.printHelp("vBatch v0.1", options );
				System.exit(1);
			}
			if ( cmd.hasOption("j") && !cmd.hasOption("db")) {
				System.out.println("vbatch Error: When '-j' option is used, '-db' option is required.");
				help.printHelp("vBatch v0.1", options );
				System.exit(1);
			}
			if ( cmd.hasOption("b") && !cmd.hasOption("db")) {
				System.out.println("vbatch Error: When '-b' option is used, '-db' option is required.");
				help.printHelp("vBatch v0.1", options );
				System.exit(1);
			}
			
			/**
			 * Now process the valid vbatch commands
			 * 
			 */
			
			if (cmd.hasOption("db")) {
				// Load properties file
				String db_connect_string_file_path = cmd.getOptionValue("db");
				InputStream inp = null;
				try {
					inp = new FileInputStream(db_connect_string_file_path);
				}
				catch (FileNotFoundException e) {
					System.out.println("ERROR: DB settings file not found: " + db_connect_string_file_path);
					System.exit(1);
				}
				
				Properties prop = new Properties();
				prop.load(inp);
		    	
				// Load individual properties
				source_db_connection.put("db", prop.getProperty("db"));
				source_db_connection.put("user", prop.getProperty("user"));
				source_db_connection.put("password", prop.getProperty("password"));
				
				
		    	if (man == null) {
		    		
		    	}
			
			}
			else {
				System.out.println("ERROR:  Did not find all required settings in db connection file");
				return;
			}
			// -j xx[,xx,xx]
			// runs one more more jobs with specified job id
			if (cmd.hasOption("j")) {
				// System.out.println("in the -t block");
				String[] requested_jobs_ids = cmd.getOptionValue("j").split(",");
				ArrayList<Integer> job_ids = new ArrayList<Integer>();
				
				// For each requested job, create and start a batch manager
				for (String job_id_str : requested_jobs_ids) {
					// Cast the job_id as int
					job_ids.add(Integer.parseInt(job_id_str));
				}
				
				// If no jobs are specified, exit with a polite error msg
				if (job_ids.size() == 0) {
					System.out.println("ERROR: must specify at least one job id (example: vbatch -j 12)");
					System.exit(1);
				}
				
				// TODO: Verify that each requested job_id exists
				// TODO: If any do not exist, report it
				
				// Start a new VBatchManger
				if (man == null) 
					man = new VBatchManager();
			    man.batchMode = VBatchManager.BatchMode_New;
				man.init(job_ids);
				System.exit(0);
			}
			
			// Handle -b (re-run existing batch)
			if (cmd.hasOption("b")) {
				// TODO:  Get ready to run the -b job
				String[] requested_jobs_ids = cmd.getOptionValue("b").split(",");
				ArrayList<Integer> job_ids = new ArrayList<Integer>();
				
				// For each requested job, create and start a batch manager
				for (String job_id_str : requested_jobs_ids) {
					// Cast the job_id as int
					job_ids.add(Integer.parseInt(job_id_str));
				}
				
				// If no jobs are specified, exit with a polite error msg
				if (job_ids.size() == 0) {
					System.out.println("vbatch Eror: must specify at least one batch_num (example: vbatch -b 12 {,13,14})");
					System.exit(1);
				}
				// Kick off the batch
				if (man == null) 
					man = new VBatchManager();
			    man.batchMode = VBatchManager.BatchMode_Repeat;
				man.init(job_ids);
				System.exit(0);
			}
			
		}
		catch (ParseException e) {

		    System.err.println(e);
		    HelpFormatter formatter = new HelpFormatter();
		    formatter.printHelp("PROJECT_NAME", options );
		} 
		catch (IOException ex) {
			System.out.println("*******Canonical path****** "+ ex.getClass().getCanonicalName());
			ex.printStackTrace();
		}
	}
}
