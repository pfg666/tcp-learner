package abslearning.learner;

import java.util.LinkedList;

public class Commandline {
	public static int portNumber=7892;
	public static int maxNumTraces;
	public static int minTraceLength;
	public static int maxTraceLength;
	public static long seed;
	public static String outputDir;

	public static String configFile;
	public static String tomteRootPath;
	
	
	public static int verboseLevel=0;

	

	public static void handleArgs(String[] args) {

				
		LinkedList <String> normalArgs= new LinkedList<String>(); 
		for (int i = 0; i < args.length; i++) {
			if ("--verbose".equals(args[i]) || "-v".equals(args[i])  ) {
				verboseLevel = 1;
				continue;
			}	
			if ("-vv".equals(args[i])  ) {
				verboseLevel = 2;
				continue;
			}				
			if ("--tomte-root-path".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --tomte-root-path.");
					printUsage();
					System.exit(-1);
				}
				tomteRootPath = args[++i];
				continue;
			}	
			if ("--output-dir".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --output-dir.");
					printUsage();
					System.exit(-1);
				}
				outputDir = args[++i];
				continue;
			}						
			if ("--max-traces".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --max-traces.");
					printUsage();
					System.exit(-1);
				}
				try {
					maxNumTraces = new Integer(args[++i]);
				} catch (NumberFormatException ex) {
					System.err.println("Error parsing argument for --max-traces. Must be integer. " + args[i]);
					System.exit(-1);
				}
				continue;
			}
			if ("--min-trace-length".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --min-trace-length.");
					printUsage();
					System.exit(-1);
				}
				try {
					minTraceLength = new Integer(args[++i]);
				} catch (NumberFormatException ex) {
					System.err.println("Error parsing argument for --min-trace-length. Must be integer. " + args[i]);
					System.exit(-1);
				}
				continue;
			}
			if ("--max-trace-length".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --max-trace-length.");
					printUsage();
					System.exit(-1);
				}
				try {
					maxTraceLength = new Integer(args[++i]);
				} catch (NumberFormatException ex) {
					System.err.println("Error parsing argument for --max-trace-length. Must be integer. " + args[i]);
					System.exit(-1);
				}
				continue;
			}
			if ("--seed".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --seed.");
					printUsage();
					System.exit(-1);
				}
				try {
					seed = new Long(args[++i]);
				} catch (NumberFormatException ex) {
					System.err.println("Error parsing argument for --seed. Must be integer. " + args[i]);
					System.exit(-1);
				}
				continue;
			}
			if ("--port".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --port.");
					printUsage();
					System.exit(-1);
				}
				try {
					portNumber = new Integer(args[++i]);
				} catch (NumberFormatException ex) {
					System.err.println("Error parsing argument for --port. Must be integer. " + args[i]);
					System.exit(-1);
				}
				continue;
			}	
			// none option arguments
			normalArgs.addLast(args[i]);
		}
		

		
	   // enforce parameter config.yaml is given!
	   // note: we also enforce arguments in learn_model.py python script 	
		if ( normalArgs.size() != 1 ) {
			printUsage();
			System.exit(-1);			
		} 
		configFile= normalArgs.get(0);
		
	/*	
		if ( normalArgs.size() == 1 ) {
			configFile= normalArgs.get(0); 
		}
	
		// always print usage message if too many params are supplied!
		if ( normalArgs.size() > 1 ) {
			printUsage();
			System.exit(-1);			
		}
	*/	 		
		
	}

	public static void printUsage() {
		System.out.println("usage: java -jar tomte.jar [options]  config.yaml");
		System.out.println("");
		System.out.println("   options:");
		System.out.println("    --max-traces       - Maximum number of traces to run during equivalence testing.");
		System.out.println("    --min-trace-length - Minimum length of traces during equivalence query.");
		System.out.println("    --max-trace-length - Maximum length of traces during equivalence query.");
		System.out.println("    --seed             - Seed to use for random number generator.");	
		System.out.println("    --output-dir       - Directory to store output.");
	  	System.out.println("    --port n           - Use tcp port n to listen on for incoming connections.");
		System.out.println("    --tomte-root-path  - Tomte Root directory");
	  	// When running tomte.jar in production it doesn't know the tomte root directory, but must
		// must be explicitly gives as commandline parameter (when running as eclipse project it is the cwd)
		// Needed for getting release version from the tomte root directory.
	  	
	}	
	
}
