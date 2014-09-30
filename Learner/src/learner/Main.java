package learner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Random;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.algorithms.packs.ObservationPack;
import de.ls5.jlearn.equivalenceoracles.RandomWalkEquivalenceOracle;

import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Learner;
import de.ls5.jlearn.logging.LearnLog;
import de.ls5.jlearn.logging.LogLevel;
import de.ls5.jlearn.logging.PrintStreamLoggingAppender;
import de.ls5.jlearn.util.DotUtil;
import de.ls5.jlearn.interfaces.State;
import java.util.LinkedList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.SimpleSutWrapper;
import sutInterface.SutInfo;
import sutInterface.SutWrapper;
import sutInterface.tcp.TCPSutWrapper;
import util.RunCmd;

public class Main {
	private static File sutConfigFile = null;
	private static int sutPort;
	private static int maxNumTraces;
	private static int minTraceLength;
	private static int maxTraceLength;
	private static long seed = System.currentTimeMillis();
	private static String seedStr = Long.toString(seed);
	public static PrintStream stdout = System.out;

	public static void main(String[] args) throws FileNotFoundException {

		handleArgs(args);

		// output learned state machine as dot and pdf file :
		File dotfile = new File("learnresult.dot");
		File pdffile = new File("learnresult.pdf");

		stdout.println("Start Learning");

		InputStream input = new FileInputStream(sutConfigFile);
		Yaml yaml = new Yaml(new Constructor(Config.class));
		Config config = (Config) yaml.load(input);

		// read in config params for learner
		maxNumTraces = config.configParams.maxNumTraces;
		minTraceLength = config.configParams.minTraceLength;
		maxTraceLength = config.configParams.maxTraceLength;
		seed = config.configParams.seed;
		sutPort = config.configParams.sutPort;
		if(sutPort == 0) {
			System.err.println("Warning: the sut port is 0 / hasn't been given");
		}

		// read in information give about sut
		SutInfo.setMinValue(config.configParams.min_value);
		SutInfo.setMaxValue(config.configParams.max_value);
		SutInfo.setInputSignatures(config.inputInterfaces);
		SutInfo.setOutputSignatures(config.outputInterfaces);

		LearnLog.addAppender(new PrintStreamLoggingAppender(LogLevel.INFO,
				System.out));
		PrintStream fileStream = new PrintStream(new FileOutputStream(
				"out.txt", false));
		System.setOut(fileStream);
		PrintStream statisticsFileStream = new PrintStream(
				new FileOutputStream("statistics.txt", false));

		seedStr = Long.toString(seed) + " - Set statically";

		System.out.println("SUT port: " + sutPort);
		System.out.println("SUT desc: " + (config.configParams.learn_tcp?"tcp":"mealy machine sut"));
		System.out.println("Maximum number of traces: " + maxNumTraces);
		System.out.println("Minimum length of traces: " + minTraceLength);
		System.out.println("Maximim length of traces: " + maxTraceLength);
		System.out.println("Seed: " + seedStr);
		System.exit(0);
		Random random = new Random(seed);
		
		// build the sut wrapper (either for simple systems or for tcp)
		SutWrapper sutWrapper = null;
		if(config.configParams.learn_tcp == true) {
			sutWrapper = new TCPSutWrapper(sutPort);
		} else {
			sutWrapper = new SimpleSutWrapper(sutPort);
		}

		EquivalenceOracle mapper = new EquivalenceOracle(sutWrapper);
		MembershipOracle memberOracle = new MembershipOracle(sutWrapper);
		mapper.setMembershipOracle(memberOracle);
		de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle;
		Learner learner = null;
		boolean done = false;
		
		// variables used for stats
		int hypCounter = 0;
		int refinementCounter = 0;
		int memQueries = 0;
		int totalMemQueries = 0;
		int totalEquivQueries = 0;
		long totalTimeMemQueries = 0;
		long totalTimeEquivQueries = 0;

		long start = System.currentTimeMillis();
		long starttmp = System.currentTimeMillis();
		long endtmp;

		while (!done) {
			eqOracle = new RandomWalkEquivalenceOracle(maxNumTraces,
					minTraceLength, maxTraceLength);
			eqOracle.setOracle(mapper);
			((RandomWalkEquivalenceOracle) eqOracle).setRandom(random);

			learner = new ObservationPack();
			learner.setOracle(memberOracle);

			learner.setAlphabet(mapper.generateInputAlphabet());
			mapper.generateOutputAlphabet();

			try {
				while (!done) {
					System.out.println("starting learning");
					System.out.println("");
					System.out.flush();
					System.err.flush();

					// execute membership queries
					learner.learn();
					System.out.flush();
					System.err.flush();
					System.out.println("done learning");

					memQueries = memberOracle.getNumMembQueries();
					statisticsFileStream.println("Membership queries: "
							+ memQueries);
					totalMemQueries += memQueries;
					endtmp = System.currentTimeMillis();
					statisticsFileStream
							.println("Running time of membership queries: "
									+ (endtmp - starttmp) + "ms.");
					totalTimeMemQueries += endtmp - starttmp;
					starttmp = System.currentTimeMillis();
					System.out.flush();

					// stable hypothesis after membership queries
					Automaton hyp = learner.getResult();
					DotUtil.writeDot(hyp, new File("tmp-learnresult"
							+ hypCounter++ + ".dot"));

					System.out.println("starting equivalence query");
					System.out.flush();
					System.err.flush();
					// search for counterexample
					EquivalenceOracleOutput o = eqOracle
							.findCounterExample(hyp);
					System.out.flush();
					System.err.flush();
					System.out.println("done equivalence query");
					statisticsFileStream
							.println("Membership queries in Equivalence query: "
									+ mapper.getNumEquivQueries());
					totalEquivQueries += mapper.getNumEquivQueries();
					endtmp = System.currentTimeMillis();
					statisticsFileStream
							.println("Running time of equivalence query: "
									+ (endtmp - starttmp) + "ms.");
					totalTimeEquivQueries += endtmp - starttmp;
					starttmp = System.currentTimeMillis();

					// no counter example -> learning is done
					if (o == null) {
						done = true;
						continue;
					}
					statisticsFileStream.println("Sending CE to LearnLib.");
					System.out.println("Counter Example: "
							+ o.getCounterExample().toString());
					System.out.flush();
					System.err.flush();
					// return counter example to the learner, so that it can use
					// it to generate new membership queries
					learner.addCounterExample(o.getCounterExample(),
							o.getOracleOutput());
					System.out.flush();
					System.err.flush();
				}
			} catch (LearningException ex) {
				System.out.println("LearningException ex in Main!");
				ex.printStackTrace();
			} catch (Exception ex) {
				statisticsFileStream.println("Exception!");
				System.out.println("Exception!");
				System.out.println("Seed: " + seedStr);
				System.err.println("Seed: " + seedStr);
				ex.printStackTrace();
				System.exit(-1);
			}
		}

		long end = System.currentTimeMillis();
		statisticsFileStream.println("");
		statisticsFileStream.println("");
		statisticsFileStream.println("STATISTICS SUMMARY:");
		statisticsFileStream.println("Total running time: " + (end - start)
				+ "ms.");
		statisticsFileStream.println("Total time Membership queries: "
				+ totalTimeMemQueries);
		statisticsFileStream.println("Total time Equivalence queries: "
				+ totalTimeEquivQueries);
		statisticsFileStream.println("Total abstraction refinements: "
				+ refinementCounter);
		statisticsFileStream.println("Total Membership queries: "
				+ totalMemQueries);
		statisticsFileStream
				.println("Total Membership queries in Equivalence query: "
						+ totalEquivQueries);

		// final output to out.txt
		System.out.println("Seed: " + seedStr);
		System.err.println("Seed: " + seedStr);
		System.out.println("Done.");
		System.err.println("Successful run.");

		// output needed for equivalence checking
		// - learnresult.dot : learned state machine
		// - output.json : abstraction,concrete alphabet, start state
		Automaton learnedModel = learner.getResult();
		State startState = learnedModel.getStart();

		statisticsFileStream
				.println("Total states in learned abstract Mealy machine: "
						+ learnedModel.getAllStates().size());

		// output learned model with start state highlighted to dot file :
		// notes:
		// - make start state the only highlighted state in dot file
		// - learnlib makes highlighted state by setting attribute color='red'
		// on state

		LinkedList<State> highlights = new LinkedList<State>();
		highlights.add(startState);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(dotfile));

			DotUtil.writeDot(learnedModel, out, learnedModel.getAlphabet()
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
		DotUtil.invokeDot(dotfile, "pdf", pdffile);

		RunCmd.runCmd("python learnlib_dot2jtorx_aut.py learnresult.dot",
				stdout, false);

		System.err.println("Learner Finished!");

		// you can uncomment this if you want some bips to notify you when
		// learning is done :)
		// SoundUtils.announce();
	}

	private static void handleArgs(String[] args) {
		if (args.length != 2) {
			System.err.println("Use: java Main --sut-config config_file");
			System.exit(-1);
		}
		if ("--sut-config".equals(args[0])) {
			if (1 == args.length) {
				System.err.println("Missing argument for --sut-config.");
				printUsage();
				System.exit(-1);
			}
			sutConfigFile = new File(args[1]);
			if (sutConfigFile.exists() == false) {
				System.err.println("The sut config file " + args[1]
						+ " does not exist");
				System.exit(-1);
			}
		}
	}

	public static void printUsage() {
		System.out
				.println(" --sut-config  config_file     - .yaml config file describing the sut/learning.");
	}
}
