package sut.interfacing.init;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/***
 * Provides trace state storing and fetching over a global static trace map. Traces are serialized and deserialized via separators.
 */
public class CacheManager {
	private static final String SEP = "_";
	private static final String SEP2 = " ";
	private static final Map<String,Boolean> cachedTraces = new HashMap<String,Boolean>();
	
	public CacheManager() {
		
	}
	
	public CacheManager(String fileName) {
		this();
		load(fileName);
	}
	
	private String buildTraceEntry(String [] inputs) {
		StringBuilder builder = new StringBuilder();
		for(String input : inputs) {
			builder.append(input).append(SEP);
		}
		return builder.toString();
	}
	
	public void storeTrace(String [] inputs, Boolean initValue) {
		String trace =  buildTraceEntry(inputs);
		cachedTraces.put(trace, initValue);
	}
	
	public Boolean getTrace(String [] inputs) {
		String trace = buildTraceEntry(inputs);
		return cachedTraces.get(trace);
	}
	
	public void dump(String fileName) {
		dump(fileName, cachedTraces);
	}
	
	public void dump(String fileName, Map<String,Boolean> cachedTraces) {
		try {
			FileWriter fw = new FileWriter(fileName, false);
			for (String message : cachedTraces.keySet()) {
				fw.append(message + SEP2 + cachedTraces.get(message) + "\n");
			}
			fw.append("\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load(String fileName) {
		load(fileName, cachedTraces);
	}
	
	public void load(String fileName, Map<String,Boolean> cachedTraces) {
		try {
			 BufferedReader reader = new  BufferedReader(new FileReader(fileName));
			 String line;
			 while((line = reader.readLine()) != null && line.isEmpty() == false) {
				 String [] tokens = line.split(SEP2);
				 String trace = tokens[0];
				 Boolean init = Boolean.parseBoolean(tokens[1]);
				 cachedTraces.put(trace, init);
			 }
			 reader.close();
		} catch (FileNotFoundException e) {
			System.err.println("Invalid cache file path "+fileName);
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean compare(String cacheFile1, String cacheFile2) {
		Map<String, Boolean> cacheMap1 = new HashMap<String, Boolean>();
		Map<String, Boolean> cacheMap2 = new HashMap<String, Boolean>();
		load(cacheFile1, cacheMap1);
		load(cacheFile2, cacheMap2);
		return cacheMap1.equals(cacheMap2);
	}
	
	public void display() {
		System.out.println(cachedTraces.toString());
	}
	
	public static void main(String[] args) {
		CacheManager cm = new CacheManager();
		//cm.dump(config.CACHE_FILE);
		System.out.println(cm.compare("cache.txt", "cache2.txt"));
	}
}
