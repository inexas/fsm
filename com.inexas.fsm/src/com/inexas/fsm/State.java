package com.inexas.fsm;

import java.util.*;
import org.jdom.Element;

/**
 * A FSM state. A state is a vertex that can become occupied
 */
public class State extends Vertex {

	/**
	 * Constructor for states other than the FSM from XML
	 *
	 * @param parent
	 * @param element
	 * @throws ParseException
	 */
	protected State(CompositeState parent, Element element) throws ParseException {
		super(parent, element);
	}

	/**
	 * Used for history state
	 *
	 * @param name
	 * @param parent
	 * @throws ParseException
	 */
	protected State(String name, CompositeState parent) throws ParseException {
		super(name, parent);
	}

	State(Vertex subject) {
		super(subject);
	}

	Collection<Transition> getActiveTransitions(Implementation implementation1) {
		final Collection<Transition> returnValue = new ArrayList<>();
		for(final Transition transition : transitions.values()) {
			if(transition.isActive(implementation1)) {
				returnValue.add(transition);
			}
		}
		return returnValue;
	}

}
