package sut.interfacing.init;

public class StoringInitCache implements InitCache{
	private CacheManager cacheManager;
	
	public StoringInitCache() {
		cacheManager = new CacheManager();
	}
	
	public StoringInitCache(String fileName) {
		this();
		cacheManager.load(fileName);
	}
	
	public void storeTrace(String [] inputs, Boolean initValue) {
		cacheManager.storeTrace(inputs, initValue);
	}
	
	public Boolean getTrace(String [] inputs) {
		return cacheManager.getTrace(inputs);
	}
}
