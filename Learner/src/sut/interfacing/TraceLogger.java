package sut.interfacing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TraceLogger {
	public static final String TRACE_DIRECTORY = "traceFiles";
	public static final String REGULAR_TRACES_FILE = TRACE_DIRECTORY+File.separator+"regularTraces.txt";
	public static final String CEX_TRACES_FILE = TRACE_DIRECTORY+File.separator+"cexTraces.txt";
	public static final String INTERESTING_TRACES_FILE = TRACE_DIRECTORY+File.separator+"interestingTraces.txt";
	public static final String DELIM = "=========Trace %d=========\n";
	
	public long traceIndex = 1;
	
	private List<String> concreteMessages = new ArrayList<String>();
	private List<String> abstractMessages = new ArrayList<String>();
	
	public void addConcrete(String input) {
		this.concreteMessages.add(input);
	}

	public void addAbstract(String input) {
		this.abstractMessages.add(input);
	}

	public void reset() {
		this.concreteMessages.clear();
		this.abstractMessages.clear();
		traceIndex ++;
	}

	public void logTrace(String filePath) {
		List<String> allTraces = new ArrayList<String>(this.concreteMessages);	
		allTraces.add("\n");
		allTraces.addAll(this.abstractMessages);
		appendMessages(filePath, allTraces);
	}
	
	public void clearTraceFile(String filePath) {
		File traceFile = new File(filePath);
		if(traceFile.exists() && traceFile.isFile()) {
			traceFile.delete();
		}
	}
	
	private void appendMessages(String fileName, List<String> messages) {
		try {
			FileWriter fw = new FileWriter(fileName, true);
			fw.append(String.format(DELIM, traceIndex));
			for (String message : messages) {
				fw.append(message + "\n");
			}
			fw.append("\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		TraceLogger logger = new TraceLogger();
		logger.addAbstract("ABS");
		logger.addAbstract("CONC");
		logger.logTrace(INTERESTING_TRACES_FILE);
		System.out.println(TraceLogger.INTERESTING_TRACES_FILE);
	}
	
}
