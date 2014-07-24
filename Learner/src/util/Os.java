package util;

import java.io.File;
import java.util.Map;

public class Os {
	static String path=null;
	
	/*
	 * isOsWindows - checks whether current operating system is Windows
	 * 
	 * note: usefull in case of executing cross platform system scripts. 
	 *        Because:
	 *        - in linux/osX system scripts don't have an extension because they are executed 
	 *          using a shebang line in the script. i
	 *        - in Windows system scripts must have a .bat or .cmd extension.
	 *        So cross platform scripts often have a linux/osX script version without extension,
	 *        and a .bat/.cmd version for windows.
	 *        Using the isOsWindows check one can then determine which to run from java. 
	 *        
	 */
	public static boolean isOsWindows() {
		String os=System.getProperty("os.name");
		if ( os.startsWith("Windows") ) {
			return true;
		}  else {
			return false;
		}					
	}

	/*
	 * getEnvVar - get environment variable
	 * 
	 * Note: 
	 *   In Windows the names of environment variables are case-insensitive.
	 *   src: http://en.wikipedia.org/wiki/Environment_variable#Case-insensitivity
	 *   In Unix and Unix-like systems the names of environment variables are case-sensitive.  
	 *   src: http://en.wikipedia.org/wiki/Environment_variable#Case-sensitive
	 */
	public static String getEnvVar(String var) {
		assert(var!=null);
				
		String[] envPair=new String[2];
		envPair[0]=var;
		getEnvVarPair(envPair);
		return envPair[1];
		
	}

	/* 
	 * get environment variable name and value
	 * 
	 * In windows environment variables are case insensitive,
	 * but this method lets  you also to retrieve the original case
	 * version of the variable name.
	 */
	 public static void getEnvVarPair(String[] envVarPair) {
		String varName=envVarPair[0];
		String varValue = "";		
		Map <String,String> env=System.getenv();		
		if ( Os.isOsWindows() ) {
			for ( String key : env.keySet() ) {			
				if ( varName.toUpperCase().equals(key.toUpperCase()) ) {
					varValue=env.get(key);
					varName=key;
					break;
				}
			}
		} else {
			varValue=env.get(varName);
		}
		envVarPair[0]=varName;
		envVarPair[1]=varValue;
	}

	/* 
	 * get PATH used to launch programs 
	 * 
	 * note: PATH in this application can differ from system PATH
	 */
	public static String getPath() {
		if ( path == null )  {
			path=getEnvVar("PATH");
		} 
		return path;
	}

	/* 
	 * append directory to PATH which is used to launch programs 
	 * note: PATH in this application can differ from system PATH
	 */
	public static void appendToPath(String dir) {
		assert(dir!=null);		
		path= getPath() + File.pathSeparator + dir;
	}

	/* 
	 * prepend directory to PATH which is used to launch programs
	 * note: PATH in this application can differ from system PATH
	 */
	public static void prependToPath(String dir) {
		assert(dir!=null);		
		path=  dir + File.pathSeparator + getPath();
	}

	/* 
	 * which - get path to a program
	 * 
	 * returns :
	 *    - if program found : full absolute path to program
	 *    - else : null 
	 *    
	 *  note: windows fat and ntfs filenames are by default case insensitive
	 *       src: http://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations  
	 */	
	public static String which(String program) {
	  	return which(program, null);
	}

	/* 
	 * which - get path to a program
	 * 
	 * params:
	 *     envPath: use instead of system PATH this alternative PATH
	 *              to search for the program
	 * 
	 * returns:
	 *    - if program found : full absolute path to program
	 *    - else : null 
	 *    
	 *  note: windows fat and ntfs filenames are by default case insensitive
	 *       src: http://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations  
	 */
	public static String which(String program, String envPATH) {
	    assert(program!=null);
	    // get specified directory of program      	
	    String dir=(new File(program)).getParent();   
	    if (  dir != null) { 
	    	// an path consisting of dir and filename is specified
	    	// so we don't have to look at system PATH
	    	program=(new File(program)).toString();
	    	if ( isOsWindows() ) {
	    	    return 	findWindowsExecutableFullPath(program);
	    	} else {
	            if ( Filesystem.isexecutable(program) )   return program;
	    	}    
	    } else { 
	    	// only a filename is specified and not directory
	    	// try to find program in system PATH
		    
		    if ( envPATH == null ) {
		    	envPATH=getPath();
		    }
		    //System.out.println("found PATH: " + envPATH );	
	        String[] paths=envPATH.split(File.pathSeparator);
		    for ( String path : paths ) {
		    	String exe_file=  path + "/" + program;
		    	exe_file=(new File(exe_file)).toString();
		    	if ( isOsWindows() ) {
		    	    String winexe_file=findWindowsExecutableFullPath(exe_file);
		    	    if ( winexe_file != null ) return winexe_file;
		    	} else {
		            if ( Filesystem.isexecutable(exe_file) )   return exe_file;
		    	}  		    			    	
		    }	    	
	    }
	    return null;	
	 }

	/*
	 * findWindowsExecutableFullPath - tries to find full filename of a windows executable
	 * 
	 * In windows shell you can specify an executable without an extension. Windows then 
	 * automatically adds the correct executable extension in the extension search order :
	 * 
	 *     ".exe", ".com", ".bat", ".cmd"
	 * 
	 * Source: http://www.microsoft.com/resources/documentation/windows/xp/all/proddocs/en-us/path.mspx?mfr=true
	 *         http://superuser.com/questions/228680/on-windows-what-filename-extensions-denote-an-executable
	 * Note: windows fat and ntfs filenames are by default case insensitive
	 *       src: http://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations  
	 * Note: windows shell doesn't complete anything if an executable already has an extension. 
	 * 
	 * returns :
	 *     - full executable filepath with right extension
	 *        note: for Runtime.exec(cmd) in java you must also supply the extension of the command
	 *              for .bat or .cmd extension, but not for .exe extension    
	 *        note: whereas for python subprocess module you can omit the extension (even you don't use the shell)
	 *     - null if executable couldn't be found
	 */
	public static String findWindowsExecutableFullPath(String filepath) {
		String extension=Filesystem.getFileExtension(filepath);
	   	// if filename already has extension then windows will not try to add an exe, com, bat, or cmd extension to it
	    if ( extension != null ) {
	    	if ( Filesystem.isexecutable( filepath) ) {
	    		filepath=(new File(filepath)).toString();
	    		return filepath;  
	    	} else {
	    	    return null; 
	    	}
	    }
		
	    String searchExtensions[]= {  ".exe", ".com", ".bat", ".cmd" };
	    for (String ext : searchExtensions ) {
	    	if ( Filesystem.isexecutable( filepath + ext) ) {
	    		return (new File(filepath+ ext)).toString();        	
	    	}
	    }      
	    return null;    	
	}

}
