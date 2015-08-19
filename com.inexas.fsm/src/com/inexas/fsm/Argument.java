package com.inexas.fsm;

import java.lang.reflect.Method;
import org.jdom.Element;
import com.inexas.exception.*;
import com.inexas.util.ReflectionU;

/**
 * An Action argument. Arguments are provided in several data types according to
 * the type attribute in the XML specification:
 *
 * <!ELEMENT argument EMPTY> <!ATTLIST argument type
 * (event|call|int|string|bool) "string" value CDATA #REQUIRED>
 *
 * Type Value string will be interpreted as...
 * <ul>
 * <li>event the name of an event parameter, e.g. "size"</li>
 * <li>call a isSer or getter from implementation, e.g. "isBig"</li>
 * <li>int an integer value, e.g. "12321"</li>
 * <li>string text, e.g. "This is some text"</li>
 * <li>bool boolean, must be either "true" or "false"</li>
 * </ul>
 *
 * For event arguments, the type is always string. If the name is not found then
 * a runtime exception will be thrown.
 *
 * For call arguments, the type for the argument is derived from the getTer or
 * isSer (getTime(), isActive()). The name of the argument will normally be
 * lower case or camelCase.
 */
class Argument {
	public static enum Type {
		// There was a 'transition' type???
		parameter,
		call,
		integer,
		// ?todo Add double, decimal?
		string,
		bool;
	}

	private final Type type;
	private final Class<?> dataType;
	private Method getter;
	private Integer intValue;
	private String stringValue;
	private Boolean boolValue;
	private String eventParameterName;

	/**
	 * Construct an argument from the JDOM element
	 *
	 * @param element
	 * @throws ParseException
	 */
	Argument(Element element, String actionName, Class<?> implementationClass) throws ParseException {
		final String typeName = element.getAttributeValue("type");
		try {
			type = Type.valueOf(typeName);
			final String value = element.getAttributeValue("value");
			switch(type) {
			case bool:
				if("true".equals(value)) {
					boolValue = Boolean.TRUE;
				} else if("false".equals(value)) {
					boolValue = Boolean.FALSE;
				} else {
					throw new ParseException("Cannot parse boolean (valid values 'true', 'false'): "
							+ value + " in action: " + actionName);
				}
				dataType = Boolean.class;
				break;

			case call:
				if(value == null || value.length() == 0) {
					throw new RuntimeException("Zero length argument name for action: " + actionName);
				}
				try {
					getter = ReflectionU.getMethod(implementationClass, value, (Class<?>[])null);
				} catch(final SecurityException e) {
					throw new ParseException("Cannot access method: " + value
							+ " in action: " + actionName);
				} catch(final NoSuchMethodException e) {
					throw new ParseException("No method found for argument " + value +
							" in action: " + actionName);
				}
				dataType = getter.getReturnType();
				break;

			case parameter:
				eventParameterName = value;
				try {
					final String className = element.getAttributeValue("class");
					if(className == null) {
						throw new InexasRuntimeException("Argument parameter must have class: " + actionName);
					}
					dataType = Class.forName(className);
				} catch(final Exception e) {
					throw new InexasRuntimeException("Missing parameter data type: " + actionName, e);
				}
				break;

			case integer:
				try {
					intValue = new Integer(value);
				} catch(final NumberFormatException e) {
					throw new ParseException("Cannot parse integer " + value + " in action: " + actionName);
				}
				dataType = Integer.class;
				break;

			case string:
				stringValue = value;
				dataType = String.class;
				break;

			default:
				throw new UnexpectedException("Argument: " + typeName);
			}
		} catch(final Exception e) {
			throw new InexasRuntimeException("Invalid type: " + typeName, e);
		}
	}

	public Class<?> getDataType() {
		return dataType;
	}

	public Object getValue(Event event, Implementation implementation) throws ParseException {
		final Object returnValue;

		switch(type) {
		case bool:
			returnValue = boolValue;
			break;

		case call:
			try {
				returnValue = getter.invoke(implementation, (Object[])null);
			} catch(final Exception e) {
				throw new ParseException("Error invoking getter: " + getter.getName(), e);
			}
			break;

		case integer:
			returnValue = intValue;
			break;

		case string:
			returnValue = stringValue;
			break;

		case parameter:
			returnValue = event.getParameter(eventParameterName);
			break;

		default:
			throw new UnexpectedException("getValue: " + type);
		}

		return returnValue;
	}

}
