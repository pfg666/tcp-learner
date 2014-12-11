package util;

import java.io.PrintStream;

public class Log {
	enum Level {
		INFO,
		WARN,
		FATAL,
		ERROR
	}
	
	private static PrintStream activePrintStream = null;
	
	public static void setActivePrintStream(PrintStream printStream) {
		activePrintStream = printStream;
	}
	
	public static void warn(String message) {
		log(Level.WARN, message);
	}
	
	public static void info(String message) {
		log(Level.INFO, message);
	}
	
	public static void fatal(String message) {
		log(Level.FATAL, message);
	}

	public static void err(String message) {
		log(Level.ERROR, message);
	}
	
	private static void log(Level level, String message) {
		if(activePrintStream != null)
			log(level, message, activePrintStream);
		log(level, message, System.out);
	}
	
	/** Logs message prepending location of log invocation. To retrieve the location from which the log was called, 
	 * it navigates through the stack until it gets outside of the Log/Exception classes. */ 
	private static void log(Level level, String message, PrintStream writer) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement relevantStackTraceElement = null;
		for(StackTraceElement element : stackTrace) {
			// nifty hardcoded way to get the correct stacktrace
			if(!element.getMethodName().equals("getStackTrace") && !element.getClassName().contains("Log") && !element.getClassName().contains("Err")) {
				relevantStackTraceElement = element;
				break;
			}
		}
		writer.println(level.name()+" (" + relevantStackTraceElement.getClassName() + ";" + relevantStackTraceElement.getMethodName() + ";" + relevantStackTraceElement.getLineNumber() + "): "+ message);
		writer.flush();
	}
}
