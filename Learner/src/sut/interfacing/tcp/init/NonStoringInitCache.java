package sut.interfacing.tcp.init;

/**
 * Cache which can retrieve the trace status from the centralized cache but doesn't store anything.
 */
public class NonStoringInitCache implements InitCache{
	private CacheManager cacheManager;
	
	public NonStoringInitCache() {
		cacheManager = new CacheManager();
	}
	
	public NonStoringInitCache(String fileName) {
		this();
		cacheManager.load(fileName);
	}
	
	public void storeTrace(String [] inputs, Boolean initValue) {
		// do n0thing
	}
	
	public Boolean getTrace(String [] inputs) {
		return cacheManager.getTrace(inputs);
	}
}
