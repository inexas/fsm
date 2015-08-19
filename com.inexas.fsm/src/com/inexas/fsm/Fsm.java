package com.inexas.fsm;

import java.util.*;
import java.util.logging.*;
import org.jdom.Element;
import com.inexas.util.*;
import com.inexas.util.logging.InexasFormatter;

/**
 * Finite State machine From the UML specification: "A behavior that specifies
 * the sequences of states that an object or an interaction goes through during
 * its life in response to events, together with its responses and actions".
 *
 * The state machine has two meta-states: build-time and runtime. At build-time
 * the SM cannot be used, only built. The transition to runtime involves a
 * sanity check and once this is made the SM can be used but not changed.
 */
public class Fsm extends CompositeState {
	public static final String INITIAL_STATE_NAME = "INITIAL";
	public static final String FINAL_STATE_NAME = "FINAL";
	public static final String HISTORY_SHALLOW_NAME = "H";
	public static final String HISTORY_DEEP_NAME = "H*";
	public static final String START_NAME = "Start";
	public static final String FINALIZE_NAME = "Finalize";
	private final Timer timer = new Timer();
	private final Queue<Event> eventQueue;
	private final Logger logger;
	private final State finalState;
	private final int size;
	private Map<String, Event> eventRegister;
	private Map<String, Vertex> allVertices;
	// subject specific data...
	private boolean logging;
	// view specific data...
	private final Implementation implementation;
	private final Fsm subject;
	private final boolean canHaveTerminalActions;
	private boolean busy;
	private final boolean terminated = false;
	private int id = 0;

	/**
	 * Construct an FSM factory
	 *
	 * @param element
	 *            the JDOM element to construct from
	 * @throws ParseException
	 *             if an error is encountered
	 */
	Fsm(Element element) throws ParseException {
		super(null, element);

		// Name and implementation are loaded by VertexImpl

		canHaveTerminalActions = Boolean.parseBoolean(element.getAttributeValue("terminal-actions"));

		// final state...
		final Element finalStateElement = element.getChild("final-state");
		if(finalStateElement != null) {
			try {
				finalState = new FinalState(this, finalStateElement);
				vertices.put(finalState.getName(), finalState);
			} catch(final DuplicateException e) {
				throw new ParseException("FINAL state already defined");
			}
		} else {
			finalState = null;
		}

		final String loggerName = element.getAttributeValue("logger-name");
		logger = Logger.getLogger(loggerName == null ? name : loggerName);
		logger.setUseParentHandlers(false);

		// Set up the console handler...
		final Handler handler = new ConsoleHandler();
		handler.setFormatter(new InexasFormatter());
		logger.addHandler(handler);

		if(element.getAttributeValue("logger-level").equals("OFF")) {
			logging = false;
			logger.setLevel(Level.OFF);
		} else {
			logging = true;
			logger.setLevel(Level.INFO);
		}

		eventQueue = null;
		subject = null;

		implementation = null;

		@SuppressWarnings("unused")
		final Compiler compiler = new Compiler(this);

		size = allVertices.size() + 1; // todo Find out why I need +1
	}

	/**
	 * View constructor
	 *
	 * @param subject
	 * @param implementation
	 */
	public Fsm(Fsm subject, Implementation implementation) {
		super(subject);
		this.subject = subject;
		this.implementation = implementation;
		finalState = subject.finalState;
		logger = subject.logger;
		eventRegister = subject.eventRegister;
		allVertices = subject.allVertices;
		size = subject.size;
		eventQueue = new LinkedList<>();
		canHaveTerminalActions = subject.canHaveTerminalActions;

		reset(implementation);
	}

	/**
	 * Start the FSM. The initial-state may have one or more transitions. If a
	 * transition with the event name Fsm.START_NAME exists, it is used.
	 * Otherwise if there is one one transition it is used. Else an exception is
	 * thrown because you should have called start(String) to avoid the
	 * ambiguity.
	 */
	public void start() {
		final Event startingEvent = getStartingEvent();
		handle(startingEvent);
	}

	public void start(Map<String, Object> startParameters) {
		final Event startingEvent;
		if(startParameters == null) {
			startingEvent = getStartingEvent();
		} else {
			startingEvent = new Event(getStartingEvent(), startParameters);
		}
		handle(startingEvent);
	}

	public void start(String startingTransitionName) {
		handle(getEvent(startingTransitionName));
	}

	public Class<?> getImplementationClass() {
		return implementationClass;
	}

