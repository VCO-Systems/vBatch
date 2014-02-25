package com.vco;

public class ExtractDBStep extends StepManager {

	public JobManager job_manager;
	
	public ExtractDBStep(JobManager jm) {
		System.out.println("ExtractDBStep() constructor");
		this.job_manager = jm;
	}
}
