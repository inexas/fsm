package com.inexas.fsm;

import org.jdom.Element;

class ActivityState extends Vertex {

	ActivityState(CompositeState parent, Element element) throws ParseException {
		super(parent, element);

		// <!ELEMENT activity (entry-action*,transition*,exit-action*)>
		// <!ATTLIST activity
		// name CDATA #REQUIRED>

		// the element is completely loaded by the super classes
	}
}
