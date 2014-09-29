package learner;

/***
 * Configuration parameters used for learning TCP.
 */
public class TCPConfig{

	private static TCPConfig tcpConfig;

	/**
	 * The port number used for learning TCP
	 */
	public int learningPort = 18200;
	
	/**
	 * The ports used for testing the TCP init state. In case prebuildCache = true and multiple concurrent testers are used
	 * (parallelTestCount > 1), the ports used are from testingPort to testingPort + parallelTestCount. 
	 *  
	 */
	
	public int testingPort = 18200;
	
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
	 * the init cache is built before learning starts (at instantiation)
	 */
	public boolean prebuildCache = true;
	
	/**
	 * the trace length up to which traces are generated and tested as to see if they are resetting, only used when
	 * prebuildCache = true
	 * this variable is assigned maximumTraceNumber + 1 unless specified in the configuration file
	 */
	public int maximumTraceNumber = 0;
	
	/**
	 * pre-learning checking for init can be parallelized. (actually, most things can be, but init checking is the easiest)
	 * TODO implement parallel checking and enable the use of this parameter
	 */
	public int parallelTesterCount = 1;

	public String oracle = "adaptive";
	
	private TCPConfig(Config config) {
		this.VERBOSE = config.tcp_verbose;
		this.CACHE_FILE = config.tcp_cacheFile;
		this.prebuildCache = config.tcp_prebuildCache;
		this.maximumTraceNumber = config.testing_maxTraceLength  + 1;
		this.learningPort = config.sutInterface_portNumber;
		this.testingPort = config.sutInterface_portNumber + 1;
		this.oracle  = config.tcp_oracle;
	}
	
	public static TCPConfig buildTCPConfig(Config config) {
		tcpConfig = new TCPConfig(config);
		return tcpConfig;
	}
	
	public static TCPConfig getTCPConfig() {
		return tcpConfig;
	}
}
