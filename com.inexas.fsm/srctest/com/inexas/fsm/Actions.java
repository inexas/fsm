package com.inexas.fsm;

public class Actions implements Implementation {
	private StringBuilder sb = new StringBuilder();
	private boolean finalGuard;
	private final StateHandler stateHandler = new StateHandler();

	public boolean toFinalGuard() {
		sb.append("toFinal");
		return finalGuard;
	}

	public void toS1() {
		sb.append("toS1,");
	}

	@SuppressWarnings("unused")
	public void toS1(Transition transition, int i, boolean b) {
		sb.append("toS1b,");
	}

	/**
	 * Read buffered call record and clear the buffer
	 */
	@Override
	public String toString() {
		final String returnValue = sb.toString();
		sb = new StringBuilder();
		return returnValue;
	}

	public boolean isFinalGuard() {
		return finalGuard;
	}

	public void setFinalGuard(boolean finalGuard) {
		this.finalGuard = finalGuard;
	}

	public void entryAction() {
		sb.append("entryAction,");
	}

	public void exitAction() {
		sb.append("exitAction,");
	}

	public void entryAction1() {
		sb.append("entryAction1,");
	}

	public void entryAction2() {
		sb.append("entryAction2,");
	}

	public String getA2() {
		sb.append("a2,");
		return "a2";
	}

	public void toSelf1() {
		sb.append("toSelf1,");
	}

	public void toSelf2(
			String s1,
			String s2,
			String s3,
			Integer i,
			String s4,
			Boolean b) {
		sb.append("toSelf2(");
		sb.append(s1);
		sb.append(',');
		sb.append(s2 == null ? "<null>" : s2.toString());
		sb.append(',');
		sb.append(s3);
		sb.append(',');
		sb.append(i);
		sb.append(',');
		sb.append(s4);
		sb.append(',');
		sb.append(b);
		sb.append("),");
		// nothing to do
	}

	@Override
	public void setState(int id, int state) {
		stateHandler.setState(id, state);
	}

	@Override
	public int getState(int id) {
		return stateHandler.getState(id);
	}

}
