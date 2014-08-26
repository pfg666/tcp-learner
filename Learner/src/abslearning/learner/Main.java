package abslearning.learner;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Scanner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;

import sut.info.SutInfo;
import sut.interfacing.init.CacheManager;
import util.ExceptionAdapter;
import util.RunCmd;
import util.SoundUtils;
import util.exceptions.CheckException;
import abslearning.exceptions.BugException;
import abslearning.exceptions.ConfigurationException;
import abslearning.exceptions.Messages;
import abslearning.exceptions.OutOfTimeException;
import abslearning.exceptions.UpdateConstantActionException;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.algorithms.packs.ObservationPack;
import de.ls5.jlearn.equivalenceoracles.RandomWalkEquivalenceOracle;
import de.ls5.jlearn.interfaces.Alphabet;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Learner;
import de.ls5.jlearn.util.DotUtil;


public class Main {



	private static final Logger logger = Logger.getLogger(Main.class);
	private static final Statistics statistics = Statistics.getInstance();


	public static Learner learner = null;

	
	public static void learn_nodata(Config config)  throws FileNotFoundException {
		
		
		PrintStream statisticsFileStream = new PrintStream(
				new FileOutputStream(config.learnResults_outputDir
						+ config.learnResults_statisticsFile, false));
		PrintStream statisticsJsonFileStream = new PrintStream(
				new FileOutputStream(config.learnResults_outputDir
						+ config.learnResults_statisticsJsonFile, false));
				
			
		
		logger.info("STARTING");
		logger.debug("Maximum number of traces: " + config.testing_maxNumTraces);
		logger.debug("Minimum length of traces: "
				+ config.testing_minTraceLength);
		logger.debug("Maximim length of traces: "
				+ config.testing_maxTraceLength);
		logger.debug("Seed: " + Long.toString(config.testing_seed));
		
		statistics.setMaxTime(config.learning_maxTime);
		logger.info("maximum learning time (seconds): " + config.learning_maxTime);
		
		logger.debug("");

		


		BasicMembershipOracle memberOracle = new BasicMembershipOracle();		
		BasicEquivalenceOracle equivalenceOracle = new BasicEquivalenceOracle();	

		RandomWalkEquivalenceOracle eqOracle = new RandomWalkEquivalenceOracle(config.testing_maxNumTraces, config.testing_minTraceLength, config.testing_maxTraceLength);
		eqOracle.setOracle(equivalenceOracle);
		//Random random = new Random(config.testing_seed);
		//((RandomWalkEquivalenceOracle)eqOracle).setRandom(random);	
		

		learner = null;
		boolean done = false;
		String learningProblem = "problem learning";
		int hypCounter = 0;
		while (!done) {
			
			learner = new ObservationPack();
			learner.setOracle(memberOracle);


			Alphabet flattenedInputAlphabet=SutInfo.generateInputAlphabet(
							config.sutInterface_flattenAlphabet_minValue,
							config.sutInterface_flattenAlphabet_maxValue);
			
			learner.setAlphabet(flattenedInputAlphabet);	
			
			logger.info("alphabet:" + SutInfo.alphabetToString(flattenedInputAlphabet));
			
			try {
				
				boolean processed = false;			
				while (!done) {
					
					statistics.startLearning();
					logger.info("Start Learning");
					

					
					//----------------
					// learn hypothesis
					learner.learn();
					//----------------
					
					logger.info("Done Learning");
					statistics.stopLearning();																	
					
					
					
					//-----------------------------------
					// new learned hyp
					Automaton hyp = learner.getResult();
					statistics.storeNumStates(hyp.getAllStates().size());
					//----------------------------------

					// log hypothese
					hypCounter=hypCounter+1;	
					logger.info("Hypothesis " + hypCounter);
					
					//if (config.logging_special_hypotheses) {
					  //TODO : just write dot in hypotheses dir
					// }
					util.learnlib.Dot.writeDotFile(hyp,
							config.learnResults_outputDir
									+ config.learnResults_abstractModelDotFile);
														
					//----------------------------------------------------------
					// search for counterexample
					
					statistics.startTesting();					
					logger.info("Start Equivalence Testing");										
					EquivalenceOracleOutput o = eqOracle.findCounterExample(hyp);
					logger.info("Done Equivalence Testing");
					statistics.stopTesting();

					// no counter example -> learning is done
					// break out of both while loops when no counter example
					// found : learning is done!
					if (o == null) {
 					    statistics.terminationReason="Couldn't find counterexample after maxNumTraces applied, ";
						statistics.terminationValue=config.testing_maxNumTraces;
						statistics.learningSuccesful=true;
						learningProblem=null;						
						done = true;						
						continue;						
					}
					

					
					// log counter example
					logger.info("Found Counterexample: "
									+ o.getCounterExample().toString());
					logger.info("Send Counterexample to LearnLib");
					logger.debug("Counter Example: "
									+ o.getCounterExample().toString());
					logger.debug("o.getOracleOutput(): "
									+ o.getOracleOutput().toString());
					System.out.println(o.getCounterExample().toString());
					System.out.println(o.getOracleOutput().toString());
					done = true;
					
					//-----------------------------------------------------------------
					// give counter example to learner
					statistics.startAddCounterexample();					
					processed = learner.addCounterExample(o.getCounterExample(), o.getOracleOutput());
					assert processed : "something wrong : learnlib says counter example is not a counter example : valid trace of the hypothesis";
					statistics.stopAddCounterexample();
					//----------------------------------------------------------
					
				}
			} catch (LearningException ex) {
				learningProblem="learningException";
				logger.error("LearningException in Main");
				ex.printStackTrace();
			} catch (OutOfTimeException ex) {
				learningProblem=null;
				statistics.learningSuccesful=true;
				statistics.terminationReason=ex.getMessage() ;
				statistics.terminationValue=config.learning_maxTime;
				done=true;				
			}
		}

		SoundUtils.announce();
        // output learned model
        //---------------------
		Automaton learnedModel = learner.getResult();
		
        // write dot file
		// notes:
		//   - make start state the only highlighted state in dot file
		//   - makes highlighted state by setting attribute color='red' on state				
		util.learnlib.Dot.writeDotFile(learnedModel,
				config.learnResults_outputDir
						+ config.learnResults_abstractModelDotFile);


		// write pdf if requested :
		if (config.learnResults_writeAbstractModelPdfFile){
			DotUtil.invokeDot(
					new File(config.learnResults_outputDir+ config.learnResults_abstractModelDotFile),
					"pdf",
					new File(config.learnResults_outputDir+ config.learnResults_abstractModelPdfFile));
	    }     

				
		// output all kinds of statistics
		// ------------------------------------------
	   
		// output general statistics
		statistics.printStatistics(statisticsFileStream, true);
		statistics.printJsonStatistics(statisticsJsonFileStream, true);
		
		statistics.printStatistics(System.out, true);
		System.out.println("");


		logger.debug("Seed: " + Long.toString(config.testing_seed));
		System.out.println("Seed: " + Long.toString(config.testing_seed));
		new CacheManager().dump("cache.txt");

		// when learning gets terminated then :  
		//      two cases :
		//        - maxtime reached   -> outoftimeexception
		//        - maxtraces reached -> no counter example found after testing
		if ( learningProblem == null ) {				
		   logger.info("FINISHED LEARNING SUCCESSFULLY");
		} else {
           logger.info("FINISHED with LEARNING PROBLEM: " + learningProblem);
		}		
		
	}
	
	
	public static void init(Config config) throws FileNotFoundException {

		 System.out.println("INITIALIZING");


			// print version info
			// -------------------------------------------------
			// fetch release version
			Scanner scanner = new Scanner(new File(config.params_tomteRootPath
					+ File.separator + "release-version.txt")).useLocale(Locale.US); // .useDelimiter("\\s*");;
			double version = 0;
			if (scanner.hasNextDouble()) {
				version = scanner.nextDouble();
			} else {
				logger.warn("couldn't determine release-version");
			}
			scanner.close();

			// output  release version
			PrintWriter versionStream = util.Filesystem.getUtf8FilePrintWriter(config.learnResults_outputDir + "/version.txt");
			String release = "release-version: " + version;
			versionStream.println(release);
			logger.info(release);

			// fetch and output TOMTE svn version
			if (util.Filesystem.isdir(config.params_tomteRootPath + "/.svn") && util.Os.which("svnversion") != null ) {			
				String[] cmd = { "svnversion", config.params_tomteRootPath };
				String svn_revision = RunCmd.runCmd(cmd);
				versionStream.println("TOMTE svn revision : " + svn_revision);
				logger.info("svn revision : " + svn_revision);
			}
			versionStream.close();

		
		// initialize sut information
		// -------------------------------------------------
		// note: by SutInfo.initiliaze all needed input info about sut is stored
		// in output folder


		SutInfo.setPortNumber(config.sutInterface_portNumber);

		// modelFile is relative from config file
//		String configDir = (new File(config.params_configFile)).getParent();
		logger.info(SutInfo.getName());

		SutInfo.initialize(config.learning_sutinfoFile,
				config.sutInterface_sutWrapperClassName,
				config.learnResults_outputDir);
		
		// TCP additions
		SutInfo.setTcpConfig(TCPConfig.buildTCPConfig(config));
	}

