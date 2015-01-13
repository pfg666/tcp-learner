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
	private static int maxNumTraces;
	private static int minTraceLength;
	private static int maxTraceLength;
	private static LearningParams learning;
	private static long seed = System.currentTimeMillis();
	private static String seedStr = Long.toString(seed);
	public static PrintStream stdout = System.out;
	public static PrintStream stderr = System.err;

	public static void main(String[] args) throws FileNotFoundException, LearningException {
		handleArgs(args);

		Log.fatal("Start Learning");

		Config config = createConfig();

		SutInterface sutInterface = createSutInterface(config);
	
		TCPParams tcp = readConfig(config, sutInterface);
		
		PrintStream statisticsFileStream = new PrintStream(
				new FileOutputStream("statistics.txt", false));
		
		// first is the membership, second is the equivalence oracle
		Tuple2<Oracle,Oracle> tcpOracles = createOraclesFromConfig(tcp);
		
		Learner learner;

		// variables used for stats
		Statistics stats = Statistics.getStats();
		int refinementCounter = 0;

		LearnResult learnResult;
		
		Random random = new Random(seed);
		RandomWalkEquivalenceOracle eqOracle = new RandomWalkEquivalenceOracle(maxNumTraces,
				minTraceLength, maxTraceLength);
		eqOracle.setOracle(tcpOracles.tuple1);
		eqOracle.setRandom(random);

		learner = new ObservationPack();
		learner.setOracle(tcpOracles.tuple0);

		learner.setAlphabet(SutInfo.generateInputAlphabet());
		SutInfo.generateOutputAlphabet();
		
		learnResult = learn(statisticsFileStream, learner, stats, eqOracle);
		/*} catch (LearningException ex) {
			stderr.println("LearningException ex in Main!");
			ex.printStackTrace();
		} catch (Exception ex) {
			statisticsFileStream.println("Exception!");
			stdout.println("Exception!");
			stdout.println("Seed: " + seedStr);
			stderr.println("Seed: " + seedStr);
			ex.printStackTrace();
			System.exit(-1);
		}*/
		
		statisticsFileStream.println("");
		statisticsFileStream.println("");
		statisticsFileStream.println("STATISTICS SUMMARY:");
		statisticsFileStream.println("Total running time: " + (learnResult.endTime - learnResult.startTime)
				+ "ms.");
		statisticsFileStream.println("Total time Membership queries: "
				+ learnResult.totalTimeMemQueries);
		statisticsFileStream.println("Total time Equivalence queries: "
				+ learnResult.totalTimeEquivQueries);
		statisticsFileStream.println("Total abstraction refinements: "
				+ refinementCounter);
		statisticsFileStream.println("Total Membership queries: "
				+ learnResult.totalMemQueries);
		statisticsFileStream
				.println("Total Membership queries in Equivalence query: "
						+ learnResult.totalEquivQueries);

		// final output to out.txt
		stdout.println("Seed: " + seedStr);
		stderr.println("Seed: " + seedStr);
		stdout.println("Done.");
		stderr.println("Successful run.");

		// output needed for equivalence checking
		// - learnresult.dot : learned state machine
		// - output.json : abstraction,concrete alphabet, start state
		State startState = learnResult.learnedModel.getStart();

		statisticsFileStream
				.println("Total states in learned abstract Mealy machine: "
						+ learnResult.learnedModel.getAllStates().size());

		// output learned model with start state highlighted to dot file :
		// notes:
		// - make start state the only highlighted state in dot file
		// - learnlib makes highlighted state by setting attribute color='red'
		// on state

		LinkedList<State> highlights = new LinkedList<State>();
		highlights.add(startState);
		BufferedWriter out = null;
		
		
		// output learned state machine as dot and pdf file :
		File outputFolder = new File("output"+File.separator + learnResult.startTime);
		outputFolder.mkdirs();
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
				statisticsFileStream.close();
			} catch (IOException ex) {
				// Logger.getLogger(DotUtil.class.getName()).log(Level.SEVERE,
				// null, ex);
			}
		}

		// write pdf
		DotUtil.invokeDot(dotFile, "pdf", pdfFile);

		stderr.println("Learner Finished!");

		// bips to notify that learning is done :)
		try {
			SoundUtils.announce();
		} catch (Exception e) {
			
		}
	}

	public static LearnResult learn(PrintStream statisticsFileStream,
			Learner learner, Statistics stats,
			de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle)
			throws LearningException, ObservationConflictException {
		LearnResult learnResult = new LearnResult();
		learnResult.startTime = System.currentTimeMillis();
		long starttmp = learnResult.startTime;
		int hypCounter = 0;
		int memQueries = 0;
		long endtmp;
		boolean done = false;
		//try {
			while (!done) {
				stdout.println("starting learning");
				stdout.println("");
				stdout.flush();
				stderr.flush();

				// execute membership queries
				learner.learn();
				stdout.flush();
				stderr.flush();
				stdout.println("done learning");

				statisticsFileStream.println("Membership queries: "
						+ memQueries);
				learnResult.totalMemQueries += memQueries;
				endtmp = System.currentTimeMillis();
				statisticsFileStream
						.println("Running time of membership queries: "
								+ (endtmp - starttmp) + "ms.");
				learnResult.totalTimeMemQueries += endtmp - starttmp;
				starttmp = System.currentTimeMillis();
				stdout.flush();

				// stable hypothesis after membership queries
				Automaton hyp = learner.getResult();
				DotUtil.writeDot(hyp, new File("tmp-learnresult"
						+ hypCounter++ + ".dot"));

				stdout.println("starting equivalence query");
				stdout.flush();
				stderr.flush();
				// search for counterexample
				EquivalenceOracleOutput o = eqOracle
						.findCounterExample(hyp);
				stdout.flush();
				stderr.flush();
				stdout.println("done equivalence query");
				statisticsFileStream
						.println("Membership queries in Equivalence query: "
								+ stats.numMembQueries);
				learnResult.totalEquivQueries += stats.numEquivQueries;
				endtmp = System.currentTimeMillis();
				statisticsFileStream
						.println("Running time of equivalence query: "
								+ (endtmp - starttmp) + "ms.");
				learnResult.totalTimeEquivQueries += endtmp - starttmp;
				starttmp = System.currentTimeMillis();

				// no counter example -> learning is done
				if (o == null) {
					done = true;
					continue;
				}
				statisticsFileStream.println("Sending CE to LearnLib.");
				stdout.println("Counter Example: "
						+ o.getCounterExample().toString());
				stdout.flush();
				stderr.flush();
				// return counter example to the learner, so that it can use
				// it to generate new membership queries
				learner.addCounterExample(o.getCounterExample(),
						o.getOracleOutput());
				stdout.flush();
				stderr.flush();
			}
			learnResult.learnedModel = learner.getResult();
			learnResult.endTime = System.currentTimeMillis();
		return learnResult;
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
		learning = config.learningParams;
		learning.printParams(stdout);

		// read sut interface information
		SutInfo.setMinValue(learning.minValue);
		SutInfo.setMaxValue(learning.maxValue);

		SutInfo.setInputSignatures(sutInterface.inputInterfaces);
		SutInfo.setOutputSignatures(sutInterface.outputInterfaces);

		LearnLog.addAppender(new PrintStreamLoggingAppender(LogLevel.INFO,
				stdout));

		// read/disp TCP config
		TCPParams tcp = config.tcpParams;
		tcp.printParams(stdout);
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
			stderr.println("Use: java Main config_file");
			System.exit(-1);
		}
		sutConfigFile = new File(args[0]);
		if (sutConfigFile.exists() == false) {
			stderr.println("The sut config file " + args[0]
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
