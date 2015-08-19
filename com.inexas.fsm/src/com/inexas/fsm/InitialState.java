package com.inexas.fsm;

import org.jdom.Element;

public class InitialState extends State {

	/**
	 * Used for initial, and history states
	 *
	 * @param parent
	 * @param element
	 * @throws ParseException
	 */
	InitialState(CompositeState parent, Element element) throws ParseException {
		super(parent, element);

		if(!fsm.canHaveTerminalActions()) {
			if(exitActions != null) {
				throw new ParseException(
						"initial-state cannot have actions, " +
								"see: fsm/terminal-actions in the root element in your XML FMS definition");
			}
		}

		// Must have one transition...
		if(transitions.size() == 0) {
			newTransition(element.getChild("transition"), null, Fsm.START_NAME);
		}
	}

	@Override
	void reset(Implementation implementation) {
		// no need to call super
		implementation.setState(getId(), STATE_OCCUPIED);
	}

}
