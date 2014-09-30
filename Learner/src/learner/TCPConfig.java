package learner;

/***
 * Configuration parameters used for learning TCP.
 */
public class TCPConfig extends ConfigParams{

	/**
	 * The port number used for learning TCP
	 */
	public int learningPort = 18200;
	
	/**
	 * if this is true, output is written to the console,
	 * containing information about the inputs sent and outputs received
	 */
	public boolean VERBOSE = false;
	
	/**
	 * this file is used to load trace init data.
	 */
	public String CACHE_FILE = "cache.txt";
	
	/**
	 * the trace length up to which traces are generated and tested as to see if they are resetting, only used when
	 * prebuildCache = true
	 * this variable is assigned maximumTraceNumber + 1 unless specified in the configuration file
	 */
	public int maximumTraceNumber = 0;

	/**
	 * If the oracle is adaptive, then for each trace of form: 
	 * i1 ... in 
	 * We take every subtrace:
	 * i1
	 * i1 i2
	 * ...
	 * And check if the after issuing each subtrace, whether the sut is left in the initial state.
	 * This is done before sending executing the membership/equiv queries. 
	 */
	public String oracle = "adaptive";
}
