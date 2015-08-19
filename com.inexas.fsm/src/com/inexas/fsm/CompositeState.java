package com.inexas.fsm;

import java.util.*;
import org.jdom.Element;
import com.inexas.util.*;

public class CompositeState extends State {
	public static final int HISTORY_NONE = 0;
	public static final int HISTORY_DEEP = 1;
	public static final int HISTORY_SHALLOW = 2;
	protected final StrictMap<String, Vertex> vertices;
	protected State initialState; // ?todo this could be final
	private int historyType; // ?todo this could be final
	private Transition initialTransition; // ?todo this could be final

	/**
	 * Construct a composite state (or state machine) from a JDOM element
	 *
	 * @param parent
	 * @param element
	 *            the element to construct from
	 * @throws ParseException
	 */
	CompositeState(CompositeState parent, Element element) throws ParseException {
		super(parent, element);
		// <!ELEMENT composite-state (
		// initial-state?,
		// entry-action*,
		// (activity|composite-state|decision|signal|state|synch-bar)*,
		// transition*,
		// exit-action*) >
		// <!ATTLIST composite-state
		// name CDATA #REQUIRED
		// history (none|shallow|deep) "none">

		vertices = new StrictMap<>();

		// Initial state...
		final Element initialElement = element.getChild("initial-state");
		if(initialElement != null) {
			initialState = new InitialState(this, initialElement);
			initialTransition = initialState.getTransitions().values().iterator().next();
			vertices.put(initialState.getName(), initialState);
		}

		final String historyText = element.getAttributeValue("history");
		if(historyText != null) {
			if(historyText.equals("deep")) {
				setHistory(HISTORY_DEEP);
			} else if(historyText.equals("shallow")) {
				setHistory(HISTORY_SHALLOW);
			}
		} // else no history

		// entry-action loaded in StateImpl

		// states...
		// todo extend <?>
		Iterator<?> i = element.getChildren("state").iterator();
		while(i.hasNext()) {
			final Element stateElement = (Element)i.next();
			final State state = new State(this, stateElement);
			try {
				vertices.put(state.getName(), state);
			} catch(final DuplicateException e) {
				throw new ParseException(
						"Duplicate key '" + state.getName() +
						"' in " + getFullName());
			}
		}

		// composite-states...
		i = element.getChildren("composite-state").iterator();
		while(i.hasNext()) {
			final Element stateElement = (Element)i.next();
			final CompositeState state = new CompositeState(this, stateElement);
			vertices.put(state.getName(), state);
		}

		// synch-bars...
		i = element.getChildren("synch-bar").iterator();
		while(i.hasNext()) {
			final Element stateElement = (Element)i.next();
			final SynchBar state = new SynchBar(this, stateElement);
			vertices.put(state.getName(), state);
		}

		// activities...
		i = element.getChildren("activity-state").iterator();
		while(i.hasNext()) {
			final Element activityElement = (Element)i.next();
			final ActivityState activity = new ActivityState(this, activityElement);
			vertices.put(activity.getName(), activity);
		}

		// decisions...
		i = element.getChildren("decision").iterator();
		while(i.hasNext()) {
			final Element decisionElement = (Element)i.next();
			final Decision decision = new Decision(this, decisionElement);
			vertices.put(decision.getName(), decision);
		}

		// signals...
		i = element.getChildren("signal").iterator();
		while(i.hasNext()) {
			final Element signalElement = (Element)i.next();
			final Signal signal = new Signal(this, signalElement);
			vertices.put(signal.getName(), signal);
		}

		// entry-action loaded in StateImpl
	}

	CompositeState(CompositeState subject) {
		super(subject);
		vertices = subject.vertices;
		initialState = subject.initialState;
		historyType = subject.historyType;
		initialTransition = subject.initialTransition;
	}

	public Map<String, Vertex> getVertices() {
		return vertices;
	}

	public Vertex getInitialState() {
		return initialState;
	}

	List<State> getCompoundState(Implementation implementation) {
		final List<State> returnValue = new ArrayList<>();
		getCompoundState(returnValue, implementation);
		return returnValue;
	}

	public History getHistory() {
		final History returnValue;
		switch(historyType) {
		case HISTORY_NONE:
			returnValue = null;
			break;

		case HISTORY_SHALLOW:
			returnValue = (History)vertices.get(Fsm.HISTORY_SHALLOW_NAME);
			assert returnValue != null;
			break;

		case HISTORY_DEEP:
			returnValue = (History)vertices.get(Fsm.HISTORY_SHALLOW_NAME);
			assert returnValue != null;
			break;

		default:
			throw new RuntimeException("Invalid history type: " + historyType);
		}
		return returnValue;
	}

	public int getHistoryType() {
		return historyType;
	}

	public Transition getInitialTransition() {
		return initialTransition;
	}

	// non-public methods

	@Override
	boolean handle(Event event, Implementation implementation) {
		// find all the occupied states before we change anything...
		final List<State> occupiedStates = new ArrayList<>();
		for(final Vertex vertex : vertices.values()) {
			if(vertex instanceof State) {
				final State state = (State)vertex;
				if(implementation.getState(state.getId()) == STATE_OCCUPIED) {
					occupiedStates.add(state);
				}
			}
		}

		boolean returnValue = false; // assume the worst

		// now fire the event in the occupied state children...
		for(final Vertex state : occupiedStates) {
			if(state.handle(event, implementation)) {
				returnValue = true;
			}
		}

		// and then handle it ourself...
		if(super.handle(event, implementation)) {
			returnValue = true;
		}

		return returnValue;
	}

