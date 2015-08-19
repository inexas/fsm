package com.inexas.fsm;

class FixTransitionsVisitor implements Visitor {

	@Override
	public void visit(Vertex vertex) {
		// nothing to do
	}

	@Override
	public void visit(Transition transition) throws ParseException {
		transition.fix();
	}
}
