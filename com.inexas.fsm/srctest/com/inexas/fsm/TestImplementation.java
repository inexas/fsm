package com.inexas.fsm;

public class TestImplementation implements Implementation {
	private StateHandler stateHandler = new StateHandler();
	private StringBuilder sb = new StringBuilder("");

	@Override
	public void setState(int id, int state) {
		stateHandler.setState(id, state);
	}

	@Override
	public int getState(int id) {
		return stateHandler.getState(id);
	}

	public void reaction() {
		sb.append('r');
	}

	public void transition() {
		sb.append('t');
	}

	public void entryAction() {
		sb.append('e');
	}

	public void exitAction() {
		sb.append('x');
	}

	@Override
	public String toString() {
		final String returnValue = sb.toString();
		sb = new StringBuilder();
		return returnValue;
	}

}
