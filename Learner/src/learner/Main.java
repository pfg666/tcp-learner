package learner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.sound.midi.SysexMessage;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.CacheOracle;
import sutInterface.CacheReaderOracle;
import sutInterface.ObservationTreeWrapper;
import sutInterface.SutInfo;
import sutInterface.SutWrapper;
import sutInterface.tcp.MapperSutWrapper;
import sutInterface.tcp.LearnResult;
import sutInterface.tcp.init.InvCheckOracleWrapper;
import sutInterface.tcp.init.LogOracleWrapper;
import util.FileManager;
import util.Log;
import util.ObservationTree;
import util.SoundUtils;
import util.Tuple2;
import util.exceptions.NonDeterminismException;
import util.learnlib.Dot;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.algorithms.packs.ObservationPack;
import de.ls5.jlearn.equivalenceoracles.RandomWalkEquivalenceOracle;
import de.ls5.jlearn.exceptions.ObservationConflictException;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Learner;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.logging.LearnLog;
import de.ls5.jlearn.logging.LogLevel;
import de.ls5.jlearn.logging.PrintStreamLoggingAppender;
import de.ls5.jlearn.shared.WordImpl;
import de.ls5.jlearn.util.DotUtil;
import debug.TraceRunner;

public class Main {
	public static final String CACHE_FILE = "cache.ser";
	
	private static File sutConfigFile = null;
	public static LearningParams learningParams;
	private static final long timeSnap = System.currentTimeMillis();
	public static final String outputDir = "output" + File.separator + timeSnap;
	private static File outputFolder;
	public static PrintStream learnOut;
	public static PrintStream absTraceOut, absAndConcTraceOut;
	public static PrintStream stdOut = System.out;
	public static PrintStream errOut = System.err;
	public static PrintStream statsOut;
	private static boolean done;
	public static Config config;
	private static File sutInterfaceFile;
	private static ObservationTree tree;
	private static MapperSutWrapper sutWrapper;

    private static PrintStream dupStdout;

	public static void main(String[] args) throws LearningException, IOException, Exception {
		handleArgs(args);
		
		setupOutput(outputDir);

		Config config = createConfig();
		Main.config = config;
		SutInterface sutInterface = createSutInterface(config);
	
		TCPParams tcp = readConfig(config, sutInterface);
		
		Log.setLogLevel(tcp.logLevel);

		// first is the membership, second is the equivalence oracle
		Tuple2<Oracle,Oracle> tcpOracles = buildOraclesFromConfig(tcp);
		
		Learner learner;

		LearnResult learnResult;

		de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle = buildEquivalenceOracle(learningParams, tcpOracles.tuple1);
		SingleTransitionReducer ceReducer = new SingleTransitionReducer(tcpOracles.tuple1);

		learner = new ObservationPack();
		//learner = new Angluin();
		learner.setOracle(tcpOracles.tuple0);

		learner.setAlphabet(SutInfo.generateInputAlphabet());
		SutInfo.generateOutputAlphabet();
		
		learnResult = learn(learner, eqOracle, ceReducer);
		

		// final output to out.txt
		absTraceOut.println("Seed: " + learningParams.seed);
		errOut.println("Seed: " + learningParams.seed);
		absTraceOut.println("Done.");
		errOut.println("Successful run.");

		// output needed for equivalence checking
		// - learnresult.dot : learned state machine
		// - output.json : abstraction,concrete alphabet, start state
		State startState = learnResult.learnedModel.getStart();

		statsOut
				.println("Total states in learned abstract Mealy machine: "
						+ learnResult.learnedModel.getAllStates().size());
		
		Statistics.getStats().printStats(statsOut);

		// output learned model with start state highlighted to dot file :
		// notes:
		// - make start state the only highlighted state in dot file
		// - learnlib makes highlighted state by setting attribute color='red'
		// on state

		LinkedList<State> highlights = new LinkedList<State>();
		highlights.add(startState);
		BufferedWriter out = null;
		
		writeOutputFiles(learnResult, highlights, out);

		errOut.println("Learner Finished!");

		// bips to notify that learning is done :)
		try {
			SoundUtils.success();
		} catch (Exception e) {
			
		}
	}
	
