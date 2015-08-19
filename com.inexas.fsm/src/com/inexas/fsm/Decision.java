package com.inexas.fsm;

import java.lang.reflect.Method;
import java.util.*;
import org.jdom.Element;
import com.inexas.exception.InexasRuntimeException;
import com.inexas.util.*;

public class Decision extends Vertex {
	private static final Object queryArgs[] = new Object[0];
	private static final Class<?> queryArgTypes[] = new Class[0];
	private final StrictMap<String, Transition> cases = new StrictMap<>();
	private final Transition defaultTransition;
	private final Method query;

	Decision(CompositeState parent, Element element) throws ParseException {
		super(parent, element);

		// <!ELEMENT decision (case*,default?)>
		// <!ATTLIST decision
		// name CDATA #REQUIRED
		// query CDATA #REQUIRED>
		//
		// <!ELEMENT query EMPTY>
		// <!ATTLIST query
		// class-name CDATA #REQUIRED>
		//
		// <!ELEMENT case (transition)>
		// <!ATTLIST case
		// name CDATA #REQUIRED>
		//
		// <!ELEMENT default (transition)>

		// query...
		query = getQuery(element.getAttributeValue("query"));

		// cases...
		// todo remove <?>
		final Iterator<?> i = element.getChildren("case").iterator();
		while(i.hasNext()) {
			final Element caseElement = (Element)i.next();
			final Element transitionElement = caseElement.getChild("transition");
			final String caseName = caseElement.getAttributeValue("name");
			final Transition transition = newTransition(transitionElement, null, null);
			cases.put(caseName, transition);
		}

		// default...
		final Element defaultElement = element.getChild("default");
		if(defaultElement != null) {
			defaultTransition = newTransition(defaultElement.getChild("transition"), null, null);
		} else {
			defaultTransition = null;
		}
	}

	Transition query(Implementation implementation) {

		// Invoke the query...
		final String result;
		try {
			result = query.invoke(implementation, queryArgs).toString();
			if(fsm.isLogging()) {
				fsm.logInfo("Query " + query.getName() + " returned " + result);
			}
		} catch(final Exception e) {
			throw new RuntimeException("Error invoking query: " + query.getName(), e);
		}

		// Figure out the which transition, if any, to take...
		Transition returnValue;
		returnValue = cases.get(result == null ? "null" : result);
		if(returnValue == null) {
			if(defaultTransition == null) {
				throw new InexasRuntimeException(
						"Unrecognised value and no default in decision: " + returnValue);
			}
			returnValue = defaultTransition;
		}

		// check any guard the transition might have...
		if(!returnValue.isActive(implementation)) {
			returnValue = null;
		}

		return returnValue;
	}

	public Transition getDefaultTransition() {
		return defaultTransition;
	}

	@Override
	void fix() throws ParseException {
		super.fix();
		cases.setGetCanReturnNull(true);
		cases.lock();
		// the contents of cases have already fixed by the
		// super.fix() call above, ditto defaultTransition
	}

	Map<String, Transition> getCases() {
		return cases;
	}

	Method getQuery() {
		return query;
	}

	private Method getQuery(String queryName) throws ParseException {
		try {
			return queryName == null
					? null : ReflectionU.getMethod(implementationClass, queryName, queryArgTypes);
		} catch(final Exception e) {
			throw new ParseException("Failed to find query: " +
					implementationClass.getName() + "." + queryName + "()", e);
		}
	}
}
