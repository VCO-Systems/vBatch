/**
 * 
 */
package com.vco;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
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
	 * Test method for {@link com.vco.VBatchManager#main(java.lang.String[])}.
	 */
	@Test
	public void helpOptionDisplaysCommandUsage() {
		String expectedString = "usage: vBatch";
		Boolean foundExpectedString = false;
		String[] vbatch_args = new String[]{"-h"};
		
		// run vbatch
		String[] vbatchConsoleOutput = 
	            AbstractMainTests.executeMain("com.vco.VBatchManager", vbatch_args);
		
		// Make sure the results of "vbatch -h" is to show the vbatch command usage
		for (int i=0; i < vbatchConsoleOutput.length; i++) {
			String vbatchConsoleLine = vbatchConsoleOutput[i];
			if (vbatchConsoleLine.contains("usage: vBatch")) {
				foundExpectedString=true;
				break;
			}
		}
		if (!foundExpectedString) {
			fail("vbatch -h did not display the command usage");
		}
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		System.out.println("Teardown after VBatchManager test.");
	}

}
