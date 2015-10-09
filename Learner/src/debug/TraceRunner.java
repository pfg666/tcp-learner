package debug;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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

import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;

import learner.Config;
import learner.EquivalenceOracle;
import learner.Main;
import learner.MembershipOracle;
import learner.SutInterface;
import learner.TCPParams;
import sutInterface.ObservationTreeWrapper;
import sutInterface.SutWrapper;
import sutInterface.tcp.InvlangMapper;
import sutInterface.tcp.MapperSutWrapper;
import sutInterface.tcp.TCPMapper;
import sutInterface.tcp.TCPSutWrapper;
import sutInterface.tcp.init.CachedInitOracle;
import sutInterface.tcp.init.FunctionalInitOracle;
import sutInterface.tcp.init.InitCacheManager;
import sutInterface.tcp.init.InitOracle;
import sutInterface.tcp.init.InvCheckOracleWrapper;
import sutInterface.tcp.init.LogOracleWrapper;
import util.InputAction;
import util.Log;
import util.NullStream;
import util.ObservationTree;
import util.OutputAction;
import util.Tuple2;

public class TraceRunner {
	private static final String PATH = "testtrace.txt";
	
	public static final String START = 		"\n****** INPUTS  ******\n";
	public static final String SEPARATOR = 	"\n****** OUTPUTS ******\n";
	public static final String END = 		"\n*********************\n";
	
	private Map<List<String>, Integer> outcomes = new HashMap<List<String>, Integer>();
	private final MapperSutWrapper sutWrapper;
	private final List<InputAction> inputTrace;
	//private final CacheInputValidator validator;
	
	public static void main(String[] args) throws IOException {
		Main.handleArgs(args);
		List<String> trace;
		try {
			trace = Files.readAllLines(Paths.get(PATH), StandardCharsets.US_ASCII);
		} catch (IOException e) {
			System.out.println("usage of java tracerunner: create a file '" + PATH + "' with the input on each line', optionally preceded by the number of times the input should be repeated");
			return;
		}
		ListIterator<String> it = trace.listIterator();
		System.out.println("TRACE FILE: ");
		int i = 1;
		while(it.hasNext()) {
			String line = it.next();
			System.out.print((i++) + ": " + line);
			if (line.startsWith("#") || line.startsWith("!")) {
				it.remove();
				System.out.println(" (skipped)");
			} else {
				 if ( line.isEmpty()) {
					 it.remove();
					 while (it.hasNext()) {
						 it.next();
						 it.remove();
					 } 
				 } else {
					 System.out.println();
				 }
			}
		}
		int iterations;
		try {
			iterations = Integer.parseInt(trace.get(0));
			trace.remove(0);
		} catch (NumberFormatException e) {
			iterations = 1;
		}
		Log.fatal("Start running trace " + iterations + " times");

		Main.setupOutput("trace runner output.txt");
		Config config = Main.createConfig();

		SutInterface sutInterface = Main.createSutInterface(config);
	
		TCPParams tcp = Main.readConfig(config, sutInterface);
		tcp.exitIfInvalid = false;
		
		/*InitOracle initOracle;
		// in a normal init-oracle ("functional") TCP setup, we use the conventional eq/mem oracles
		if(! "adaptive".equalsIgnoreCase(tcp.oracle)) {
			initOracle = new FunctionalInitOracle();
		} else {
			initOracle = new CachedInitOracle(new InitCacheManager());
		}
		TCPMapper tcpMapper = new TCPMapper(initOracle);*/
		//TCPSutWrapper sutWrapper = new TCPSutWrapper(tcp.sutPort, tcpMapper, tcp.exitIfInvalid);
		
		MapperSutWrapper sutWrapper = new MapperSutWrapper(tcp.sutPort, Main.learningParams.mapper);
		TraceRunner traceRunner = new TraceRunner(trace, sutWrapper);
		traceRunner.testTrace(iterations);
		sutWrapper.close();
		System.out.println(traceRunner.getResults());
	}
	
	public String getResults() {
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
	
	public TraceRunner(Word word, MapperSutWrapper sutWrapper) {
		List<InputAction> inputActions = new ArrayList<>(word.size());
		for (Symbol symbol : word.getSymbolList()) {
			inputActions.add(new InputAction(symbol.toString()));
		}
		this.inputTrace = inputActions;
		this.sutWrapper = sutWrapper;
	}
	
	public TraceRunner(List<String> inputTrace, MapperSutWrapper sutWrapper) {
		List<InputAction> inputActions = new ArrayList<>(inputTrace.size());
		for (String s : inputTrace) {
			inputActions.add(new InputAction(s.trim()));
		}
		this.inputTrace = inputActions;
		this.sutWrapper = sutWrapper;
	}
	
	public void testTrace(int iterations) {
		for (int i = 0; i < iterations; i++) {
			runTrace((i+1));
		}
	}
	
	protected void runTrace(int printNumber) {
		List<String> outcome = new LinkedList<String>();
		sutWrapper.sendReset();
		System.out.println("# " + printNumber);
		//System.out.println("# " + number + " @@@ " + this.sutWrapper.toString());
		for (InputAction input : inputTrace) {
			OutputAction output;
			if (input.getMethodName().equals("reset")) {
				this.sutWrapper.sendReset();
				output = new OutputAction("RESET");
			} else {
				output = this.sutWrapper.sendInput(input);
			}
			//System.out.println("# " + number + " >>> " + input + " >>> " + output);
			//System.out.println("# " + number + " @@@ " + this.sutWrapper.toString());
			outcome.add(output.toString());
		}
		Integer currentCounter = outcomes.get(outcome);
		if (currentCounter == null) {
			currentCounter = 0;
		}
		outcomes.put(outcome, currentCounter + 1);
	}
}
