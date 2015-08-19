package com.inexas.fsm;

/**
 * An implementation of an FSM. This class allows the implementation to be
 * separated from the definition of the FSM. It allows one FSM definition to be
 * loaded statically and used with many implementations.
 */
public interface Implementation {
	/**
	 * The implementation is expected to maintain the state and persist it as
	 * required. This method is called whenever the state of a stateful object
	 * changes. You probably want to use a StateHandler to do the work for you.
	 * 
	 * @param id
	 * @param state
	 */
	void setState(int id, int state);

	/**
	 * @see #setState()
	 */
	int getState(int id);

}
