package com.vco;

import java.util.ArrayList;
import java.util.List;

import model.Step;

public class StepManager implements IStepManager {

	public JobManager job_manager;
	public Step step_record;
	
	// State Management
	protected boolean running = false;
	protected boolean completed=false;
	protected boolean failed = false;
	protected int pages_processed = 0;
	protected int records_processed = 0;
	
	// Data
	protected List<Object> dataPageOut = new ArrayList<Object>();
	protected List<Object> dataPageIn;
	protected List<Object> stepData;
	protected List<Object> dataAlternate;
	
	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean start() {
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
