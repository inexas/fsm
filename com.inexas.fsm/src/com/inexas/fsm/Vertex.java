package com.inexas.fsm;

import java.util.*;
import org.jdom.Element;
import com.inexas.util.*;

public abstract class Vertex implements Comparable<State> {
	public static int STATE_EMPTY = 1;
	public static int STATE_OCCUPIED = 2;
	public static int STATE_SUSPENDED = 3;
	private static final Set<String> invalidNames;
	static {
		final Set<String> invalidNamesTmp = new HashSet<>(7);
		invalidNamesTmp.add(null);
		invalidNamesTmp.add("");
		invalidNamesTmp.add(Fsm.INITIAL_STATE_NAME);
		invalidNamesTmp.add(Fsm.FINAL_STATE_NAME);
		invalidNamesTmp.add(Fsm.HISTORY_SHALLOW_NAME);
		invalidNamesTmp.add(Fsm.HISTORY_DEEP_NAME);
		invalidNames = Collections.unmodifiableSet(invalidNamesTmp);
	}
	protected final Fsm fsm;
	// todo I shouldn't really need this as a field, it's only used in
	// construction
	// todo and it shouldn't be <?>
	protected final Class<?> implementationClass;
	protected final String name;
	private final String fullName;
	protected final CompositeState parent;
	protected final StrictMap<String, Transition> transitions;
	protected final Action entryActions[];
	protected final Action exitActions[];
	private final int id;
	// view specific data...
	// none (it's in FSM)

	private boolean historized;
	private int synchBarTransitionSize;
	private SynchBar[] synchBarTransitions; // todo move to state?

	/**
	 * Viewable constructor. This is used for the FSM only
	 *
	 * @param subject
	 */
	protected Vertex(Vertex subject) {
		fsm = subject.fsm;
		implementationClass = subject.implementationClass;
		name = subject.name;
		fullName = subject.fullName;
		parent = subject.parent;
		historized = subject.historized;
		entryActions = subject.entryActions;
		exitActions = subject.exitActions;
		transitions = subject.transitions;
		synchBarTransitions = subject.synchBarTransitions;
		synchBarTransitionSize = subject.synchBarTransitionSize;
		id = subject.id;
	}

	protected Vertex(CompositeState parent, Element element) throws ParseException {

		name = checkName(element.getAttributeValue("name"));

		if(this instanceof Fsm) { // it's the FSM...
			assert parent == null;
			fsm = (Fsm)this;
			this.parent = null;

			final String implementationClassName = element.getAttributeValue("implementation");
			try {
				if(implementationClassName == null) {
					implementationClass = Implementation.class;
				} else {
					implementationClass = Class.forName(implementationClassName);
				}
			} catch(final ClassNotFoundException e) {
				throw new ParseException("Can't load class: " + implementationClassName, e);
			}
		} else { // not the FSM...
			assert parent != null : "Null parent?";
			this.parent = parent;
			fsm = parent.fsm;
			implementationClass = ((Vertex)parent).implementationClass;
		}

		id = fsm.getNextId();

		// entry actions...
		entryActions = Action.loadElements(implementationClass, element.getChildren("entry-action"), fsm);

		// Transitions...
		transitions = new StrictMap<>();
		Iterator<?> i = element.getChildren("transition").iterator();
		while(i.hasNext()) {
			// The to-state will have to be resolved later as it
			// might be a forward reference and so not loaded yet...
			final Element transitionElement = (Element)i.next();
			newTransition(transitionElement, null, null);
		}
		i = element.getChildren("reaction").iterator();
		while(i.hasNext()) {
			final Element transitionElement = (Element)i.next();
			newTransition(transitionElement, null, null);
		}

		// exit actions...
		exitActions = Action.loadElements(implementationClass, element.getChildren("exit-action"), fsm);

		fullName = makeFullName();
	}

