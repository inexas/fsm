package com.inexas.fsm;

import javax.persistence.*;
import com.inexas.util.StringU;

// !todo I should not need persistence here

/**
 * This is a helper class that instances of Implementation can delegate to to
 * implement the get and set state methods.
 */
@Embeddable
public class StateHandler implements Implementation {
	@Access(AccessType.PROPERTY)
	private int[] state;

	public StateHandler() {
		state = new int[32];
	}

	public StateHandler(int numberOfStates) {
		state = new int[numberOfStates];
	}

	@Override
	public void setState(int id, int state) {
		this.state[id] = (byte)state;
	}

	@Override
	public int getState(int id) {
		return state[id];
	}

	@Override
	public String toString() {
		return getState();
	}

	public String getState() {
		return StringU.stringify(state);
	}

	public void setState(String string) {
		state = StringU.destringifyIntArray(string);
	}
}
