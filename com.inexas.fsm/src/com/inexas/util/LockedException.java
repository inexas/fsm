package com.inexas.util;

/**
 * 
 * @author KeithWhittingham
 * @version $Revision: 1.1 $
 */
public class LockedException extends RuntimeException {
	private static final long serialVersionUID = -7300922753867700973L;

	public LockedException(String message) {
		super(message);
	}

}
