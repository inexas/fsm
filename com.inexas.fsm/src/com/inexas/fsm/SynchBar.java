/**
 * Copyright (C) 2005, Inexas,  All rights reserved.
 * See also Inexas Software Usage Licence http://www.inexas.com/isul/
 *
 * This software is open source but it is not necessarily free.
 *
 * The following restrictions apply to the contents of this file in
 * source or binary form including use by other packages in the form
 * of a software library or as a source for deriving packages or
 * programs:
 *
 * - Use is not permitted by military entities such as an army or other
 *   defence agencies or any entity that is significantly involved in
 *   the support through products or services of a military entity -
 *   a reasonable guide is when 30% or more of a companies revenue
 *   is received for goods or services rendered to military agencies
 *
 * - Use for commercial purposes is permitted against a licence fee.
 *   Commercial use includes production use for core business or
 *   administration but excludes evaluation of this package for
 *   eventual commercial use.
 *
 * - Use for non-commercial purposes is permitted free of charge.
 *   Non-commercial use includes:
 *
 *   - Educational bodies such as schools, universities, adult
 *     educational centers, on-line training courses (even in
 *     commercial environments)
 *
 *   - Not-for-profit organisations such as open source groups, home
 *     use, charitable organisations
 *
 * The software can be copied, modified and redistributed provided this
 * copyright notice and disclaimer is reproduced in a prominent place.
 *
 * Neither the name of the Inexas nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL INEXAS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.inexas.fsm;

import java.util.*;
import org.jdom.Element;

class SynchBar extends Vertex {
	private List<Transition> incoming = new ArrayList<>();

	SynchBar(CompositeState parent, Element element) throws ParseException {
		super(parent, element);
	}

	List<Transition> getIncoming() {
		return incoming;
	}

	void isSynched(Implementation implementation) {
		boolean isSynched = true; // be optimistic...
		for(final Transition transition : incoming) {
			if(implementation.getState(transition.getSource().getId()) != STATE_OCCUPIED) {
				isSynched = false;
				break;
			}
		}

		if(isSynched) {
			for(final Transition transition : incoming) {
				implementation.setState(getId(), STATE_EMPTY);
				transition.handle(null, implementation);
			}
			for(final Transition transition : transitions.values()) {
				// for the second and subsequent times through
				implementation.setState(getId(), STATE_OCCUPIED);
				transition.handle(null, implementation);
			}
		}
	}

	boolean fire(Event toFire, Implementation implementation) {
		// exit the incoming states...
		for(final Transition transition : incoming) {
			transition.getSource().exit(toFire, implementation);
		}

		// enter the outgoing states...
		for(final Transition transition : transitions.values()) {
			transition.getTarget().enter(toFire, implementation);
		}
		return true;
	}

	@Override
	protected void fix() throws ParseException {
		super.fix();

		// set up the incoming states...
		for(final Vertex vertex : parent.getVertices().values()) {
			for(final Transition transition : vertex.getTransitions().values()) {
				transition.fix();
				if(transition.getTarget() == this) {
					if(vertex instanceof State) {
						incoming.add(transition);
					} else {
						throw new ParseException(
								"Transitions to synch bars must be from states or " +
										"composite states: " + transition.getFullName());
					}
				}
			}
		}
		incoming = Collections.unmodifiableList(incoming);

		for(final Transition transition : transitions.values()) {
			if(transition.getEvent() != null) {
				throw new ParseException(
						"Outgoing transitions from synch bars can not have events, " +
								getFullName() + " does");
			}
		}
	}

}
