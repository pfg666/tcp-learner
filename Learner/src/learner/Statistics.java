package learner;

public class Statistics {
	private static Statistics stats = new Statistics();
	
	public static Statistics getStats() {
		return stats;
	}
	
	public long startTime = 0;
	public long endTime = 0;
	public int numMembQueries = 0;
	public int numEquivQueries = 0;
}
