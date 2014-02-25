package com.vco;

public class GenerateCSVStep extends StepManager {

	public JobManager job_manager;
	
	public GenerateCSVStep(JobManager jm) {
		System.out.println("GenerateCSVStep() constructor");
		this.job_manager = jm;
	}
}
