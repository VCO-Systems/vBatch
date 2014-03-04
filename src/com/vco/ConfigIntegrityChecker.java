package com.vco;

public class ConfigIntegrityChecker {

	/**
	 * Things to check the steps table for:
	 * 
	 * 	  - required fields for a DB Extraction step: 
	 *    - raw sql missing \* where *\ clause
	 *    - no max_rec or max_rec_per_file set for extraction or CSVGeneration step
	 */
}
