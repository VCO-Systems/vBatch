package com.vco;

import java.util.List;

public interface IStepManager {

	public boolean init();
	public boolean start();
	public boolean processPageOfData(List<Object> pageOfData);
	public boolean finish();
	
}