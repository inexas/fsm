package com.inexas.fsm;

import java.io.*;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * Finite State Machine factory
 */
public class FsmFactory {
	private final Fsm fsm;
	private final String name;

	/**
	 * Construct an FSM factory from an input stream
	 *
	 * @param inputStream
	 *            the input stream to use
	 * @param filename
	 *            the file name used for error messages
	 * @throws ParseException
	 */
	public FsmFactory(InputStream inputStream, String filename) throws ParseException {
		try {
			final SAXBuilder builder = new SAXBuilder(true);
			final Element element = builder.build(inputStream).getRootElement();
			name = element.getAttributeValue("name");
			fsm = new Fsm(element);
		} catch(final Exception e) {
			throw new ParseException("Error loading file: " + filename, e);
		}
	}

	/**
	 * Construct an FSM factory given the name of the file that contains the XML
	 * specification.
	 *
	 * @param fileName
	 *            the name of the file
	 * @throws ParseException
	 *             if an error is encountered
	 */
	public FsmFactory(String fileName) throws ParseException {
		this(new File(fileName));
	}

	/**
	 * Construct an FSM factory
	 *
	 * @param xmlFile
	 *            the XML file to use
	 * @throws ParseException
	 *             if an error is encountered
	 */
	public FsmFactory(File xmlFile) throws ParseException {
		try {
			if(!xmlFile.exists()) {
				throw new RuntimeException("File does not exist: " + xmlFile.getName());
			}
			final SAXBuilder builder = new SAXBuilder(true);
			final Element element = builder.build(xmlFile).getRootElement();
			name = element.getAttributeValue("name");
			fsm = new Fsm(element);
		} catch(final Exception e) {
			throw new ParseException("Error loading file: " + xmlFile.getAbsolutePath(), e);
		}
	}

	public FsmFactory(Element element, String filename) throws ParseException {
		this.name = filename;
		fsm = new Fsm(element);
	}

	public Fsm newInstance(Implementation implementation) {
		final Class<?> c1 = fsm.getImplementationClass();
		final Class<?> c2 = implementation.getClass();
		if(!c1.isAssignableFrom(c2)) {
			throw new RuntimeException(
					"Invalid implementation class: " + c2.getName() +
					"; must be instance of " + c1.getName());
		}
		return new Fsm(fsm, implementation);
	}

	public int stateCount() {
		return fsm.size();
	}

	@Override
	public String toString() {
		return "FSM factory::" + name;
	}

	public String getName() {
		return name;
	}

	public Event getEvent(String eventName) {
		return fsm.getEvent(eventName);
	}
}