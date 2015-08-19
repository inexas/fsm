package com.inexas.fsm;

import org.jdom.Element;

public class FinalState extends State {

	FinalState(Fsm fsm, Element finalStateElement) throws ParseException {
		super(fsm, finalStateElement);

		if(!fsm.canHaveTerminalActions()) {
			if(entryActions != null) {
				throw new ParseException(
						"final-state cannot have actions, " +
								"see: fsm/terminal-actions in the root element in your XML FMS definition");
			}
		}
	}

}
