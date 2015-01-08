package learner;

import java.io.PrintStream;

public class LearningParams {
	// params for learning
	public String sutInterface;
	public int maxValue;
	public int minValue;
	public long seed;
	public int maxNumTraces;
	public int minTraceLength;
	public int maxTraceLength;
	
	public void printParams(PrintStream stdout) {
		String seedStr = Long.toString(seed) + " - Set statically";

		stdout.println("Maximum number of traces: " + this.maxNumTraces);
		stdout.println("Minimum length of traces: " + this.minTraceLength);
		stdout.println("Maximim length of traces: " + this.maxTraceLength);
		stdout.println("Seed: " + seedStr);
		stdout.flush();
	}
}
