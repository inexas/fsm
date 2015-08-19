package com.inexas.fsm;

import java.util.*;
import com.inexas.util.StrictMap;

class CheckVisitor implements Visitor {

	@Override
	public void visit(Vertex vertex) throws ParseException {
		if(vertex instanceof Fsm) {
			assert vertex.getFsm() == vertex;
			assert vertex.getParent() == null;
		} else {
			assert vertex.getFsm() != null;
			assert vertex.getParent() != null;
		}
		final String name = vertex.getName();
		if(name == null || name.length() == 0 || name.contains("/")) {
			throw new ParseException("Invalid vertex name: " + name);
		}

		if(vertex.getTransitions().size() > 0 && (
				vertex instanceof History ||
				vertex instanceof FinalState)) {
			throw new ParseException(
					"Vertex can not have outgoing transition: " + vertex.getFullName());
		}

		check(vertex.getEntryActions());
		check(vertex.getExitActions());
		final Map<String, Transition> transitions = vertex.getTransitions();
		if(vertex instanceof FinalState) {
			assert transitions.size() == 0;
		}
		if((vertex instanceof Fsm) != (vertex.getFullName().length() == 0)) {
			throw new ParseException("Vertex has zero length name: " + vertex.getName());
		}

		if(vertex instanceof ActivityState) {
			check((ActivityState)vertex);
		} else if(vertex instanceof Decision) {
			check((Decision)vertex);
		} else if(vertex instanceof History) {
			check((History)vertex);
		} else if(vertex instanceof Signal) {
			check((Signal)vertex);
		} else if(vertex instanceof State) {
			check(vertex);
		} else if(vertex instanceof SynchBar) {
			check((SynchBar)vertex);
		}
	}

	private void check(ActivityState activityState) {
		final String fullName = activityState.getFullName();
		final Collection<Transition> transitions = activityState.getTransitions().values();
		assert transitions.size() == 1 : "ActivityStates must have one outgoing transition: " + fullName;
		final Transition transition = transitions.iterator().next();
		assert transition.getGuard() == null : "ActivityStates exit transition cannot have guards: "
				+ transition.getFullName();
		// assert transition.getEvent() == null :
		// "ActivityStates exit transition cannot have events: "
		// + transition.getFullName();
	}

	private void check(Decision decision) {
		final StrictMap<String, Transition> cases = (StrictMap<String, Transition>)decision.getCases();
		assert cases.isLocked() : "Cases not locked in " + decision.getFullName();
		assert decision.getDefaultTransition() != null || cases.size() > 0 : "No cases or default in "
				+ decision.getFullName();
		assert decision.getQuery() != null : "Missing query in " + decision.getFullName();
	}

	private void check(History history) {
		assert history.getTransitions().size() == 0 : "History contains outgoing transitions";
	}

	private void check(@SuppressWarnings("unused") final Signal signal) {
		// nothing to check
	}

	private void check(Vertex state) {
		// no checks for state
		if(state instanceof CompositeState) {
			check((CompositeState)state);
		} else if(state instanceof InitialState) {
			check((InitialState)state);
		} else if(state instanceof FinalState) {
			check((FinalState)state);
		}
	}

	private void check(CompositeState compositeState) {
		final StrictMap<String, Vertex> vertices = (StrictMap<String, Vertex>)compositeState.getVertices();
		assert vertices.isLocked();
		assert vertices.size() > 0 : "Composite state has no children: " + compositeState.getFullName();
	}

	private void check(InitialState initialState) {
		final Map<String, Transition> transitions = initialState.getTransitions();
		assert transitions.size() >= 1;
		for(final Transition transition : transitions.values()) {
			if(transition.getGuard() != null) {
				throw new RuntimeException(
						"Transitions from initial states may not have guards, " +
								transition.getFullName() + " does");
			}

			final Vertex target = transition.getTarget();
			if(target == initialState) {
				throw new RuntimeException(
						"Transitions from initial states may not point at initial states, " +
								transition.getFullName() + " does");
			}

			if(!target.isSibling(initialState)) {
				throw new RuntimeException(
						"Transitions from initial states must target a sibing, " +
								transition.getFullName() + " does not");
			}

			if(target instanceof History) {
				throw new RuntimeException(
						"Transitions from initial states must target a history state, " +
								transition.getFullName() + " does");
			}

			if(transition.getEvent() == null) {
				throw new RuntimeException(
						"Transitions from initial states must have an event, " +
								transition.getFullName() + " does not");
			}
		}
	}

	private void check(FinalState finalState) {
		assert finalState.getTransitions().size() == 0;
	}

	private void check(SynchBar synchBar) throws ParseException {
		final List<Transition> incoming = synchBar.getIncoming();

		// check incoming transitions...
		for(final Transition transition : incoming) {
			final String tName = transition.getName();

			// they must originate at a state...
			if(!(transition.getSource() instanceof State)) {
				throw new ParseException(
						"Incoming transitions to synchbars must originate at a state, " +
								tName + " in " + synchBar.getFullName() + " does not");
			}

			// can't have events...
			if(transition.getEvent() != null) {
				throw new ParseException(
						"Incoming transitions to synchbar can not have events, " +
								transition.getName() + " in " + synchBar.getFullName() + " does");
			}
		}

		// check outgoing transitions...
		for(final Transition transition : synchBar.getTransitions().values()) {
			final String tName = transition.getName();

			// they must terminate at a state...
			if(!(transition.getTarget() instanceof State)) {
				throw new ParseException(
						"Outgoing transitions from synchbars must terminate at a state, " +
								tName + " in " + synchBar.getFullName() + " does not");
			}

			// can't have events...
			if(transition.getEvent() != null) {
				throw new ParseException(
						"Outgoing transitions from synchbars can not have events, " +
								transition.getName() + " in " + synchBar.getFullName() + " does");
			}
		}

	}

	private void check(Action actions[]) {
		assert actions == null || actions.length > 0;
	}

	@Override
	public void visit(Transition transition) throws ParseException {
		final Event event = transition.getEvent();
		final Vertex source = transition.getSource();
		final Vertex target = transition.getTarget();
		final String fullName = transition.getFullName();
		final String name = transition.getName();

		if(name == null || name.length() == 0) {
			throw new ParseException("Invalid transition name: " + name);
		}

		assert source != null;
		assert target != null;

		final Action actions[] = transition.getActions();
		assert actions == null || actions.length > 0;

		assert transition.getFsm() != null;

		if(event == null) {
			// if there are no events then the source must either to
			// from a synch bar, from the initial state or to the
			// final state...
			if(!(source instanceof SynchBar ||
					source instanceof InitialState ||
					source instanceof ActivityState ||
					source instanceof Decision || target instanceof SynchBar)) {
				throw new ParseException("Transition must have an event: " + fullName);
			}
		}

		if(target instanceof SynchBar && event != null) {
			throw new ParseException("Transitions to synchbars should not have events: " + fullName);
		}

		if(target instanceof History) {
			// transitions must come from outside the CS, no exits
			if(source.isDescendentOf(target.getParent())) {
				throw new ParseException(
						"Transitions to history should originate outside the CS: " + fullName);
			}
		}

		if(source instanceof Decision) {
			// outgoing transitions cannot have events
			assert event == null : "Transitions from decisions should not have events: " + fullName;
		}

		if(target instanceof CompositeState && !transition.isReaction()) {
			// target CSs must have an initial state
			assert ((CompositeState)target).getInitialState() != null;
		}
	}

}
