package com.vco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

//file logging
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;

import java.text.MessageFormat;

import model.JobDefinition;
import model.Step;

public class VBatchManager {
	// version string used in logging
	public static final String vbatch_version = "vBatch v1.1";
    
	// JPA entity manager
	private static final String PERSISTENCE_UNIT_NAME = "vbatch";
	private static EntityManagerFactory factory;
	protected EntityManager em;
	
	// JDBC settings
	public static HashMap<String, String> source_db_connection = new HashMap<String,String>(1);

	// Is this a new job, a re-run of an existing batch?
	public String batchMode = BatchMode_New;
	public static String BatchMode_New = "BatchModeNew";  // Start a new job
	public static String BatchMode_Repeat = "BatchModeRepeat";  // Repeat an existing job (by batch_num)
	// Set effective date for one or more job(s)
	public static String BatchMode_SetEffectiveDate = "BatchModeSetEffectiveDate";  
	
	
	// file logging
	private String log4jPropertiesFilePath = "";
	private Properties vBatchProperties=null;
	public static Logger log = null;
	private String defaultLogFile = "";
	
	// settings passed in from command line
	private String requested_effective_date = new String();
	private List requested_job_ids_str = new ArrayList<String>();
	
	public VBatchManager() {
		// Set up db connection
		//factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		
		loadvBatchProperties();
		this.setLog4jPropertiesFilePath(this.getvBatchProperties().getProperty("vbatch.log4jproperties"));
		this.em = this.createEntityManager();
	}
	
	/**
	 * Set log4j logger - log
	 * use MessageFormat object to create the string message
	 * @param job_id
	 */
	private void setLogging(long order_num){
		// build the run logfile based on job id and timestamp
		Date today = new Date();
		this.defaultLogFile = MessageFormat.format("vbatch_j{0}_d{1}_t{2}.log", String.valueOf(order_num), new SimpleDateFormat("yyyyMMdd").format(today), new SimpleDateFormat("HHmmss").format(today));
		
		// set system properties used in log4j logfile Filename
		System.setProperty("logfile",defaultLogFile);

		// log4j setup - assign log4j properties file from vbatch.ini
		PropertyConfigurator.configure(this.getLog4jPropertiesFilePath());
		
		// start the logging
		log = Logger.getLogger(vbatch_version);	
//		log.debug(MessageFormat.format("ORDER_NUM : {0}", order_num));
	}
	
	/**
	 * Takes a list of job_ids, and manages the execution
	 * of each job.
	 * 
	 * @param job_ids
	 * @throws Exception 
	 */
	public void init(ArrayList<String> job_ids) throws Exception {
		// Make sure we have all necessary configuration information for each job,
		// 
		if (this.batchMode.equals(VBatchManager.BatchMode_SetEffectiveDate)) {  // -set_job_date run
			VBatchUtilities vutil = new VBatchUtilities();
			vutil.job_ids=job_ids;
			vutil.requested_job_date = this.requested_effective_date;
			vutil.setJobDates();
		}
		else {  // -j or -b run
			for (String job_id_str : job_ids) {
				Integer job_id = Integer.parseInt(job_id_str);
				setLogging(job_id);
				JobManager job_manager = new JobManager(this, job_id);
				// Tell the job manager whether new or existing job is being run
				job_manager.batchMode = this.batchMode;  
				job_manager.init();
			}
		}
	}

	public String getLog4jPropertiesFilePath() {
		return log4jPropertiesFilePath;
	}

	public void setLog4jPropertiesFilePath(String log4jPropertiesFilePath) {
		this.log4jPropertiesFilePath = log4jPropertiesFilePath;
	}
	
	public Properties getvBatchProperties() {
		return vBatchProperties;
	}

	public void setvBatchProperties(Properties vBatchProperties) {
		this.vBatchProperties = vBatchProperties;
	}
	
