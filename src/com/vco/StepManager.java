package com.vco;

import java.util.ArrayList;
import java.util.List;

import model.JobStepsXref;
import model.Step;

public class StepManager implements IStepManager {

	public JobManager job_manager;
	public JobStepsXref jobStepXref;
	
	// State Management
	protected boolean running = false;
	protected boolean completed=false;
	protected boolean failed = false;
	protected Long pages_processed = 0L;
	protected Long records_processed = 0L;
	
	// Data
	protected List<Object> dataPageOut = new ArrayList<Object>();
	protected List<Object> dataPageIn;
	protected List<Object> stepData;
	public List<Object> alternateOutputData;
	
	// Flags
	public Boolean outputsAlternateData = false;
	
	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean start() throws Exception{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processPageOfData(List<Object> pageOfData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean finish() {
		// TODO Auto-generated method stub
		return false;
	}

}
