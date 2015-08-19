package com.inexas.fsm;

import com.inexas.util.StrictMap;


class FixVerticesVisitor implements Visitor {

	@Override
	public void visit(Vertex vertex) throws ParseException {
		vertex.fix();
		// the following check asserts that sub-classes of vertex are
		// calling super.fix();
		assert ((StrictMap<String, Transition>)vertex.getTransitions()).isLocked();
	}

	@Override
	public void visit(Transition transition) {
		// nothing to do
	}
}
