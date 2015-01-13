package debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import learner.Config;
import learner.Main;
import learner.SutInterface;
import learner.TCPParams;
import sutInterface.SutWrapper;
import sutInterface.tcp.TCPMapper;
import sutInterface.tcp.TCPSutWrapper;
import sutInterface.tcp.init.CachedInitOracle;
import sutInterface.tcp.init.FunctionInitOracle;
import sutInterface.tcp.init.InitCacheManager;
import sutInterface.tcp.init.InitOracle;
import util.InputAction;
import util.Log;
import util.OutputAction;

public class TraceRunner {
	public static final String START = 		"\n****** INPUTS  ******\n";
	public static final String SEPARATOR = 	"\n****** OUTPUTS ******\n";
	public static final String END = 		"\n*********************\n";
	
	private Map<List<String>, Integer> outcomes = new HashMap<>();
	private final SutWrapper sutWrapper;
	private final List<InputAction> inputTrace;
	//private final CacheInputValidator validator;
	
	public static void main(String[] args) throws IOException {
		Main.handleArgs(args);
		List<String> trace;
		try {
			trace = Files.readAllLines(Paths.get("testtrace.txt"), StandardCharsets.US_ASCII);
		} catch (IOException e) {
			System.out.println("usage of java tracerunner: create a file 'testtrace.txt with the input on each line'");
			return;
		}
		ListIterator<String> it = trace.listIterator();
		while(it.hasNext()) {
			String line = it.next();
			if (line.startsWith("#") || line.isEmpty()) {
				it.remove();
			}
		}
		int iterations;
		try {
			iterations = Integer.parseInt(trace.get(0));
			trace.remove(0);
		} catch (NumberFormatException e) {
			iterations = 1;
		}
		Log.fatal("Start running trace");

		Config config = Main.createConfig();

		SutInterface sutInterface = Main.createSutInterface(config);
	
		TCPParams tcp = Main.readConfig(config, sutInterface);
		tcp.exitIfInvalid = false;
		
		InitOracle initOracle;
		// in a normal init-oracle ("functional") TCP setup, we use the conventional eq/mem oracles
		if(! "adaptive".equalsIgnoreCase(tcp.oracle)) {
			initOracle = new FunctionInitOracle();
		} else {
			initOracle = new CachedInitOracle(new InitCacheManager());
		}
		TCPMapper tcpMapper = new TCPMapper(initOracle);
		TCPSutWrapper sutWrapper = new TCPSutWrapper(tcp.sutPort, tcpMapper, tcp.exitIfInvalid);
		sutWrapper.setExitOnInvalidParameter(false);
		TraceRunner traceRunner = new TraceRunner(trace, sutWrapper);
		for (int i = 0; i < iterations; i++) {
			traceRunner.runTrace();
		}
		sutWrapper.close();
		System.out.println(traceRunner.results());
	}
	
	private String results() {
		StringBuilder sb = new StringBuilder();
		sb.append(START);
		sb.append("input:" + this.inputTrace);
		sb.append(SEPARATOR);
		List<Entry<List<String>, Integer>> orderedEntries = new ArrayList<>(this.outcomes.size());
		for (Entry<List<String>, Integer> entry : this.outcomes.entrySet()) {
			orderedEntries.add(entry);
		}
		Collections.sort(orderedEntries, new Comparator<Entry<List<String>, Integer>>() {
			@Override
			public int compare(Entry<List<String>, Integer> arg0,
					Entry<List<String>, Integer> arg1) {
				return Integer.compare(arg0.getValue(), arg1.getValue());
			}
		});
		for (Entry<List<String>, Integer> entry : orderedEntries) {
			sb.append(entry.getValue().toString() + ": " + entry.getKey() + "\n");
		}
		sb.append(END);
		return sb.toString();
	}

	public TraceRunner(List<String> inputTrace, TCPSutWrapper sutWrapper) {
		List<InputAction> inputActions = new ArrayList<>(inputTrace.size());
		for (String s : inputTrace) {
			inputActions.add(new InputAction(s));
		}
		this.inputTrace = inputActions;
		this.sutWrapper = sutWrapper;
	}
	
	protected void runTrace() {
		List<String> outcome = new LinkedList<String>();
		sutWrapper.sendReset();
		for (InputAction input : inputTrace) {
			OutputAction output;
			output = this.sutWrapper.sendInput(input);
			System.out.println(" >>> " + input + " >>> " + output);
			outcome.add(output.toString());
		}
		Integer currentCounter = outcomes.get(outcome);
		if (currentCounter == null) {
			currentCounter = 0;
		}
		outcomes.put(outcome, currentCounter + 1);
	}
}
