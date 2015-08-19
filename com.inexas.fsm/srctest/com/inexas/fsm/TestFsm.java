package com.inexas.fsm;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import com.inexas.exception.UnexpectedException;

public class TestFsm {
	private Fsm fsm;

	private void checkActions(String expected) {
		final Actions actions = (Actions)fsm.getImplementation();
		final String got = actions.toString();
		if(!got.equals(expected)) {
			System.out.println("Exp: " + expected);
			System.out.println("Got: " + got);
			assertEquals(expected, got);
		}
	}

	private void checkState(String expected) {
		final List<State> states = fsm.getCompoundState();
		Collections.sort(states);
		final String got = states.toString();

		if(!got.equals(expected)) {
			System.out.println("Exp: " + expected);
			System.out.println("Got: " + got);
			assertEquals(expected, got);
		}
	}

	private void fire(Fsm theFsm, String eventName) {
		theFsm.handle(theFsm.getEvent(eventName));
	}

	public void XtestTimer() throws ParseException {
		fsm = (new FsmFactory("datatest/fsm/min.xml")).newInstance(new TestImplementation());
		fsm.start();
		final TimerTask timerTask = fsm.newTimerEvent(
				fsm.getEvent(Fsm.START_NAME),
				(new Date()).getTime() + 100,
				100);
		try {
			Thread.sleep(5 * 100);
		} catch(InterruptedException e) {
			throw new UnexpectedException("Timer interrupt");
		}
		timerTask.cancel();
		try {
			Thread.sleep(3 * 100);
		} catch(InterruptedException e) {
			throw new UnexpectedException("Timer interrupt");
		}
	}

	@Test
	public void testMin() throws Exception {
		final FsmFactory factory = new FsmFactory("datatest/fsm/min.xml");
		final Implementation implementation = new TestImplementation();
		fsm = factory.newInstance(implementation);
		fsm.start();
		checkState("[/s1]");
	}

	@Test
	public void testSmorgasbord() throws Exception {
		fsm = (new FsmFactory("datatest/fsm/smorgasbord.xml")).newInstance(new Smorgasbord());
		fsm.start();
	}

	@Test
	public void testTurnstyle() throws Exception {
		fsm = (new FsmFactory("datatest/fsm/turnstyle.xml")).newInstance(new Turnstyle());
		fsm.start();
		checkState("[/NormalMode, /NormalMode/Locked]");
	}

	@Test
	public void testToaster() throws Exception {
		fsm = (new FsmFactory("datatest/fsm/toaster.xml")).newInstance(new Toaster());
		fsm.start();
		checkState("[/Idle]");
	}

	@Test
	public void testActions() throws ParseException {
		fsm = (new FsmFactory("datatest/fsm/actions.xml")).newInstance(new Actions());
		fsm.start();
		checkState("[/S1]");
		checkActions("toS1,entryAction1,entryAction2,");

		// Need a parameter...
		final Event event = fsm.getEvent("ToSelf");
		final Event parameterizedEvent = new Event(event);
		parameterizedEvent.setParameter("p", "I'd like a P please Bob!");
		fsm.handle(parameterizedEvent);

		// the toFindGuard is correct below as the guard m
		checkActions(""
				+ "exitAction,"
				+ "toSelf1,"
				+ "a2,"
				+ "toSelf2(a1,I'd like a P please Bob!,a2,3,a4,false),"
				+ "entryAction1,"
				+ "entryAction2,");
	}

	@Test
	public void testCompstate() throws Exception {
		fsm = (new FsmFactory("datatest/fsm/compstate.xml")).newInstance(new TestImplementation());
		fsm.start();
		checkState("[/s0]");

		fire(fsm, "to-cs");
		checkState("[/cs, /cs/cs.s1]");

		fire(fsm, "to-cs.s2");
		checkState("[/cs, /cs/cs.s2]");

		fire(fsm, "to-cs.cs");
		checkState("[/cs, /cs/cs.cs, /cs/cs.cs/cs.cs.s1]");

		fire(fsm, "to-cs.cs.s2");
		checkState("[/cs, /cs/cs.cs, /cs/cs.cs/cs.cs.s2]");

		fire(fsm, "to-cs.s1");
		checkState("[/cs, /cs/cs.s1]");

		fire(fsm, "to-s0");
		checkState("[/s0]");

		fire(fsm, "to-cs.s2");
		checkState("[/cs, /cs/cs.s2]");

		fire(fsm, "to-s0");
		checkState("[/s0]");

		fire(fsm, "to-cs.cs.s2");
		checkState("[/cs, /cs/cs.cs, /cs/cs.cs/cs.cs.s2]");

		fire(fsm, "to-s0");
		fire(fsm, "to-cs.s2");
		checkState("[/cs, /cs/cs.s2]");
		fire(fsm, "to-s0");
		checkState("[/s0]");
	}

