package com.inexas.fsm;

import java.lang.reflect.Method;
import java.util.*;
import org.jdom.Element;
import com.inexas.util.ReflectionU;

/**
 * This is a wrapper for entry, exit and transit actions, It takes care of all
 * the loading and firing of actions.
 */
class Action {
	private final String name;
	private final int argumentCount;
	private final Argument[] arguments;
	private final Method method;
	private final Fsm fsm;

	// C o n s t r u c t o r s

	/**
	 * Load action from a list of JDOM elements
	 *
	 * @param actionElements
	 * @return a list of at least on action or null
	 * @throws ParseException
	 */
	static Action[] loadElements(
			Class<?> implementationClass,
			Collection<?> actionElements,
			Fsm fsm) throws ParseException {
		final List<Action> actions;
		final int actionCount = actionElements.size();
		if(actionCount == 0) {
			actions = null;
		} else {
			actions = new ArrayList<>(actionCount);
			final Iterator<?> j = actionElements.iterator();
			while(j.hasNext()) {
				actions.add(new Action(implementationClass, (Element)j.next(), fsm));
			}
		}
		return (actions == null ? null : actions.toArray(new Action[actions.size()]));
	}

	static Action[] loadNames(Class<?> implementationClass, List<Element> elements, Fsm fsm)
			throws ParseException {
		final List<Action> actions;
		if(elements == null) {
			actions = null;
		} else {
			final int actionCount = elements.size();
			if(actionCount == 0) {
				actions = null;
			} else {
				actions = new ArrayList<>(actionCount);
				for(final Element element : elements) {
					actions.add(new Action(implementationClass, element, fsm));
				}
			}
		}
		return actions == null ? null : (Action[])actions.toArray(new Action[actions.size()]);
	}

	/**
	 * Construct an action given an element
	 *
	 * @param implementationClass
	 * @param element
	 * @throws ParseException
	 */
	private Action(Class<?> implementationClass, Element element, Fsm fsm) throws ParseException {
		this.fsm = fsm;
		name = element.getAttributeValue("name");
		if(name == null || name.length() == 0) {
			throw new ParseException("Null or zero length action name");
		}

		// Arguments...
		// todo extend <?>
		final List<?> argumentElements = element.getChildren("argument");
		argumentCount = argumentElements.size();
		arguments = new Argument[argumentCount];
		final Class<?> actionArgTypes[] = new Class[argumentCount];
		int argumentIndex = 0;
		// todo extend <?>
		final Iterator<?> i = argumentElements.iterator();
		while(i.hasNext()) {
			final Element argumentElement = (Element)i.next();
			final Argument argument = new Argument(argumentElement, name, implementationClass);
			arguments[argumentIndex] = argument;
			actionArgTypes[argumentIndex++] = argument.getDataType();
		}

		// now get the action method...
		try {
			method = ReflectionU.getMethod(implementationClass, name, actionArgTypes);
		} catch(final Exception e) {
			throw new ParseException("Error loading action method", e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Action: void ");
		sb.append(name);
		sb.append('(');
		String comma = "";
		for(int i = 0; i < arguments.length; i++) {
			sb.append(comma);
			comma = ", ";
			sb.append(arguments[i].getDataType().getName());
		}
		sb.append(')');
		return sb.toString();
	}

	String getName() {
		return name;
	}

	Method getMethod() {
		return method;
	}

	static void invoke(Action actions[], Event event, Implementation implementation) {
		if(actions != null) {
			for(int i = 0; i < actions.length; i++) {
				actions[i].invoke(event, implementation);
			}
		}
	}

	Object invoke(Event event, Implementation implementation) {
		final Object returnValue;
		try {
			final Object args[] = new Object[argumentCount];
			for(int i = 0; i < argumentCount; i++) {
				args[i] = arguments[i].getValue(event, implementation);
			}
			if(fsm.isLogging()) {
				fsm.logInfo("Fire action: " + name);
			}
			returnValue = method.invoke(implementation, args);
		} catch(final Exception e) {
			throw new RuntimeException("Error invoking action: " + name, e);
		}
		return returnValue;
	}

}