	// run from command line in windows :
	//   java -cp bin;lib\json_simple-1.1.jar;lib\learnlib-distribution-20110714.jar;lib\snakeyaml-1.9.jar abslearning.learner.Main
	public static void main(String[] args) {
		
		Config config=null;
		boolean develMode=true;
		try {try {
				/*
				 * read config from args and config yaml file - args have higher
				 * priority then yaml file - using --configfile x.yaml an
				 * alternative yaml config file can be specified serialize final
				 * config to config yaml file
				 */
//				File file = new File(args[0]);
				//System.out.println(file.exists());
				 config = new Config(args);  // simply loads params from config file
				 
				 develMode=config.devel_modeOn;
				 config.init();  // validates and initialize config for Tomte
				 				
				
				 
				 // initialize 
				 init(config);
					
				 // learn SUT
				 learn_nodata(config);				   			    
				 
		} catch ( Exception e ) {  
			// always do in case of exception : 
			System.out.flush();
			System.err.flush();
			util.Time.millisleep(1000);	// makes sure exception stacktrace is printed after logger messages 				
			if (! Config.isLog4jConfigured() ) {
				// exception occurred before logging initialized: switch to basic log4j					 
				BasicConfigurator.configure();	
				// get the containing repository 
			    LoggerRepository repository = logger.getLoggerRepository(); 				 
			    // Set the hierarchy-wide threshold to WARN effectively disabling 
			    // all INFO and DEBUG requests. 
			    repository.setThreshold(Level.INFO);
			    
				logger.info("fatal exception occurred before logging initialized: switch to basic log4j");
			}
			throw e;
		}} catch ( CheckException ce ) {		
			logger.fatal("\n\nDEPENDENCY ERROR:" + ce.getMessage() + "\n\n");
			if ( develMode)  ce.printStackTrace();
		} catch (ConfigurationException ce) {
			logger.fatal("\n\nCONFIGURATION ERROR:\n     " + ce.getMessage() + "\n\n");
			if (develMode) ce.printStackTrace();
		} catch (BugException be) {
			logger.fatal(Messages.LEARNING_PROBLEM);
			logger.fatal(be.getMessage()  + "\n\n");
			if (develMode)  be.printStackTrace();
		} catch (UpdateConstantActionException ucae) {
			logger.fatal(ucae.getMessage() + "\n\n");
			logger.fatal(Messages.UPDATING_CONSTANT);
			if (develMode) ucae.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			logger.fatal(fnfe.getMessage() + "\n\n");
			if (develMode) fnfe.printStackTrace();
		} catch (ExceptionAdapter ea) {
			try {
				ea.rethrow();
			} catch (LearningException le) {
				logger.fatal(le.getMessage() + "\n\n");
				if (develMode) le.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				logger.fatal(fnfe.getMessage() + "\n\n");
				if (develMode) fnfe.printStackTrace();
			} catch (Exception e) {	
				logger.fatal(Messages.BUG  + "\n\n");
				// no specific handler match: rethrow ea and let java print
				// exception
				throw ea;
			}
		} catch (Exception e) {			
			logger.fatal(Messages.BUG + "\n\n");
			// no specific handler match: rethrow e and let java print exception
			if (develMode) e.printStackTrace();
		}
		
		
		logger.info("THE END");
		System.exit(0); 
	}


}
