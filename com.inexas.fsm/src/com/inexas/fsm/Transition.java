package com.inexas.fsm;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import org.jdom.Element;
import com.inexas.util.*;

public class Transition {
	private static final Class<?> guardArgTypes[] = new Class[0];
	private static final Object guardArgs[] = new Object[0];
	private final String name;
	private final Vertex source;
	private final String targetName;
	private final Event event;
	private final Method guard;
	private final Fsm fsm;
	private final Action actions[];
	private final boolean reaction;
	private Vertex exitVertex;
	private Vertex entryList[];
	private Vertex target;
	private boolean resume;

	/**
	 * Construct from XML
	 *
	 * @param source
	 *            source vertex
	 * @param element
	 *            the JDOM element to construct from
	 * @param name
	 *            if not null this will override the name in the XML
	 * @param defaultEventName
	 *            if no event then use this if not null
	 * @throws ParseException
	 */
	Transition(Vertex source, Element element, String name, String defaultEventName, boolean reaction)
			throws ParseException {

		// <!ELEMENT transition (transition-action*)>
		// <!ATTLIST transition
		// name CDATA #IMPLIED
		// event CDATA #IMPLIED
		// target CDATA #REQUIRED
		// guard CDATA #IMPLIED>

		this.source = source;
		this.reaction = reaction;

		this.fsm = source.getFsm();

		// Get or create the event...
		final String eventName = element.getAttributeValue("event");
		if(eventName == null || eventName.length() == 0) {
			// No name specified in XML file...
			if(defaultEventName != null) {
				event = fsm.registerEvent(defaultEventName);
			} else {
				event = null;
			}
		} else {
			event = fsm.registerEvent(eventName);
		}

		/*
		 * The target vertex may not have been loaded yet so we'll just take its
		 * name, if the name is _SELF then the it's a transition to itself so we
		 * can resolve it...
		 */
		final String elementTargetName = element.getAttributeValue("target");
		if("_SELF".equals(elementTargetName) || reaction) {
			targetName = source.getName();
			target = source;
			if(target instanceof CompositeState && !reaction) {
				Logger.getLogger("com.inexas.bpm").warning(source.getFullName() + " has a transition " +
						"to self; wouldn't it be better pointing to a history vertex " +
						"so you don't lose state?");
			}
		} else {
			targetName = elementTargetName;
			target = null;
		}

		// Figure out what the transition name should be, it's either
		// - given to us by the caller
		// - a default value also provided by the caller
		// - specified in the XML
		// - or we'll construct it in the form: sourceName-eventName-targetName
		if(name != null && name.length() == 0) {
			this.name = name;
		} else {
			final String xmlName = element.getAttributeValue("name");
			if(xmlName != null && xmlName.length() > 0) {
				this.name = xmlName;
			} else if(event == null) {
				this.name = source.getName() + "-" + targetName;
			} else {
				this.name = source.getName() + "-" + event.getName() + "-" + targetName;
			}
		}

		final String elementName = reaction ? "reaction-action" : "transition-action";
		actions = Action.loadElements(fsm.implementationClass, element.getChildren(elementName), fsm);

		guard = getGuard(element.getAttributeValue("guard"), source.implementationClass);
		assert targetName != null;
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return source.getFullName() + "::" + name;
	}

	public Vertex getSource() {
		return source;
	}

	public Vertex getTarget() {
		assert target != null;
		return target;
	}

	public Event getEvent() {
		return event;
	}

	/**
	 * Return the name of the transition. The name is constructed in the form:
	 * event-name(parameter-list) [guard] / action-list ^ event-list
	 *
	 * @return the name of the transition
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(reaction ? "Reaction<" : "Transition<");
		sb.append(source.getFullName());
		sb.append("->");
		sb.append(target.getFullName());
		sb.append("> ");

		if(event != null) {
			sb.append(event.getName());
		}
		if(guard != null) {
			sb.append('[');
			sb.append(guard.getName());
			sb.append(']');
		}
		if(actions != null) {
			sb.append('/');
			for(int i = 0; i < actions.length; i++) {
				if(i > 0) {
					sb.append(',');
				}
				sb.append(actions[i].getName());
			}
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object rhsObject) {
		boolean returnValue;
		if(this == rhsObject) { // try for a cheap true...
			returnValue = true;
		} else if(rhsObject == null) { // try for a cheap false...
			returnValue = false;
		} else {
			if(rhsObject instanceof Transition) {
				final Transition rhs = (Transition)rhsObject;
				// both LHS and RHS are the same types, check for an exact
				// match...
				returnValue = toString().equals(rhs.toString());
			} else {
				returnValue = false; // not the same types: false...
			}
		}
		return returnValue;
	}

	boolean invokeGuard(Implementation implementation) {
		final boolean returnValue;
		if(guard == null) {
			returnValue = true;
		} else {
			try {
				final Object returnObject = guard.invoke(implementation, guardArgs);
				returnValue = ((Boolean)returnObject).booleanValue();
			} catch(final Exception e) {
				throw new RuntimeException("Error invoking guard: " + guard.getName(), e);
			}
		}
		return returnValue;
	}

	void visit(Visitor visitor) throws ParseException {
		visitor.visit(this);
	}

	/**
	 * This is called when we stitch from build- to runtime. We may need to set
	 * the target vertex if we've only been given the name and we have to set
	 * the entry and exit lists.
	 *
	 * @throws ParseException
	 */
	void fix() throws ParseException {
		// figure out the target...
		if(target == null) {
			// the path may be either relative or absolute, it
			// starts with a '/' if it's absolute...
			final String path = targetName.startsWith("/") ?
					targetName :
					source.getParent().getFullName() + "/" + targetName;
			try {
				target = fsm.getVertex(path);
			} catch(final NotFoundException e) {
				throw new ParseException(
						"Target state not defined for transition to " + path +
								" in " + source.getFullName());
			}
		}

		// E x i t a n d e n t r y l i s t s . . .

		// Get the common parent..
		CompositeState commonParent = source.getParent();
		exitVertex = source;
		while(!target.isDescendentOf(commonParent)) {
			exitVertex = commonParent;
			commonParent = commonParent.getParent();
		}

		final Vertex actualTarget;
		if(target instanceof History) {
			// entry list depends on target...
			actualTarget = target.getParent();
			resume = true;
		} else {
			actualTarget = target;
			resume = false;
		}

		entryList = getAncestors(actualTarget, commonParent, true);
	}

