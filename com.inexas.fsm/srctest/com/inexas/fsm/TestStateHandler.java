package com.inexas.fsm;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TestStateHandler {
	@Test
	public void testStreaming() {
		final StateHandler sh = new StateHandler(5);
		for(int i = 0; i < 5; i++) {
			sh.setState(i, i + 10);
		}
		final StateHandler sh1 = new StateHandler(5);
		sh1.setState(sh.toString());
		assertEquals(sh.toString(), sh1.toString());
	}
}
