package com.inexas.util;

import com.inexas.exception.InexasRuntimeException;

/**
 * 
 * @author Keith Whittingham
 * @version $Revision: 1.1 $
 */
public class DuplicateException extends InexasRuntimeException {
	private static final long serialVersionUID = -2450497327970803591L;

	public DuplicateException(String message) {
		super(message);
	}
}