	@Test
	public void testSynch() throws Exception {
		fsm = (new FsmFactory("datatest/fsm/synch.xml")).newInstance(new TestImplementation());
		fsm.start();
		checkState("[/s0]");
		fire(fsm, "to-s1");
		checkState("[/s2, /s3]");
		fire(fsm, "to-s4");
		checkState("[/s5]");
	}

	@Test
	public void testHistory() throws Exception {
		fsm = (new FsmFactory("datatest/fsm/history.xml")).newInstance(new TestImplementation());
		fsm.start();

		checkState("[/shallow, /shallow/shallow.s0]");

		fire(fsm, "to-s1");
		checkState("[/s1]");

		// Reinitialize shallow
		fire(fsm, "to-shallow");
		checkState("[/shallow, /shallow/shallow.s0]");

		// this should be remembered...
		fire(fsm, "to-shallow.s1");
		checkState("[/shallow, /shallow/shallow.s1]");
		fire(fsm, "to-s1");
		checkState("[/s1]");
		fire(fsm, "to-shallow/H");
		checkState("[/shallow, /shallow/shallow.s1]");

		// make sure the deep state isn't maintained in a shallow state
		// resume...
		fire(fsm, "to-shallow.cs");
		checkState("[/shallow, /shallow/shallow.cs, /shallow/shallow.cs/shallow.cs.s0]");
		fire(fsm, "to-shallow.cs.s1");
		checkState("[/shallow, /shallow/shallow.cs, /shallow/shallow.cs/shallow.cs.s1]");
		fire(fsm, "to-s1");
		checkState("[/s1]");
		fire(fsm, "to-shallow/H");
		checkState("[/shallow, /shallow/shallow.cs, /shallow/shallow.cs/shallow.cs.s0]");
		// the above is a difficult case, see the discussion about history on
		// the wiki

		// now check the deep history...
		fire(fsm, "to-deep");
		checkState("[/deep, /deep/deep.s0]");
		fire(fsm, "to-deep.cs");
		test(fsm.getCompoundState().toString(), "[/deep, /deep/deep.cs, /deep/deep.cs/deep.cs.s1]");
		fire(fsm, "to-deep.cs.cs.s0");
		test(fsm.getCompoundState().toString(),
				"[/deep, /deep/deep.cs, /deep/deep.cs/deep.cs.cs, " +
						"/deep/deep.cs/deep.cs.cs/deep.cs.cs.s0]");
		fire(fsm, "to-deep.cs.cs.s1");
		test(fsm.getCompoundState().toString(),
				"[/deep, /deep/deep.cs, /deep/deep.cs/deep.cs.cs, " +
						"/deep/deep.cs/deep.cs.cs/deep.cs.cs.s1]");
		fire(fsm, "to-s2");
		checkState("[/s2]");
		fire(fsm, "to-deep/H*");
		test(fsm.getCompoundState().toString(),
				"[/deep, /deep/deep.cs, /deep/deep.cs/deep.cs.cs, " +
						"/deep/deep.cs/deep.cs.cs/deep.cs.cs.s1]");
	}

	@Test
	public void testReaction() throws Exception {
		final TestImplementation test = new TestImplementation();
		fsm = (new FsmFactory("datatest/fsm/reaction.xml")).newInstance(test);
		fsm.start();
		test.toString(); // clear the string buffer
		checkState("[/s0]");
		fire(fsm, "transition");
		assertTrue(test.toString().equals("xte"));
		fire(fsm, "reaction");
		assertTrue(test.toString().equals("r"));
	}

	public void test(String s1, String s2) {
		if(!s1.equals(s2)) {
			System.out.println("Got: " + s1 + " should be " + s2);
			assertTrue(false);
		}
	}

}
