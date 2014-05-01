package com;


public class VBatchException extends Exception {

	private String msg = "";
	public boolean logged = false;
	
	public VBatchException(String message) {
		super(message);
		this.msg = message;
	}
}