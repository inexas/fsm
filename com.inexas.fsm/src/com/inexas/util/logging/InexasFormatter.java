package com.inexas.util.logging;

import java.util.Date;
import java.util.logging.*;
import com.inexas.util.DateU;

/**
 * Better logging
 * <ul>
 * <li>Compact</li>
 * <li>First part is aligned</li>
 * <li>Eclipse clickable</li>
 * </ul
 */
public class InexasFormatter extends Formatter {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Override
	public String format(LogRecord record) {
		final StringBuilder sb = new StringBuilder();

		// Logger name, last 8 characters, padded to 4 characters...
		final String loggerName = record.getLoggerName() + "        ";
		sb.append(loggerName.substring(0, 8));
		sb.append(' ');

		// INF, DEB, ERR, SEV...
		sb.append(record.getLevel().toString().subSequence(0, 3));
		sb.append(' ');

		// Date time 2014/10/23 11:22:12...
		final Date date = new Date(record.getMillis());
		sb.append(DateU.formatDateTimeStandard(date));
		sb.append(' ');

		// The message...
		sb.append(record.getMessage());

		// File name, line number [abc.x.java:21]...
		// Note there was a space between the square brackets and the 'at',
		// maybe this
		// is needed for Eclipse?
		sb.append(" [at ");
		final String sourceClassName = record.getSourceClassName();
		sb.append(sourceClassName);
		sb.append('.');
		sb.append(record.getSourceMethodName());
		sb.append('(');
		sb.append(sourceClassName.substring(sourceClassName.lastIndexOf('.') + 1));
		// Need a line number here for Eclipse to parse the message into a link
		// but the record doesn't contain a real one so use anything (i.e. :1)
		sb.append(".java:1)]");
		sb.append(LINE_SEPARATOR);
		return sb.toString();
	}
}
