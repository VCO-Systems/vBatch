package com;


public class VBatchException extends Exception {

	private String msg = "";
	
	public VBatchException(String message) {
		super(message);
		this.msg = message;
	}
}