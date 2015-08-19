package com.inexas.util;

import com.inexas.exception.InexasRuntimeException;

/**
 * @author Keith Whittingham
 * @version $Revision: 1.1 $
 */
public class NotFoundException extends InexasRuntimeException {
	private static final long serialVersionUID = 8398072541217939438L;

	public NotFoundException(String name) {
		super("Object not found: " + name);
	}

	public NotFoundException(String name, String reason) {
		super("Object not found: " + name + " (" + reason + ")");
	}

	public NotFoundException(String name, Exception reason) {
		super("Object not found: " + name, reason);
	}

}
