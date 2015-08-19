package com.inexas.fsm;

public class Toaster implements Implementation {
	private StateHandler stateHandler = new StateHandler();

	@Override
	public void setState(int id, int state) {
		stateHandler.setState(id, state);
	}

	@Override
	public int getState(int id) {
		return stateHandler.getState(id);
	}

	public String getTemperature() {
		return null;
	}

	public String getSwitchColorChangeRate() {
		return null;
	}

	public boolean notTooLight() {
		return false;
	}

	public int getAbsoluteColor() {
		return 0;
	}

	public void fireDone() {
		// nothing to do
	}

	public void stopTimer() {
		// nothing to do
	}

	public void resetTimer() {
		// nothing to do
	}

	public void eject() {
		// nothing to do
	}

	public void heaterOn() {
		// nothing to do
	}

	public void heaterOff() {
		// nothing to do
	}

	public void checkTemperature() {
		// nothing to do
	}

	public void readColorSensor() {
		// nothing to do
	}

	public void checkAbsoluteColor() {
		// nothing to do
	}

	public void checkColorChangeRate() {
		// nothing to do
	}

}
