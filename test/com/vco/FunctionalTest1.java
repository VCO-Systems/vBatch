/**
 * 
 */
package com.vco;

import static org.junit.Assert.*;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author vco
 *
 */
public class FunctionalTest1 {
	
	private static final String PERSISTENCE_UNIT_NAME = "vbatch";
	private static EntityManagerFactory factory;
	protected static EntityManager em;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// connect to the db
		em = createEntityManager();
		
		
		// drop the test db
		
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		fail("Not yet implemented"); // TODO
	}
	
	private static EntityManager createEntityManager() {
		
		// Settings to read from ini file
		String db_connect_string, user, password;
		
		// Load vBatch DB properties
		//db_connect_string = this.getvBatchProperties().getProperty("vbatch.db");
		db_connect_string = "jdbc:oracle:thin:@192.168.56.1:1521/xe";
		if (db_connect_string == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.db");
			System.exit(1);
		}
		// user = this.getvBatchProperties().getProperty("vbatch.user");
		user = "vbatch";
		if (user == "") {
			System.out.println("ERROR: could not read parameter from config/vbatch.ini : vbatch.user");
			System.exit(1);
		}
		//password = this.getvBatchProperties().getProperty("vbatch.password");
		password = "vbatch";
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

}
