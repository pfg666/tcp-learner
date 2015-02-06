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
import java.util.LinkedList;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.SutInfo;
import sutInterface.SutWrapper;
import sutInterface.tcp.LearnResult;
import sutInterface.tcp.TCPMapper;
import sutInterface.tcp.TCPSutWrapper;
import sutInterface.tcp.init.AdaptiveTCPOracleWrapper;
import sutInterface.tcp.init.CachedInitOracle;
import sutInterface.tcp.init.FunctionInitOracle;
import sutInterface.tcp.init.InitCacheManager;
import sutInterface.tcp.init.InitOracle;
import util.Log;
import util.SoundUtils;
import util.Tuple2;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.algorithms.packs.ObservationPack;
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

public class Main {
	private static File sutConfigFile = null;
	private static LearningParams learningParams;
	private static long seed = System.currentTimeMillis();
	private static String seedStr = Long.toString(seed);
	private static String outputDir = "output" + File.separator + System.currentTimeMillis();
	private static File outputFolder;
	public static PrintStream learnOut;
	public static PrintStream errOut;
	public static PrintStream statsOut;
	
	public static void main(String[] args) throws FileNotFoundException, LearningException {		
		handleArgs(args);
		
		setupOutput(outputDir);

		Log.fatal("Start Learning");

		Config config = createConfig();

		SutInterface sutInterface = createSutInterface(config);
	
		TCPParams tcp = readConfig(config, sutInterface);
		
		
		// first is the membership, second is the equivalence oracle
		Tuple2<Oracle,Oracle> tcpOracles = createOraclesFromConfig(tcp);
		
		Learner learner;

		LearnResult learnResult;
		
		Random random = new Random(seed);
		RandomWalkEquivalenceOracle eqOracle = new RandomWalkEquivalenceOracle(learningParams.maxNumTraces,
				learningParams.minTraceLength, learningParams.maxTraceLength);
		eqOracle.setOracle(tcpOracles.tuple1);
		eqOracle.setRandom(random);

		learner = new ObservationPack();
		learner.setOracle(tcpOracles.tuple0);

		learner.setAlphabet(SutInfo.generateInputAlphabet());
		SutInfo.generateOutputAlphabet();
		
		learnResult = learn(learner, eqOracle);
		

		// final output to out.txt
		learnOut.println("Seed: " + seedStr);
		errOut.println("Seed: " + seedStr);
		learnOut.println("Done.");
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
		
		writeDotFiles(learnResult, highlights, out);

		errOut.println("Learner Finished!");

		// bips to notify that learning is done :)
		try {
			SoundUtils.announce();
		} catch (Exception e) {
			
		}
	}

	private static void writeDotFiles(LearnResult learnResult,
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
			// Logger.getLogger(DotUtil.class.getName()).log(Level.SEVERE, null,
			// ex);
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
	
	public static void setupOutput(String outputDir) throws FileNotFoundException {
		outputFolder = new File(outputDir);
		outputFolder.mkdirs();
		learnOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "log.txt", false));
		
		errOut = System.err;
		
		statsOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "statistics.txt", false));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				closeOutputStreams();
			}
		});
	}

	public static LearnResult learn(Learner learner,
			de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle)
			throws LearningException, ObservationConflictException {
		LearnResult learnResult = new LearnResult();
		Statistics stats = Statistics.getStats();
		stats.startTime = System.currentTimeMillis();
		long starttmp = stats.startTime;
		int hypCounter = 0;
		int memQueries = 0;
		long endtmp;
		boolean done = false;
		learnOut.println("starting learning\n");
		//try {
			while (!done) {
				learnOut.println("		RUN NUMBER: " + ++stats.runs);
				learnOut.println("");
				learnOut.flush();
				errOut.flush();

				// execute membership queries
				learner.learn();
				learnOut.flush();
				errOut.flush();
				learnOut.println("done learning");
				endtmp = System.currentTimeMillis();
				statsOut
						.println("Running time of membership queries: "
								+ (endtmp - starttmp) + "ms.");
				stats.totalTimeMemQueries += endtmp - starttmp;
				starttmp = System.currentTimeMillis();
				learnOut.flush();

				// stable hypothesis after membership queries
				Automaton hyp = learner.getResult();
				DotUtil.writeDot(hyp, new File(outputDir + File.separator + "tmp-learnresult"
						+ hypCounter++ + ".dot"));

				learnOut.println("starting equivalence query");
				learnOut.flush();
				errOut.flush();
				// search for counterexample
				EquivalenceOracleOutput o = eqOracle
						.findCounterExample(hyp);
	
				learnOut.flush();
				errOut.flush();
				learnOut.println("done equivalence query");
				endtmp = System.currentTimeMillis();
				stats.totalTimeEquivQueries += endtmp - starttmp;
				starttmp = System.currentTimeMillis();

				// no counter example -> learning is done
				if (o == null) {
					done = true;
					continue;
				}
				learnOut.println("Sending CE to LearnLib.");
				learnOut.println("Counter Example: "
						+ o.getCounterExample().toString());
				learnOut.flush();
				errOut.flush();
				// return counter example to the learner, so that it can use
				// it to generate new membership queries
				learner.addCounterExample(o.getCounterExample(),
						o.getOracleOutput());
				learnOut.flush();
				errOut.flush();
			}
		stats.endTime = System.currentTimeMillis();
		learnResult.learnedModel = learner.getResult();
		return learnResult;
	}
	
	private static void closeOutputStreams() {
		statsOut.close();
		learnOut.close();
		errOut.close();
	}
	
	private static Tuple2<Oracle, Oracle> createOraclesFromConfig(TCPParams tcp) {
		// setup tcp oracles/wrappers
		SutWrapper sutWrapper = null;
		Oracle eqOracleRunner = null;
		Oracle memOracleRunner = null;
		
		// in a normal init-oracle ("functional") TCP setup, we use the conventional eq/mem oracles
		if(! "adaptive".equalsIgnoreCase(tcp.oracle)) {
			InitOracle initOracle = new FunctionInitOracle();
			TCPMapper tcpMapper = new TCPMapper(initOracle);
			sutWrapper = new TCPSutWrapper(tcp.sutPort, tcpMapper, tcp.exitIfInvalid);
			eqOracleRunner = new EquivalenceOracle(sutWrapper);
			memOracleRunner = new MembershipOracle(sutWrapper);
		} 
		
		// in an adaptive-oracle ("adaptive") TCP setup, we wrap eq/mem oracles around an adaptive Wrapper class
		// this class, along with passing regular queries, also applies the SYN extension to determine the init-status
		// it updates the init status in a cache
		// a CachedInitOracle will then read from this cache and is used by the mapper instead of the FunctionInitOracle
		else {
			InitCacheManager cacheManager = new InitCacheManager();
			InitOracle initOracle = new CachedInitOracle(cacheManager);
			TCPMapper tcpMapper = new TCPMapper(initOracle);
			sutWrapper = new TCPSutWrapper(tcp.sutPort, tcpMapper, false);
			eqOracleRunner = new AdaptiveTCPOracleWrapper(new EquivalenceOracle(sutWrapper), cacheManager);
			memOracleRunner = new AdaptiveTCPOracleWrapper(new MembershipOracle(sutWrapper), cacheManager);
		}
		
		return new Tuple2<Oracle,Oracle>(memOracleRunner, eqOracleRunner);
	}

	public static TCPParams readConfig(Config config, SutInterface sutInterface) {
		// read/disp config params for learner
		learningParams = config.learningParams;
		learningParams.printParams(learnOut);

		// read sut interface information
		SutInfo.setMinValue(learningParams.minValue);
		SutInfo.setMaxValue(learningParams.maxValue);

		SutInfo.setInputSignatures(sutInterface.inputInterfaces);
		SutInfo.setOutputSignatures(sutInterface.outputInterfaces);

		LearnLog.addAppender(new PrintStreamLoggingAppender(LogLevel.INFO,
				learnOut));

		// read/disp TCP config
		TCPParams tcp = config.tcpParams;
		tcp.printParams(learnOut);
		return tcp;
	}

	public static SutInterface createSutInterface(Config config)
			throws FileNotFoundException {
		File sutInterfaceFile = new File(sutConfigFile
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