	private static void copyInputsToOutputFolder() {
		File inputFolder = sutConfigFile.getParentFile();
		Path srcInputPath = inputFolder.toPath();
		Path dstInputPath = outputFolder.toPath().resolve(inputFolder.getName()); // or resolve("input")
		Path srcTcpPath = Paths.get(System.getProperty("user.dir")).resolve("Learner").resolve("src").resolve("sutInterface").resolve("tcp");
		Path dstTcpPath = outputFolder.toPath().resolve("tcp");
		try {
			FileManager.copyFromTo(srcInputPath, dstInputPath);
			FileManager.copyFromTo(srcTcpPath, dstTcpPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeOutputFiles(LearnResult learnResult,
			LinkedList<State> highlights, BufferedWriter out) {
		// output learned state machine as dot and pdf file :
		//File outputFolder = new File(outputDir + File.separator + learnResult.startTime);
		//outputFolder.mkdirs();
		File dotFile = new File(outputFolder.getAbsolutePath() + File.separator + "learnresult.dot");
		File pdfFile = new File(outputFolder.getAbsolutePath() + File.separator + "learnresult.pdf");
		
		try {
			out = new BufferedWriter(new FileWriter(dotFile));

			DotUtil.writeDot(learnResult.learnedModel, out, learnResult.learnedModel.getAlphabet()
					.size(), highlights, "");
		} catch (IOException ex) {
			System.out.println(ex);
			// Logger.getLogger(DotUtil.class.getName()).log(Level.SEVERE, null,
			// ex);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				out.close();
				statsOut.close();
			} catch (IOException ex) {
				// Logger.getLogger(DotUtil.class.getName()).log(Level.SEVERE,
				// null, ex);
			}
		}

		// write pdf
		DotUtil.invokeDot(dotFile, "pdf", pdfFile);
	}
	
	
	public static void setupOutput(final String outputDir) throws FileNotFoundException {
		outputFolder = new File(outputDir);
		outputFolder.mkdirs();
		absTraceOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "tcpLog.txt", false));
		absAndConcTraceOut = new PrintStream(
						new FileOutputStream(outputDir + File.separator + "tcpTrace.txt", false));
		absAndConcTraceOut.println("copy this to obtain the regex describing any text between two inputs of a trace:\n[^\\r\\n]*[\\r\\n][^\\r\\n]*[\\r\\n][^\\r\\n]*[\\r\\n][^\\r\\n]*[\\r\\n]\n\n");
		learnOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "learnLog.txt", false));
		dupStdout =  new PrintStream(
                new FileOutputStream(outputDir + File.separator + "stdout.txt", false));
	//	Log.setActivePrintStream(dupStdout);
		
		errOut = System.err;
		
		statsOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "statistics.txt", false));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				closeOutputStreams();
				copyInputsToOutputFolder();
				writeCacheTree(tree, true);

				//InitCacheManager mgr = new InitCacheManager();
				//mgr.dump(outputDir + File.separator +  "cache.txt"); 
				if (done == false) {
					SoundUtils.failure();
				}
			}
		});
	}

	public static LearnResult learn(Learner learner,
			de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle, SingleTransitionReducer ceReducer)
			throws LearningException, ObservationConflictException, IOException {
		LearnResult learnResult = new LearnResult();
		Statistics stats = Statistics.getStats();
		stats.startTime = System.currentTimeMillis();
		long starttmp = stats.startTime;
		int hypCounter = 1;
		long endtmp;
		done = false;

		Log.fatal("Start Learning");
		absTraceOut.println("starting learning\n");
		//try {
			while (!done) {
				absTraceOut.println("		RUN NUMBER: " + ++stats.runs);
				absTraceOut.println("");
				absTraceOut.flush();
				errOut.flush();

				try {
					// execute membership queries
					learner.learn();
					absTraceOut.flush();
					errOut.flush();
					absTraceOut.println("done learning");
					endtmp = System.currentTimeMillis();
					statsOut
							.println("Running time of membership queries: "
									+ (endtmp - starttmp) + "ms.");
					stats.totalTimeMemQueries += endtmp - starttmp;
					starttmp = System.currentTimeMillis();
					absTraceOut.flush();
	
					// stable hypothesis after membership queries
					Automaton hyp = learner.getResult();
					String hypFileName = outputDir + File.separator + "tmp-learnresult"
							+ hypCounter + ".dot";
					String hypPdfFileName = outputDir + File.separator + "tmp-learnresult"
							+ hypCounter + ".pdf";
					
					File hypPDF = new File(hypPdfFileName);
					Dot.writeDotFile(hyp, hypFileName );
					DotUtil.invokeDot(hypFileName, "pdf", hypPDF);
	
					absTraceOut.println("starting equivalence query");
					absTraceOut.flush();
					errOut.flush();
					// search for counterexample
					EquivalenceOracleOutput o = eqOracle
							.findCounterExample(hyp);
					
					absTraceOut.flush();
					errOut.flush();
					absTraceOut.println("done equivalence query");
					endtmp = System.currentTimeMillis();
					stats.totalTimeEquivQueries += endtmp - starttmp;
					starttmp = System.currentTimeMillis();
	
					// no counter example -> learning is done
					if (o == null) {
						done = true;
						continue;
					} 
					o = ceReducer.reducedCounterexample(o, hyp);
					
					logCounterExampleAnalysis(hyp, hypCounter, o);
					hypCounter ++;
					absTraceOut.println("Sending CE to LearnLib.");
					absTraceOut.println("Counter Example: "
							+ o.getCounterExample().toString());
					absTraceOut.flush();
					errOut.flush();
					// return counter example to the learner, so that it can use
					// it to generate new membership queries
					learner.addCounterExample(o.getCounterExample(),
							o.getOracleOutput());
					absTraceOut.flush();
					errOut.flush();
				} catch (NonDeterminismException e) {
					int testIterations = config.learningParams.nonDeterminismTestIterations;
					if (testIterations > 0) {
						TraceRunner traceRunner = new TraceRunner(e.getInputs(), sutWrapper);
						traceRunner.testTrace(testIterations);
						System.err.println(traceRunner.getResults());
					}
					Log.err(e.getMessage() + "\n" + e.getStackTrace());
					throw e;
				}
			}
		stats.endTime = System.currentTimeMillis();
		learnResult.learnedModel = learner.getResult();
		return learnResult;
	}
	
	private static void logCounterExampleAnalysis(Automaton hyp, int hypCounter, EquivalenceOracleOutput o) throws IOException {
		PrintStream out = new PrintStream( new FileOutputStream(outputDir + File.separator +"cexanalysis.txt", true));
		Word ceInputWord = o.getCounterExample();
		Word oracleOutputWord = o.getOracleOutput();
		List<Symbol> ceInputSymbols = ceInputWord.getSymbolList();
		List<Symbol> sutOutput = oracleOutputWord.getSymbolList();
		List<Symbol> inputSymbols = new ArrayList<Symbol>();
		List<Symbol> hypOutput = hyp.getTraceOutput(ceInputWord).getSymbolList();
		out.print("\n Counterexample for hyp"+hypCounter +"\n");
		
		for (int i = 0; i < ceInputSymbols.size(); i++) {
			inputSymbols.add(ceInputSymbols.get(i));
			Word inputWord = new WordImpl((Symbol[]) inputSymbols.toArray(new Symbol[inputSymbols.size()]));
			out.println(ceInputSymbols.get(i));
			out.println("!" +hypOutput.get(i) + " s" + hyp.getTraceState(inputWord, i+1).getId());
			
			if (! hypOutput.get(i).equals( sutOutput.get(i))) {
				out.println("#!" +sutOutput.get(i));
				break;
			} 
		}
		out.close();
	}

	private static void closeOutputStreams() {
		statsOut.close();
		absTraceOut.close();
		absAndConcTraceOut.close();
		learnOut.close();
		errOut.close();
		dupStdout.close();
	}
	
	private static de.ls5.jlearn.interfaces.EquivalenceOracle buildEquivalenceOracle(LearningParams learningParams, Oracle queryOracle) {
		de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle = null;
		if (learningParams.yanCommand == null) {
			Random random = new Random(learningParams.seed);
			RandomWalkEquivalenceOracle eqOracle1 = new RandomWalkEquivalenceOracle(learningParams.maxNumTraces,
					learningParams.minTraceLength, learningParams.maxTraceLength);
			eqOracle1.setOracle(queryOracle);
			eqOracle1.setRandom(random);
			eqOracle = eqOracle1;
		} else {
			YannakakisWrapper.setYannakakisCmd(learningParams.yanCommand);
			eqOracle = new YannakakisEquivalenceOracle(queryOracle, learningParams.maxNumTraces);
			YannakakisWrapper.setYannakakisCmd(learningParams.yanCommand);
		}
		if (learningParams.testTraces != null && !learningParams.testTraces.isEmpty()) {
			WordCheckingEquivalenceOracle eqOracle2 = new WordCheckingEquivalenceOracle(queryOracle, learningParams.testTraces);
			CompositeEquivalenceOracle compOracle = new CompositeEquivalenceOracle(eqOracle2, eqOracle);
			eqOracle = compOracle;
		}
		return eqOracle;
	}
	
	private static Tuple2<Oracle, Oracle> buildOraclesFromConfig(TCPParams tcp) {
		sutWrapper = new MapperSutWrapper(tcp.sutPort, Main.learningParams.mapper);
		tree = readCacheTree();
		if (tree == null) {
			tree = new ObservationTree();
		}
		Oracle eqOracleRunner = new NonDeterminismValidatorWrapper(10, new ObservationTreeWrapper(tree, new CacheReaderOracle(tree, new NonDeterministicOutputCheckWrapper(new LogOracleWrapper(new EquivalenceOracle(sutWrapper)))))); //new LogOracleWrapper(new EquivalenceOracle(sutWrapper));
		Oracle memOracleRunner = new NonDeterminismValidatorWrapper(10, new ObservationTreeWrapper(tree, new CacheReaderOracle(tree, new NonDeterministicOutputCheckWrapper(new LogOracleWrapper( new MembershipOracle(sutWrapper))))));
		
		/*ObservationTree tree = new ObservationTree();
		Oracle eqOracleRunner = new InvCheckOracleWrapper(new ObservationTreeWrapper(tree, new LogOracleWrapper(new EquivalenceOracle(sutWrapper)))); //new LogOracleWrapper(new EquivalenceOracle(sutWrapper));
		Oracle memOracleRunner = new InvCheckOracleWrapper(new ObservationTreeWrapper(tree, new LogOracleWrapper(new MembershipOracle(sutWrapper))));
		*/
		return new Tuple2<Oracle,Oracle>(memOracleRunner, eqOracleRunner);
	}

	public static TCPParams readConfig(Config config, SutInterface sutInterface) {
		// read/disp config params for learner
		learningParams = config.learningParams;
		learningParams.printParams(absTraceOut);
		
		// read sut interface information
		SutInfo.setMinValue(learningParams.minValue);
		SutInfo.setMaxValue(learningParams.maxValue);
		
		SutInfo.setInputSignatures(sutInterface.inputInterfaces);
		SutInfo.setOutputSignatures(sutInterface.outputInterfaces);

		LearnLog.addAppender(new PrintStreamLoggingAppender(LogLevel.DEBUG,
				learnOut));

		// read/disp TCP config
		TCPParams tcp = config.tcpParams;
		tcp.printParams(absTraceOut);
		return tcp;
	}

	public static SutInterface createSutInterface(Config config)
			throws FileNotFoundException {
		sutInterfaceFile = new File(sutConfigFile
				.getParentFile().getAbsolutePath()
				+ File.separator 
				+ config.learningParams.sutInterface);
		InputStream sutInterfaceInput = new FileInputStream(sutInterfaceFile);
		Yaml yaml = new Yaml(new Constructor(SutInterface.class));
		SutInterface sutInterface = (SutInterface) yaml.load(sutInterfaceInput);
		return sutInterface;
	}

	public static Config createConfig() throws FileNotFoundException {
		InputStream configInput = new FileInputStream(sutConfigFile);
		Yaml yaml = new Yaml(new Constructor(Config.class));
		Config config = (Config) yaml.load(configInput);
		return config;
	}

	public static void handleArgs(String[] args) {
		if (args.length == 0) {
			errOut.println("Use: java Main config_file");
			System.exit(-1);
		}
		sutConfigFile = new File(args[0]);
		if (sutConfigFile.exists() == false) {
			errOut.println("The sut config file " + args[0]
					+ " does not exist");
			System.exit(-1);
		}
		sutConfigFile = sutConfigFile.getAbsoluteFile();
	}

	public static void printUsage() {
		System.out
				.println(" config_file     - .yaml config file describing the sut/learning.");
	}
	
	public static int cachedTreeNum = 0;
	
	public static void writeCacheTree(ObservationTree tree, boolean isFinal) {
		if (tree == null) {
			System.err.println("Could not write uninitialized observation tree");
			return;
		}
		try (
				OutputStream file = new FileOutputStream(isFinal?CACHE_FILE:cachedTreeNum+CACHE_FILE);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);
				) {
			output.writeObject(tree);
			output.close();
		}  
		catch (IOException ex){
			System.err.println("Could not write observation tree");
		}
		Main.cachedTreeNum += 1;
	}
	
	public static ObservationTree readCacheTree() {
		try(
				InputStream file = new FileInputStream(CACHE_FILE);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream (buffer);
				) {
			//deserialize the List
			return (ObservationTree)input.readObject();
		}
		catch(ClassNotFoundException ex) {
			System.err.println("Cache file corrupt");
			return null;
		}
		catch(IOException ex) {
			System.err.println("Could not read cache file");
			return null;
		}
	}
}


