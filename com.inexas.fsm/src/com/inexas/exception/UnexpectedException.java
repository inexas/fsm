package com.inexas.exception;

/**
 * This exception is thrown when we reach a place in the code that we didn't
 * expect
 */
public class UnexpectedException extends InexasRuntimeException {
	private static final long serialVersionUID = 8637197045486771888L;

	/**
	 * @param message
	 */
	public UnexpectedException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param chainedException
	 */
	public UnexpectedException(String message, Exception chainedException) {
		super(message, chainedException);
	}

}
