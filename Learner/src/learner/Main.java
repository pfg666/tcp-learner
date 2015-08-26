package learner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Random;

import javax.sound.midi.SysexMessage;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.CacheOracle;
import sutInterface.DeterminismCheckerOracleWrapper;
import sutInterface.ProbablisticOracle;
import sutInterface.SutInfo;
import sutInterface.SutWrapper;
import sutInterface.tcp.InvlangSutWrapper;
import sutInterface.tcp.LearnResult;
import sutInterface.tcp.TCPMapper;
import sutInterface.tcp.TCPSutWrapper;
import sutInterface.tcp.init.AdaptiveInitOracle;
import sutInterface.tcp.init.ClientInitOracle;
import sutInterface.tcp.init.FunctionalInitOracle;
import sutInterface.tcp.init.InitCacheManager;
import sutInterface.tcp.init.InitOracle;
import sutInterface.tcp.init.InvCheckOracleWrapper;
import sutInterface.tcp.init.LogOracleWrapper;
import sutInterface.tcp.init.PartialInitOracle;
import util.FileManager;
import util.Log;
import util.SoundUtils;
import util.Tuple2;
import util.learnlib.Dot;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.algorithms.angluin.Angluin;
import de.ls5.jlearn.equivalenceoracles.RandomWalkEquivalenceOracle;
import de.ls5.jlearn.exceptions.ObservationConflictException;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Learner;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.logging.LearnLog;
import de.ls5.jlearn.logging.LogLevel;
import de.ls5.jlearn.logging.PrintStreamLoggingAppender;
import de.ls5.jlearn.util.DotUtil;
import sutInterface.tcp.functionalMappers.TCPMapperSpecification;
import sutInterface.tcp.functionalMappers.TCPSutWrapperSpecification;

public class Main {
	private static File sutConfigFile = null;
	public static LearningParams learningParams;
	private static final long timeSnap = System.currentTimeMillis();
	private static final String outputDir = "output" + File.separator + timeSnap;
	private static File outputFolder;
	public static PrintStream learnOut;
	public static PrintStream absTraceOut, absAndConcTraceOut;
	public static PrintStream stdOut = System.out;
	public static PrintStream errOut = System.err;
	public static PrintStream statsOut;
	private static boolean done;
	public static Config config;
	private static File sutInterfaceFile;

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

		//learner = new ObservationPack();
		learner = new Angluin();
		learner.setOracle(tcpOracles.tuple0);

		learner.setAlphabet(SutInfo.generateInputAlphabet());
		SutInfo.generateOutputAlphabet();
		
		learnResult = learn(learner, eqOracle);
		

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
		
		errOut = System.err;
		
		statsOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "statistics.txt", false));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				closeOutputStreams();
				copyInputsToOutputFolder();
				InitCacheManager mgr = new InitCacheManager();
				mgr.dump(outputDir + File.separator +  "cache.txt"); 
				if (done == false) {
					SoundUtils.failure();
				}
			}
		});
	}

	public static LearnResult learn(Learner learner,
			de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle)
			throws LearningException, ObservationConflictException, IOException {
		LearnResult learnResult = new LearnResult();
		Statistics stats = Statistics.getStats();
		stats.startTime = System.currentTimeMillis();
		long starttmp = stats.startTime;
		int hypCounter = 0;
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
						+ hypCounter++ + ".dot";
				String hypPdfFileName = outputDir + File.separator + "tmp-learnresult"
						+ hypCounter++ + ".pdf";
				
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
			}
		stats.endTime = System.currentTimeMillis();
		learnResult.learnedModel = learner.getResult();
		return learnResult;
	}
	
	private static void closeOutputStreams() {
		statsOut.close();
		absTraceOut.close();
		absAndConcTraceOut.close();
		learnOut.close();
		errOut.close();
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
			eqOracle = new YannakakisEquivalenceOracle(queryOracle, learningParams.maxNumTraces);
		}
		if (learningParams.testTraces != null && !learningParams.testTraces.isEmpty()) {
			WordCheckingEquivalenceOracle eqOracle2 = new WordCheckingEquivalenceOracle(queryOracle, learningParams.testTraces);
			CompositeEquivalenceOracle compOracle = new CompositeEquivalenceOracle(eqOracle, eqOracle2);
			eqOracle = compOracle;
		}
		return eqOracle;
	}
	
	private static Tuple2<Oracle, Oracle> buildOraclesFromConfig(TCPParams tcp) {
		// setup tcp oracles/wrappers
		SutWrapper sutWrapper = null;
		Oracle eqOracleRunner = null;
		Oracle memOracleRunner = null;
		
		// in a normal init-oracle ("functional") TCP setup, we use the conventional eq/mem oracles
		if(! "adaptive".equalsIgnoreCase(tcp.oracle)) {
			InitOracle initOracle = null;
			if("client".equalsIgnoreCase(tcp.oracle)) {
				initOracle = new ClientInitOracle();
			} else {
				initOracle = new FunctionalInitOracle();
			}
			//TCPMapperSpecification tcpMapper = new TCPMapperSpecification(initOracle);
			//sutWrapper = new TCPSutWrapperSpecification(tcp.sutPort, tcpMapper, tcp.exitIfInvalid);
			sutWrapper = new InvlangSutWrapper(tcp.sutPort, Main.learningParams.mapper);
			//eqOracleRunner = new InvCheckOracleWrapper(new DeterminismCheckerOracleWrapper(new ProbablisticOracle(new LogOracleWrapper(new EquivalenceOracle(sutWrapper)), 1, 0.8, 1))); //new LogOracleWrapper(new EquivalenceOracle(sutWrapper));
			//memOracleRunner = new InvCheckOracleWrapper(new DeterminismCheckerOracleWrapper(new ProbablisticOracle(new LogOracleWrapper(new MembershipOracle(sutWrapper)), 1, 0.8, 1)));
			eqOracleRunner = new InvCheckOracleWrapper(new DeterminismCheckerOracleWrapper(new LogOracleWrapper(new CacheOracle(new EquivalenceOracle(sutWrapper))))); //new LogOracleWrapper(new EquivalenceOracle(sutWrapper));
			memOracleRunner = new InvCheckOracleWrapper(new DeterminismCheckerOracleWrapper(new LogOracleWrapper(new CacheOracle(new MembershipOracle(sutWrapper)))));
		}
		
		// in an adaptive-oracle ("adaptive") TCP setup, we wrap eq/mem oracles around an adaptive Wrapper class
		// this class, along with passing regular queries, also applies the SYN extension to determine the init-status
		// it updates the init status in a cache
		// a CachedInitOracle will then read from this cache and is used by the mapper instead of the FunctionInitOracle
		else {
			//TCPMapperSpecification tcpMapper = new TCPMapperSpecification();
			sutWrapper = new InvlangSutWrapper(tcp.sutPort, Main.learningParams.mapper);
			//sutWrapper = new TCPSutWrapperSpecification(tcp.sutPort, tcpMapper, false);
			//InitOracle initOracle = new AdaptiveInitOracle(tcp.sutPort, new PartialInitOracle());
			//tcpMapper.setInitOracle(initOracle);
			eqOracleRunner = new InvCheckOracleWrapper(new DeterminismCheckerOracleWrapper(new LogOracleWrapper(new EquivalenceOracle(sutWrapper))));
			memOracleRunner = new InvCheckOracleWrapper(new DeterminismCheckerOracleWrapper(new LogOracleWrapper(new MembershipOracle(sutWrapper))));
		}
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
}


