package com.inexas.util.logging;

import java.util.logging.*;

/**
 * I could not figure out how to configure java.logging on OX X so I had to
 * write this wrapper
 */
public class InexasLogger {
	private static Logger logger;

	public static void initialize(Class<?> clazz) {
		if(logger == null) {
			try {
				logger = Logger.getLogger(clazz.getName());
				logger.setUseParentHandlers(false);

				// Set up the console handler...
				final Handler handler = new ConsoleHandler();
				handler.setFormatter(new InexasFormatter());
				logger.addHandler(handler);

				// Set up the file handler...
				final String packageName = clazz.getName();
				final int lastDot = packageName.lastIndexOf('.');
				final String fileName = lastDot == -1 ? packageName : packageName.substring(lastDot + 1);
				// final String logNamePattern = "logs/" + fileName + ".%g" +
				// ".log";
				final String logNamePattern = "/tmp/" + fileName + ".%g" + ".log";
				final FileHandler fileHandler = new FileHandler(logNamePattern, 10 * 1000, 10);
				fileHandler.setFormatter(new InexasFormatter());
				logger.addHandler(fileHandler);

			} catch(final Exception e) {
				throw new RuntimeException("Error opening logger", e);
			}
		}
	}

	private InexasLogger() {
		// Hide
	}

	public static void severe(String message) {
		assert logger != null : "Initialse the logger first: Inexas.initialize(Class)";
		logger.severe(message);
	}

	public static void warning(String message) {
		assert logger != null : "Initialse the logger first: Inexas.initialize(Class)";
		logger.warning(message);
	}

	public static void info(String message) {
		assert logger != null : "Initialse the logger first: Inexas.initialize(Class)";
		logger.info(message);
	}

}
