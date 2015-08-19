package com.inexas.fsm;

/**
 * 
 * @author KeithWhittingham
 * @version $Revision: 1.1 $
 */
public class NotActivatedException extends RuntimeException {
	private static final long serialVersionUID = -350880025312816340L;

	NotActivatedException(CompositeState parent) {
		super("Composite state has not yet been entered so has no history: " +
		        parent.getFullName());
	}

}
