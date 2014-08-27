package sut.interfacing.init;

import java.util.Stack;

/***
 * Sets up the init cache. Can be used before calling starting learning, so that all init traces are available. 
 */
public class CacheBuilder {
	private InitChecker initChecker; 
	private InitCache initCache;
	
	public CacheBuilder(InitChecker initChecker, InitCache initCache) {
		this.initChecker = initChecker;
		this.initCache = initCache;
	}
	
	public void buildInitCache(String fileName, String [] inputs, int traceLength) {
		buildInitCache(inputs, traceLength);
	} 
	
	public void buildInitCache(String [] inputs, int traceLength) {
		for(int i = 1; i < traceLength; i ++) {
			buildTraces(inputs, i, new Stack<String>());
		}
	} 
	
	private void buildTraces(String [] inputs, int traceLength, Stack<String> trace) {
		if(trace.size() == traceLength) {
			runTrace(trace);
		} else {
			for (String input : inputs) {
				trace.push(input);
				buildTraces(inputs, traceLength, trace);
				trace.pop();
			} 
		}
	}

	private void runTrace(Stack<String> trace) {
		String [] inputs = trace.toArray(new String [trace.size()]);
		boolean lastOutput = initChecker.checkTrace(inputs);
		initCache.storeTrace(inputs, lastOutput);
	}
	
	public static void main(String[] args) {
		InitChecker checker = new ActiveInitChecker(18200);
		StoringInitCache cache = new StoringInitCache("cache.txt");
		new CacheBuilder(checker, cache).buildInitCache("cache.txt", new String[]{"SYN(V,V)","ACK(V,V)","RST(V,V)"}, 3);
	}
}
