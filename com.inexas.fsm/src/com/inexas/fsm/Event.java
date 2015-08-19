package com.inexas.fsm;

import java.util.*;
import com.inexas.exception.InexasRuntimeException;

public class Event {
	public static enum Type {
		/** Explicit signal from outside the system */
		signal,
		/** An invocation from inside the system */
		call,
		/** A designated condition becoming true */
		change,
		/**
		 * The passage of a designated period of time From the UML spec: "An
		 * event that denotes the time elapsed since the current state was
		 * entered. See: event".
		 */
		time;
	}

	private final String name;
	private final Type type;
	private final Map<String, Object> parameters;

	Event(String name, Type type) {
		checkName(name);
		this.name = name;
		this.type = type;
		parameters = null;
	}

	/**
	 * This method is used when parameters need to be passed to an Event.
	 * Normally we reused statically defined Events but this allows us to
	 * construct an instance.
	 *
	 * @param toCopy
	 *            Event to copy
	 * @param parameters
	 *            parameters to add
	 */
	Event(Event toCopy, Map<String, Object> parameters) {
		assert parameters != null && parameters.size() > 0;
		this.name = toCopy.name;
		this.type = toCopy.type;
		this.parameters = parameters;
	}

	public Event(Event toCopy) {
		this.parameters = new HashMap<>();
		this.name = toCopy.name;
		this.type = toCopy.type;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public void setParameter(String parameterName, Object value) {
		parameters.put(parameterName, value);
	}

	public Object getParameter(String parameterName) {
		final Object result;

		if(parameters == null || !parameters.containsKey(parameterName)) {
			throw new InexasRuntimeException("Missing event parameter: " + parameterName);
		}
		result = parameters.get(parameterName);

		return result;
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ type.hashCode();
	}

	@Override
	public boolean equals(Object rhsObject) {
		final boolean returnValue;

		if(this == rhsObject) { // try for a cheap true...
			returnValue = true;
		} else if(rhsObject == null) { // try for a cheap false...
			returnValue = false;
		} else {
			// check we have the same types...
			if(rhsObject instanceof Event) {
				final Event rhs = (Event)rhsObject;
				// both LHS and RHS are the same types, check for an exact
				// match...
				returnValue = type == rhs.type && name.equals(rhs.name);
			} else {
				return false; // not the same types: false...
			}
		}
		return returnValue;
	}

	@Override
	public String toString() {
		return "Event<" + name + ", " + type.name() + ">";
	}

	private static void checkName(String toCheck) {
		if(toCheck == null ||
				toCheck.length() == 0 ||
				toCheck.trim().length() != toCheck.length()) {
			throw new RuntimeException("Invalid event name: " + toCheck);
		}
	}

}
