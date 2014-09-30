package learner;

public class ConfigParams {
	// params for describing the sut
	public int sutPort;
	public boolean learn_tcp;
	
	// params for learning
	public int max_value;
	public int min_value;
	public long seed;
	public int maxNumTraces;
	public int minTraceLength;
	public int maxTraceLength;
}
