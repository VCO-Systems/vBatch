/**
 * 
 */
package com.vco;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.AbstractMainTests;

/**
 * @author vco
 *
 */
public class VBatchManagerTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("Setup before VBatchManager test.");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		System.out.println("Teardown after VBatchManager test.");
	}

	/**
	 * Test method for {@link com.vco.VBatchManager#main(java.lang.String[])}.
	 */
	@Test
	public void testHelpOption() {
		String expected = "vbatch Error: Either -j or -b option must be specified.";
		System.out.println("tt");
		//VBatchManager.main(new String[]{"h"});
		String[] results = 
	            AbstractMainTests.executeMain("com.vco.VBatchManager", new String[]{"h"});
		System.out.println("tt");
		System.out.println("RESULTS: " + results);
		//fail("Not yet implemented"); // TODO
	}

}
