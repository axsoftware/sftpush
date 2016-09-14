package com.axsoftware.sftpush.exception;

public class SFTPushException extends RuntimeException {

	private static final long serialVersionUID = 4744783555897554836L;

	public SFTPushException(final String message) {
		super(message);
	}
	
	public SFTPushException(final String message, final Throwable throwable) {
		super(message, throwable);
	}
	
}
