package com.inexas.fsm;

interface Visitor {
	void visit(Vertex vertex) throws ParseException;

	void visit(Transition transition) throws ParseException;
}
