

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

import com.vco.VBatchManager;


public class Main {
	
	// Properties set by config file, or at command-line
	public static String testing_db_name;
	
	private static InputStream config_file_input = null;
	private static Properties config_properties = new Properties();

    public static void main(String[] args) throws Exception {
    	
    	
    	
		Options options = new Options();
		options.addOption("h", false, "Display list of vBatch commands.");
		options.addOption("test_create", false, "Create a sample WMOS source database.");
		options.addOption("j", true, "Specify one or more jobs to start.");

		CommandLineParser parser = new BasicParser();
		try {
			
			// Read config options from vbatch.config file
			System.out.println(new File(".").getAbsolutePath());
//			config_file_input = new FileInputStream("vbatch.properties");
//	    	
//	    	config_properties.load(config_file_input);
//	    	testing_db_name = config_properties.getProperty("testing_db_name");
//	    	System.out.println(testing_db_name);
	    	
			CommandLine cmd = parser.parse(options, args);
			
			if (cmd.hasOption("h")) {
			    HelpFormatter help = new HelpFormatter();
			    help.printHelp("vBatch v0.1", options );
			}
			
			// -test_create option
			if (cmd.hasOption("test_create")) {
			    VBatchManager batch_manager = new VBatchManager();
			    //batch_manager.init();
			    
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
				// TODO: Verify that each requested job_id exists
				// TODO: If any do not exist, report it
				
				// Start a new VBatchManger
				VBatchManager man = new VBatchManager();
//				man.init(job_ids);
			}
		}
		catch (ParseException e) {

		    System.err.println(e);
		    System.out.println();
		    HelpFormatter formatter = new HelpFormatter();
		    formatter.printHelp("PROJECT_NAME", options );
		} 
//		catch (IOException ex) {
//			System.out.println("*******Canonical path****** "+ ex.getClass().getCanonicalName());
//			ex.printStackTrace();
//		} 
    }
}