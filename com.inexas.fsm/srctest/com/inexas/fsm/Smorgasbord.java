package com.inexas.fsm;

public class Smorgasbord implements Implementation {
	private StateHandler stateHandler = new StateHandler(64);

	@Override
	public void setState(int id, int state) {
		stateHandler.setState(id, state);
	}

	@Override
	public int getState(int id) {
		return stateHandler.getState(id);
	}

	public String query() {
		return null;
	}

	public boolean guard() {
		return false;
	}

	public void transitOne() {
		// nothing to do
	}

	public void transitTwo() {
		// nothing to do
	}

	public void enter() {
		// nothing to do
	}

	public void enter(
			@SuppressWarnings("unused") String s1,
			@SuppressWarnings("unused") String s3,
			@SuppressWarnings("unused") Integer i,
			@SuppressWarnings("unused") String s4,
			@SuppressWarnings("unused") Boolean b) {
		// nothing to do
	}

	public void exit() {
		// nothing to do
	}

	public String getA2() {
		return "a2";
	}

}
