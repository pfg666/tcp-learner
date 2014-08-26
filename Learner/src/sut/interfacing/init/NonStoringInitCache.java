package sut.interfacing.init;

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