	/**
	 * Construct a new vertex. This is used for History, FinalState and
	 * InitialStates.
	 *
	 * @param name
	 *            the name of the state, must be unique within the parent state
	 * @param parent
	 *            the parent composite state (or FSM)
	 * @throws ParseException
	 */
	protected Vertex(String name, CompositeState parent) throws ParseException {
		assert parent != null;

		this.name = checkName(name);
		this.parent = parent;
		this.fsm = parent.fsm;
		this.entryActions = null;
		this.exitActions = null;
		fullName = makeFullName();
		implementationClass = parent.implementationClass;
		transitions = new StrictMap<>();
		id = fsm.getNextId();
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Fsm getFsm() {
		return fsm;
	}

	public CompositeState getParent() {
		return parent;
	}

	public String getFullName() {
		return fullName;
	}

	public Map<String, Transition> getTransitions() {
		return transitions;
	}

	public int getState() {
		return fsm.getState(id);
	}

	// standard methods

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object rhsObject) {
		boolean returnValue;
		if(this == rhsObject) { // try for a cheap true...
			returnValue = true;
		} else if(rhsObject == null) { // try for a cheap false...
			returnValue = false;
		} else {
			if(rhsObject instanceof Vertex) {
				final Vertex rhs = (Vertex)rhsObject;
				returnValue = getClass() == rhs.getClass() && name.equals(rhs.name);
			} else {
				returnValue = false;
			}
		}
		return returnValue;
	}

	/**
	 * Return a visual representation of this vertex as "v:" plus the full name
	 */
	@Override
	public String toString() {
		return getFullName();
	}

	Action[] getEntryActions() {
		return entryActions;
	}

	Action[] getExitActions() {
		return exitActions;
	}

	// non-public methods

	/**
	 * Reset is called to (re-)initialize the vertex and set it in an empty
	 * state, it is overridden by CS and InitialState
	 *
	 * @see InitialState#reset(Implementation)
	 * @see CompositeState#reset(Implementation)
	 */
	void reset(Implementation implementation) {
		implementation.setState(id, STATE_EMPTY);
	}

	/**
	 * This is called when the vertex is entered either directly or through a a
	 * history when the vertex has never been occupied
	 *
	 * @param event
	 *            the event causing the transition
	 */
	void enter(Event event, Implementation implementation) {
		assert implementation.getState(id) == STATE_EMPTY : "State " + getFullName()
				+ " entered but current state is "
				+ implementation.getState(id);
		if(fsm.isLogging()) {
			fsm.logInfo(
					"Enter state: " + (this instanceof Fsm ? "FSM:" + name : this) +
					" for " + event);
		}
		occupy(event, implementation);
	}

	/**
	 * This is called when the vertex is exited but it is historic so we can't
	 * exit the state
	 *
	 * @param transition
	 *            the transition causing the suspend
	 * @param event
	 *            the event causing the transition
	 */
	void suspend(
			Transition transition,
			Event event,
			Implementation implementation) {
		assert implementation.getState(id) == STATE_OCCUPIED;
		if(fsm.isLogging()) {
			fsm.logInfo(
					"Suspend state: " + (this instanceof Fsm ? "FSM:" + name : this) +
					" for " + event);
		}
		// We can't call the exit actions because we haven't terminated which
		// isn't very nice. The only alternative I see is that the exit actions
		// are called when the final state is entered by visiting the entire
		// FSM and looking for suspended states. Doing so might cause some
		// behavior that might not be acceptable for developers though...
		implementation.setState(id, STATE_SUSPENDED);
	}

	/**
	 * This is called when the vertex is re-entered through a transition to
	 * either a H in the parent or a H* in an ancestor
	 *
	 * @param event
	 *            the event causing the transition
	 */
	void resume(Event event, Implementation implementation) {
		assert implementation.getState(id) == STATE_SUSPENDED;
		if(fsm.isLogging()) {
			fsm.logInfo(
					"Resume state: " + (this instanceof Fsm ? "FSM:" + name : this) +
					" for " + event);
		}
		occupy(event, implementation);
	}

	/**
	 * This is called when the vertex is exited out of the vertex or by a
	 * termination of the FSM
	 *
	 * @param event
	 *            the event causing the transition
	 */
	void exit(Event event, Implementation implementation) {
		assert implementation.getState(id) != STATE_EMPTY : "Invalid state: " + implementation.getState(id);
		if(fsm.isLogging()) {
			fsm.logInfo(
					"Exit state: " + (this instanceof Fsm ? "FSM:" + name : this) +
					" for " + event);
		}
		Action.invoke(exitActions, event, implementation);
		implementation.setState(id, STATE_EMPTY);
	}

	boolean isDescendentOf(CompositeState possibleParent) {
		final boolean returnValue;
		if(parent == null) {
			returnValue = false; // I am the FSM and the descendent of noone
		} else if(parent == possibleParent) {
			returnValue = true; // my parent is the parent we are looking for
		} else {
			// otherwise if my parent is a descendent of the possible parent
			// then I must be too..
			returnValue = ((Vertex)parent).isDescendentOf(possibleParent);
		}
		return returnValue;
	}