	/**
	 * load java properties from vbatch.ini and set the local class variable
	 */
	private void loadvBatchProperties(){
		
		// Load JPA connection settings from ini file
		String db_connect_string_file_path = "config/vbatch.ini";
		InputStream inp = null;
		try {
			inp = new FileInputStream(db_connect_string_file_path);
		}
		catch (FileNotFoundException e) {
//			log.error(MessageFormat.format("JPA DB Settings file not found: {0}", db_connect_string_file_path));
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
		
		this.setvBatchProperties(prop);
	}
	
	/**
	 * Create a JPA entity manager, overriding the db_connection, user, and password
	 * with settings from the config/vbatch.ini file
	 */
	private EntityManager createEntityManager() {
		
		// Settings to read from ini file
		String db_connect_string, user, password;
		
		// Load vBatch DB properties
		db_connect_string = this.getvBatchProperties().getProperty("vbatch.db");
		if (db_connect_string == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.db");
			System.exit(1);
		}
		user = this.getvBatchProperties().getProperty("vbatch.user");
		if (user == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.user");
			System.exit(1);
		}
		password = this.getvBatchProperties().getProperty("vbatch.password");
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
		
		
		factory = Persistence.createEntityManagerFactory("vbatch", properties);
		EntityManager em = factory.createEntityManager();
		
		return em;
	}
	
	
	public void getRequestedJobs() {
		
	}
	
	public static void main(String[] args) {
		String requested_job_date;
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
		options.addOption("set_job_date",true,"Set effective date for one or more jobs");
		

		CommandLineParser parser = new BasicParser();
		
		try {
			
			// main vBatch variable
			VBatchManager man = null;
			
			CommandLine cmd = parser.parse(options, args);
			HelpFormatter help = new HelpFormatter();
			if (cmd.hasOption("h")) {
			    help.printHelp(vbatch_version, options );
			}
			
			/**
			 * Before processing particular commands, check to make sure the
			 * vbatch command's basic requirements have been met.
			 * 
			 */
			
			else if ( !cmd.hasOption("j")  && !cmd.hasOption("b") ) {
				System.out.println("vbatch Error: Either -j or -b option must be specified.");
				help.printHelp(vbatch_version, options );
			}
			else if ( cmd.hasOption("j") && !cmd.hasOption("db")) {
				System.out.println("vbatch Error: When '-j' option is used, '-db' option is required.");
				help.printHelp(vbatch_version, options );
			}
			else if ( cmd.hasOption("b") && !cmd.hasOption("db")) {
				System.out.println("vbatch Error: When '-b' option is used, '-db' option is required.");
				help.printHelp(vbatch_version, options );
			}
			
			else {
				/**
				 * Now process the valid vbatch commands
				 * 
				 */
				
				// We check for -db first, because if a valid db settings file
				// cannot be found, we'll abort the job, since neither -j nor -b 
				// can work.
				if (cmd.hasOption("db")) {
					// Load properties file
					String db_connect_string_file_path = cmd.getOptionValue("db");
					InputStream inp = null;
					try {
						inp = new FileInputStream(db_connect_string_file_path);
					}
					catch (FileNotFoundException e) {
//						System.out.println("ERROR: DB settings file not found: " + db_connect_string_file_path);
						throw new Exception("vbatch ERROR: DB settings file not found: " + db_connect_string_file_path);
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
					System.out.println(VBatchManager.vbatch_version);
					String[] requested_jobs_ids = cmd.getOptionValue("j").split(",");
					ArrayList<String> job_ids = new ArrayList<String>();
					for (String job_id_str : requested_jobs_ids) {
						// Cast the job_id as int
						job_ids.add(job_id_str);
					}
					
					// -set_job_date is a special case, which uses -j to pass
					// in job numbers, but bypasses the normal JobManager logic
					if (cmd.hasOption("set_job_date")) {
//						requested_job_date = cmd.getOptionValue("set_job_date");
						// We have a valid job number(s) and job_date, so run the
						// -set_job_date logic
						if (man==null) {
							man = new VBatchManager();
							man.batchMode = VBatchManager.BatchMode_SetEffectiveDate;
							man.requested_effective_date = cmd.getOptionValue("set_job_date");
							man.init(job_ids);
						}
						
						
					}
					else {  // regular -j run
					
						// For each requested job, create and start a batch manager
						
						
						// If no jobs are specified, exit with a polite error msg
						if (job_ids.size() == 0) {
							throw new Exception("ERROR: must specify at least one job id (example: vbatch -j 12)");
						}
						
						// TODO: Verify that each requested job_id exists
						// TODO: If any do not exist, report it
						
						// Start a new VBatchManger
						if (man == null) 
							man = new VBatchManager();
					    man.batchMode = VBatchManager.BatchMode_New;
						man.init(job_ids);
					}
				}
				
				// Handle -b (re-run existing batch)
				if (cmd.hasOption("b")) {
					System.out.println(VBatchManager.vbatch_version);
					// TODO:  Get ready to run the -b job
					String[] requested_jobs_ids = cmd.getOptionValue("b").split(",");
					ArrayList<String> job_ids = new ArrayList<String>();
					
					// For each requested job, create and start a batch manager
					for (String job_id_str : requested_jobs_ids) {
						// Cast the job_id as int
						job_ids.add(job_id_str);
					}
					
					// If no jobs are specified, exit with a polite error msg
					if (job_ids.size() == 0) {
						throw new Exception("vbatch Eror: must specify at least one batch_num (example: vbatch -b 12 {,13,14})");
					}
					// Kick off the batch
					if (man == null) 
						man = new VBatchManager();
				    man.batchMode = VBatchManager.BatchMode_Repeat;
					man.init(job_ids);
				}
			}
		} // end: outer try/catch
		catch (ParseException e) {
		    System.err.println(e);
		} 
		catch (IOException ex) {
			System.out.println("*******Canonical path****** "+ ex.getClass().getCanonicalName());
			ex.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