	@Override
	void reset(Implementation implementation) {
		// women and children first...
		for(final Vertex vertex : vertices.values()) {
			vertex.reset(implementation);
		}
		// now me...
		super.reset(implementation);
	}

	@Override
	void enter(Event event, Implementation implementation) {
		// if the state was suspended we have to exit first...
		if(implementation.getState(getId()) == STATE_SUSPENDED) {
			exit(event, implementation);
		}

		// now enter our state...
		super.enter(event, implementation);
	}

	/**
	 * Depending on the history we have to either call suspend or exit on the
	 * children but suspend ourselves
	 */
	@Override
	void suspend(Transition transition, Event event, Implementation implementation) {
		for(final Vertex vertex : vertices.values()) {
			if(implementation.getState(vertex.getId()) == STATE_OCCUPIED) {
				if(vertex.isHistorized()) {
					vertex.suspend(transition, event, implementation);
				} else {
					vertex.exit(event, implementation);
				}
			}
		}
		super.suspend(transition, event, implementation);
	}

	@Override
	void resume(Event event, Implementation implementation) {
		final Iterator<Vertex> i = vertices.values().iterator();
		boolean seenSomeSuspendedStates = false;
		while(i.hasNext()) {
			final Vertex vertex = i.next();
			if(implementation.getState(vertex.getId()) == STATE_SUSPENDED) {
				vertex.resume(event, implementation);
				seenSomeSuspendedStates = true;
			}
		}
		if(!seenSomeSuspendedStates) {
			// This CS has not been suspended so the transition to HISTORY does
			// the same as a start. However composite states (other than the FSM
			// itself, may not have initial states to that's going to generate
			// a runtime error...
			implementation.setState(initialState.getId(), Vertex.STATE_OCCUPIED);
			handle(getStartingEvent(), implementation);
		}
		super.resume(event, implementation);
	}

	protected Event getStartingEvent() {
		final Event result;
		if(initialState == null) {
			throw new RuntimeException(
					getFullName() + " needs an initial-state but does not have one defined");
		}
		final Map<String, Transition> transitionMap = initialState.getTransitions();
		final int transitionCount = transitionMap.size();
		if(transitionCount == 1) {
			final Transition transition = transitionMap.entrySet().iterator().next().getValue();
			result = transition.getEvent();
		} else if(transitionCount > 1) {
			result = fsm.getEvent(Fsm.START_NAME);
		} else {
			// todo This is bad so I should trap it in a sanity check during
			// parsing...
			throw new RuntimeException(
					"There are more than one transitions from the initial state in " +
							getFullName() + " so I don't know which one to use");
		}
		return result;
	}

	@Override
	void exit(Event event, Implementation implementation) {
		final Iterator<Vertex> i = vertices.values().iterator();
		while(i.hasNext()) {
			final Vertex vertex = i.next();
			if(implementation.getState(vertex.getId()) != STATE_EMPTY) {
				vertex.exit(event, implementation);
			}
		}
		super.exit(event, implementation);
	}

	@Override
	void visit(Visitor visitor) throws ParseException {
		// women and children first...
		final Iterator<Vertex> i = vertices.values().iterator();
		while(i.hasNext()) {
			i.next().visit(visitor);
		}
		// now visit me...
		super.visit(visitor);
	}

	@Override
	protected void fix() throws ParseException {
		vertices.lock();
		super.fix();
	}

	private void getCompoundState(Collection<State> currentStates, Implementation implementation) {
		final Iterator<Vertex> i = vertices.values().iterator();
		while(i.hasNext()) {
			final Vertex vertex = i.next();
			if(implementation.getState(vertex.getId()) == STATE_OCCUPIED) {
				currentStates.add((State)vertex);
				if(vertex instanceof CompositeState) {
					((CompositeState)vertex).getCompoundState(currentStates, implementation);
				}
			}
		}
	}

	// Build-time methods

	private void setHistory(int historyType) throws ParseException {
		if(this.historyType != HISTORY_NONE) {
			throw new RuntimeException("History already defined for " + getName());
		}

		this.historyType = historyType;
		switch(historyType) {
		case HISTORY_NONE:
			break;

		case HISTORY_SHALLOW:
			final History shallow = new History(this, false);
			vertices.put(shallow.getName(), shallow);
			break;

		case HISTORY_DEEP:
			final History deep = new History(this, true);
			vertices.put(deep.getName(), deep);
			break;

		default:
			throw new RuntimeException("Invalid history type: " + historyType);
		}
	}

	// @Override protected final boolean deriveHistorized() {
	// boolean returnValue;
	// if(parent == null) {
	// // we are the FSM and never historized...
	// returnValue = false;
	// } else {
	// if(historyType != CompositeState.HISTORY_NONE || parent.getHistoryType()
	// != CompositeState.HISTORY_NONE) {
	// returnValue = true;
	// } else {
	// CompositeState ancestor = parent.parent;
	// returnValue = false;
	// while(ancestor != null) {
	// if(ancestor.getHistoryType() == CompositeState.HISTORY_DEEP) {
	// returnValue = true;
	// break;
	// }
	// ancestor = ancestor.getParent();
	// }
	// }
	// }
	// return returnValue;
	// }
	//
}