	Transition newTransition(Element element, String newName, String defaultEventName) throws ParseException {
		final boolean isReaction = element.getName().equals("reaction");
		final Transition transition = new Transition(
				this,
				element, newName, defaultEventName,
				isReaction);
		add(transition);
		return transition;
	}

	void visit(Visitor visitor) throws ParseException {
		visitor.visit(this);
		final Iterator<Transition> i = transitions.values().iterator();
		while(i.hasNext()) {
			i.next().visit(visitor);
		}
	}

	@SuppressWarnings("unused")
	void fix() throws ParseException {
		transitions.lock();

		// check if we need to trigger any synch bars...
		final List<SynchBar> list = new ArrayList<>();
		for(final Transition transition : transitions.values()) {
			final Vertex target = transition.getTarget();
			if(target instanceof SynchBar) {
				list.add((SynchBar)target);
			}
		}
		synchBarTransitionSize = list.size();
		synchBarTransitions = synchBarTransitionSize == 0 ? null : list
				.toArray(new SynchBar[synchBarTransitionSize]);
		historized = deriveHistorized();
	}

	boolean handle(Event event, Implementation implementation) {
		boolean returnValue = false; // assume the worst
		final Iterator<Transition> i = transitions.values().iterator();
		while(i.hasNext()) {
			final Transition transition = i.next();
			if(transition.isActive(event, implementation)) {
				transition.handle(event, implementation);
				returnValue = true;
			}
		}
		return returnValue;
	}

	boolean isSibling(Vertex vertex) {
		return parent == vertex.getParent();
	}

	boolean isHistorized() {
		return historized;
	}

	/**
	 * The historized boolean is used to decide whether to call exit or suspend
	 * when a transition leads away from a vertex. The rules are different for
	 * CSs than other vertices
	 *
	 * @return true if this vertex it historized
	 */
	protected boolean deriveHistorized() {
		boolean returnValue;
		if(parent == null) {
			returnValue = false;
		} else if(parent.getHistoryType() != CompositeState.HISTORY_NONE) {
			returnValue = true;
		} else {
			CompositeState ancestor = parent.getParent();
			returnValue = false;
			while(ancestor != null) {
				if(ancestor.getHistoryType() == CompositeState.HISTORY_DEEP) {
					returnValue = true;
					break;
				}
				ancestor = ancestor.getParent();
			}
		}
		return returnValue;
	}

	private String makeFullName() {
		final StringBuilder sb = new StringBuilder();
		if(parent != null) {
			if(!(parent instanceof Fsm)) {
				sb.append(parent.getFullName());
			}
			sb.append('/');
			sb.append(name);
		}
		return sb.toString();
	}

	protected void add(Transition transition) {
		try {
			transitions.put(transition.getName(), transition);
		} catch(final DuplicateException e) {
			throw new RuntimeException("Vertex " + name + " already contains a transition "
					+ "with the name " + transition.getName(), e);
		}
	}

	private String checkName(String toCheck) throws ParseException {
		final String result;
		if(this instanceof InitialState) {
			assert toCheck == null;
			result = Fsm.INITIAL_STATE_NAME;
		} else if(this instanceof FinalState) {
			assert toCheck == null;
			result = Fsm.FINAL_STATE_NAME;
		} else if(this instanceof History) {
			if(!(Fsm.HISTORY_SHALLOW_NAME.equals(toCheck) || Fsm.HISTORY_DEEP_NAME.equals(toCheck))) {
				throw new ParseException("Invalid final state name: " + toCheck);
			}
			result = toCheck;
		} else {
			if(invalidNames.contains(toCheck) || toCheck.indexOf('/') >= 0) {
				throw new ParseException("Invalid name: " + toCheck);
			}
			result = toCheck;
		}
		return result;
	}

	/**
	 * We're called when by both enter() and resume()
	 */
	private void occupy(Event event, Implementation implementation) {
		Action.invoke(entryActions, event, implementation);
		implementation.setState(id, STATE_OCCUPIED);

		// check if we need to trigger any synch bars...
		for(int i = 0; i < synchBarTransitionSize; i++) {
			final SynchBar synchBar = synchBarTransitions[i];
			synchBar.isSynched(implementation);
		}
	}

	@Override
	public int compareTo(State rhs) {
		return getFullName().compareTo(rhs.getFullName());
	}

}