	/**
	 * Register an event, if the event has already been registered the we return
	 * it.
	 *
	 * @param eventName
	 *            the name of the event to register
	 * @return the event
	 */
	public Event registerEvent(String eventName) {
		assert eventName != null && eventName.length() > 0;

		// The following is a bit of a dirty trick but it's necessary and
		// it works. The eventRegister needs to be constructed before the
		// constructor of this class which is not really possible in Java
		if(eventRegister == null) {
			eventRegister = new HashMap<>();
		}

		Event result = eventRegister.get(eventName);
		if(result == null) {
			result = new Event(eventName, Event.Type.call);
			eventRegister.put(eventName, result);
		}

		return result;
	}

	/**
	 * Get an event from the event register
	 *
	 * @param eventName
	 *            the name of the event to retrieve
	 * @return the event
	 */
	public Event getEvent(String eventName) {
		final Event result = eventRegister.get(eventName);
		if(result == null) {
			throw new RuntimeException("No such event: " + eventName);
		}
		return result;
	}

	public Event getEvent(String eventName, Map<String, Object> parameters) {
		final Event event = getEvent(eventName);
		return new Event(event, parameters);
	}

	/**
	 * Fire an event. The event is passed to each compound state (including the
	 * FSM which is a composite state depth first. It is possible for actions to
	 * fire a other events - these are queued until the current event is
	 * completely handled.
	 */
	public Map<String, NameValue<? extends Object>> handle(Event event) {
		// !todo What's the return code about here. Change to boolean recognized
		// event?
		eventQueue.add(event);
		if(!busy) {
			busy = true;
			while(eventQueue.size() > 0) {
				if(terminated) {
					throw new RuntimeException("Event fired after FSM terminated: " + event);
				}
				final Event toFire = eventQueue.remove();
				logInfo("Handle event: " + toFire);
				if(toFire instanceof SynchEvent) {
					((SynchEvent)toFire).getSynchBar().fire(toFire, implementation);
				} else {
					super.handle(toFire, implementation);
				}
			}
			busy = false;
		}
		return null;
	}

	public Implementation getImplementation() {
		return implementation;
	}

	public Collection<Event> getActiveEvents() {
		final List<Event> returnValue = new ArrayList<>();
		for(final State state : getCompoundState(implementation)) {
			for(final Transition transition : state.getActiveTransitions(implementation)) {
				returnValue.add(transition.getEvent());
			}
		}
		return returnValue;
	}

	public Map<String, Event> getActiveEventMap() {
		final Map<String, Event> result = new HashMap<>();
		for(final State state : getCompoundState(implementation)) {
			for(final Transition transition : state.getActiveTransitions(implementation)) {
				final Event event = transition.getEvent();
				result.put(event.getName(), event);
			}
		}
		return result;
	}

	public Vertex getVertex(String path) {
		return allVertices.get(path);
	}

	/**
	 * Set the logging level, by default is at INFO
	 *
	 * @see Logger#setLevel(java.util.logging.Level)
	 * @param newLevel
	 */
	public void setLoggingLevel(Level newLevel) {
		// delegate to subject
		if(subject != null) {
			subject.setLoggingLevel(newLevel);
		} else {
			logger.setLevel(newLevel);
			// currently only info level is used
			logging = newLevel.intValue() >= Level.INFO.intValue();
		}
	}

	public TimerTask newTimerEvent(final Event event, long when) {
		final Date timeToRun = new Date(when);
		final TimerTask returnValue = new TimerTask() {
			@Override
			public void run() {
				handle(event);
			}
		};
		timer.schedule(returnValue, timeToRun);
		return returnValue;

	}

	public TimerTask newTimerEvent(final Event event, long firstOccurence, long interval) {
		final Date timeToRun = new Date(firstOccurence);
		final TimerTask returnValue = new TimerTask() {
			@Override
			public void run() {
				handle(event);
			}
		};
		timer.schedule(returnValue, timeToRun, interval);
		return returnValue;
	}

	public void logInfo(String message) {
		logger.info(message);
	}

	public boolean isLogging() {
		// delegate to subject
		return subject == null ? logging : subject.logging;
	}

	public void setState(int stateId, int state) {
		implementation.setState(stateId, state);
	}

	public int getState(int stateId) {
		return implementation.getState(stateId);
	}

	public List<State> getCompoundState() {
		return getCompoundState(implementation);
	}

	public boolean isTerminated() {
		return finalState != null && getState(finalState.getId()) == STATE_OCCUPIED;
	}

	public String getStateAsString() {
		return getCompoundState().toString();
	}

	public int size() {
		return size;
	}

	public boolean canHaveTerminalActions() {
		return canHaveTerminalActions;
	}

	@Override
	public String toString() {
		return "FSM:" + name + ':' + getStateAsString();
	}

	void setAllVertices(Map<String, Vertex> allVertices) {
		this.allVertices = allVertices;
	}

	int getNextId() {
		return id++;
	}

}