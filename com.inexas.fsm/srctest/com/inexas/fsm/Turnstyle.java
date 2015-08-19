package com.inexas.fsm;

public class Turnstyle implements Implementation {
	private StateHandler stateHandler = new StateHandler();

	@Override
	public void setState(int id, int state) {
		stateHandler.setState(id, state);
	}

	@Override
	public int getState(int id) {
		return stateHandler.getState(id);
	}

	public void saveDeviceStates() {
		// nothing to do
	}

	public void thankyouOff() {
		// nothing to do
	}

	public void alarm() {
		// nothing to do
	}

	public void resetAlarm() {
		// nothing to do
	}

	public void thankyou() {
		// nothing to do
	}

	public void lock() {
		// nothing to do
	}

	public void unlock() {
		// nothing to do
	}

	public void transitionAction() {
		// nothing to do
	}

	public void restoreDeviceStatus() {
		// nothing to do
	}

}
