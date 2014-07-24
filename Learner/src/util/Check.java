package util;

import util.exceptions.CheckException;

public class Check {


	public static void readableFile(String file) { 
		if ( ! util.Filesystem.isreadablefile(file) ) {
			throw new CheckException("cannot read file: " + file);
		}
	}
	
	public static void executableFile(String file) { 
		if ( ! util.Filesystem.isexecutable(file) ) {
			throw new CheckException("cannot execute file: " + file);
		}
	}	
		
	public static String programExist(String program, String path) {
	    String foundPath=util.Os.which(program,path);
		if ( foundPath == null ) {
	        throw new CheckException("cannot find program '" +  program + "' in PATH");
	    }
		return foundPath;
	}
	
	public static String programExist(String program) {
		return  programExist(program, null);
	}	
	
 	
}
