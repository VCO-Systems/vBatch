package com.vco;

import java.util.List;

public interface IStepManager {

	public boolean init();
	public boolean start() throws Exception;
	public boolean processPageOfData(List<Object> pageOfData) throws Exception;
	public boolean finish();
	
}