	/**
	 * Attempt to handle the event. If the event fires the transition return
	 * return otherwise false
	 *
	 * @param firedEvent
	 *            the event to handle
	 */
	void handle(Event firedEvent, Implementation implementation) {

		// we can take the exit from the source state but if the
		// target is a decision it needs to have an exit transition
		// that we will take otherwise we can't exit the source
		// state...
		if(target instanceof Decision) {
			final Decision decision = (Decision)target;
			final Transition exitTransition = decision.query(implementation);
			if(exitTransition != null) {
				takeActions(firedEvent, implementation);
				exitTransition.handle(firedEvent, implementation);
			}
		} else {
			takeActions(firedEvent, implementation);
		}
	}

	Method getGuard() {
		return guard;
	}

	Fsm getFsm() {
		return fsm;
	}

	Action[] getActions() {
		return actions;
	}

	boolean isActive(Event toTest, Implementation implementation) {
		return (toTest == null || toTest.equals(event) && isActive(implementation));
	}

	boolean isActive(Implementation implementation) {
		final boolean returnValue;
		try {
			if(source instanceof Decision) {
				returnValue = invokeGuard(implementation);
			} else {
				if(implementation.getState(source.getId()) == Vertex.STATE_OCCUPIED) {
					returnValue = invokeGuard(implementation);
				} else {
					returnValue = false; // source state not occupied or missing
					// event
				}
			}
		} catch(final Exception e) {
			throw new RuntimeException("Error invoking guard", e);
		}
		return returnValue;
	}

	@SuppressWarnings("null")
	private void takeActions(Event firedEvent, Implementation implementation) {

		if(!reaction) {
			// Exit all the vertices on the exit list...
			if(exitVertex instanceof CompositeState) {
				final CompositeState exitCompositeState = (CompositeState)exitVertex;
				if(exitCompositeState.getHistoryType() != CompositeState.HISTORY_NONE) {
					exitCompositeState.suspend(this, firedEvent, implementation);
				} else {
					exitCompositeState.exit(firedEvent, implementation);
				}
			} else {
				exitVertex.exit(firedEvent, implementation);
			}
		}

		// call the actions...
		Action.invoke(actions, firedEvent, implementation);

		if(!reaction) {
			// and call the entry list
			Vertex vertex = null;
			for(int i = 0; i < entryList.length; i++) {
				vertex = entryList[i];
				if(resume && implementation.getState(vertex.getId()) == Vertex.STATE_SUSPENDED) {
					vertex.resume(firedEvent, implementation);
				} else {
					if(implementation.getState(vertex.getId()) == Vertex.STATE_SUSPENDED) {
						vertex.exit(firedEvent, implementation);
					}
					vertex.enter(firedEvent, implementation);
				}
			}

			// if the last one was a composite state special case
			// we may need to fire the initial transition
			if(!resume && (vertex instanceof CompositeState)) {
				final CompositeState toStart = (CompositeState)vertex;
				final Transition initialTransition = toStart.getInitialTransition();
				if(initialTransition == null) {
					throw new RuntimeException(
							"Attempt to resume a composite state that has never " +
									"been occupied and has no initial state in transition: " + getFullName());
				}
				// force the initial state to occupied...
				implementation.setState(toStart.getInitialState().getId(), Vertex.STATE_OCCUPIED);
				// and fire the action...
				toStart.handle(toStart.getInitialTransition().getEvent(), implementation);
			}
		}
	}

	private Vertex[] getAncestors(Vertex start, CompositeState stop, boolean reverse) {
		final List<Vertex> list = new ArrayList<>();
		Vertex currentVertex = start;
		do {
			list.add(currentVertex);
			currentVertex = currentVertex.getParent();
		} while(currentVertex != stop);

		if(reverse) {
			Collections.reverse(list);
		}

		return list.toArray(new Vertex[list.size()]);
	}

	private Method getGuard(String guardName, Class<?> implementationClass)
			throws ParseException {
		try {
			return guardName == null
					? null : ReflectionU.getMethod(implementationClass, guardName, guardArgTypes);
		} catch(final Exception e) {
			throw new ParseException("Failed to load guard: " + guardName + "()", e);
		}
	}

	boolean isReaction() {
		return reaction;
	}

}
