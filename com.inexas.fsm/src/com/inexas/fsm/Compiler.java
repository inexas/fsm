package com.inexas.fsm;

import java.util.Map;

class Compiler {
	private final Map<String, Vertex> allVertices;

	public Compiler(Fsm fsm) throws ParseException {
		// collect all the vertices...
		final GetAllVerticesVisitor gavv = new GetAllVerticesVisitor();
		fsm.visit(gavv);
		allVertices = gavv.getMap();
		fsm.setAllVertices(allVertices);

		// fix the model
		fsm.visit(new FixTransitionsVisitor());
		fsm.visit(new FixVerticesVisitor());
		fsm.visit(new CheckVisitor());
	}

	public void checkVerticesReachable(Fsm fsm) {
		// travers all states from the FSM downwards removing them
		// from allVertices as we go
		visit(fsm.getInitialState());
		if(allVertices.size() != 0) {
			final StringBuilder sb = new StringBuilder("The following vertices are unreachable:");
			for(Vertex vertex : allVertices.values()) {
				sb.append("\n\t");
				sb.append(vertex.getFullName());
			}
		}
	}

	private void visit(Vertex vertex) {
		if(allVertices.remove(vertex.getFullName()) != null) {
			// we haven't been here before...
			for(Transition transition : vertex.getTransitions().values()) {
				visit(transition.getTarget());
			}
		}
		if(vertex instanceof CompositeState) {
			// a transition terminating at a CS causes the initialstate
			// to be taken so follow that...
			visit(((CompositeState)vertex).getInitialState());
		}
	}

}
