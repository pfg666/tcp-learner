package learner;

import java.io.PrintStream;

public class Statistics {
	private static Statistics stats = new Statistics();
	
	public static Statistics getStats() {
		return stats;
	}
	
	public long startTime = 0;
	public long endTime = 0;
	public int totalEquivQueries = 0;
	public int totalMemQueries = 0;
	public int totalTimeMemQueries = 0;
	public int totalTimeEquivQueries = 0;
	public int runs = 0;
	
	public void printStats(PrintStream statsOut) {
		statsOut.println("");
		statsOut.println("");
		statsOut.println("		STATISTICS SUMMARY:");
		statsOut.println("Total running time: " + (endTime - startTime)
				+ "ms.");
		statsOut.println("Total time Membership queries: "
				+ totalTimeMemQueries);
		statsOut.println("Total time Equivalence queries: "
				+ totalTimeEquivQueries);
		statsOut.println("Total number of runs: "
				+ runs);
		statsOut.println("Total Membership queries: "
				+ totalMemQueries);
		statsOut
				.println("Total Membership queries in Equivalence query: "
						+ totalEquivQueries);
	}
}
