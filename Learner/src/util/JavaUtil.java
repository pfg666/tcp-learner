package util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractList;
import java.util.List;

import org.apache.log4j.Logger;

import sut.info.SutInterface;

/* 
 *  java helpers
 * 
 */
public class JavaUtil {
	private static final Logger logger = Logger.getLogger(JavaUtil.class);
	
	private static String javacCmd;

	public static String getJavacCmd() {
		return javacCmd;
	}

	public static void setJavacCmd(String javacCmd) {
		JavaUtil.javacCmd = javacCmd;
	}

	//Helper method to convert int arrays into Lists
	// source: http://stackoverflow.com/questions/960431/how-to-convert-listinteger-to-int-in-java
	public static List<Integer> intArrayAsList(final int[] a) {
	    if(a == null)
	        throw new NullPointerException();
	    return new AbstractList<Integer>() {

	        @Override
	        public Integer get(int i) {
	            return a[i];//autoboxing
	        }
	        @Override
	        public Integer set(int i, Integer val) {
	            final int old = a[i];
	            a[i] = val;//auto-unboxing
	            return old;//autoboxing
	        }
	        @Override
	        public int size() {
	            return a.length;
	        }
	    };
	}	
	
	public String repeat(String s, int times){
	    StringBuffer b = new StringBuffer();

	    for(int i=0;i < times;i++){
	        b.append(s);
	    }

	    return b.toString();
	}	
	
	public static String join(Object[] s, String glue)
	{
		int k = s.length;
		if (k == 0)
			return "";
		StringBuilder out = new StringBuilder();
		out.append(s[0]);
		for (int x = 1; x < k; ++x)
			out.append(glue).append(s[x]);
		return out.toString();
	}	
	
	public static String getClassPath(String classPackage, String className) {
		 String packagePath = classPackage.replace('.', '/');
		 return  packagePath + '/' +  className + ".java";	 	 		
	}

	public static String getFullClassPath(String sourcePathDir, String classPackage, String className) {			
		 return  sourcePathDir + "/" + getClassPath(classPackage, className);	 	 		
	}

	/* compileJavaSource - compile a java source file 
	 * 
	 * - compiles <classPathDir>/<classPackage as path>/<className>.java
	 * - <classPath> is the java CLASSPATH which specifies all paths having  java compiled bytecode classes which may be needed
	 *   for compiling dependencies
	 * - note: <classPathDir> must already exist but <classPackage as path> will be made on the fly
	*/
	public static void compileJavaSource(String classPath, String sourcePathDir, String classPackage, String className) {
		 String cmd_str=javacCmd +" -sourcepath " + sourcePathDir + " -classpath " + classPath + " " + getFullClassPath(sourcePathDir,classPackage, className);
		 RunCmd.runCmd(cmd_str,System.out,System.err,true);
		 logger.debug(cmd_str);
	}

	//  java class loader where you can specify runtime the CLASSPATH
	//
	// when a class is not in the CLASSPATH you normally cannot use it in java,
	// however with this function you can!
	// 
	// note: after loading you must cast the object to the right interface
	public static 	Object	 loadJavaClass(String classPathDir ,String fullClassName) {
	    ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
	
	    URL dirUrl;
	    
	    URLClassLoader urlClassLoader = null; 
	     
		try {
			dirUrl = new File(classPathDir).toURL();
			
	        // Add the conf dir to the classpath
	        // Chain the current thread classloader
	         urlClassLoader
	         = new URLClassLoader(new URL[]{dirUrl},
	        		 currentThreadClassLoader);
	
	        // Replace the thread classloader - assumes
	        // you have permissions to do so
	      //  Thread.currentThread().setContextClassLoader(urlClassLoader);
	        
		} catch (MalformedURLException e) {
			throw new ExceptionAdapter(e);
		}
		Class cls = null;
		Object inst= null;
		try {
			cls=urlClassLoader.loadClass(fullClassName);
			try {
				inst= (SutInterface) cls.newInstance();
			} catch (InstantiationException e) {
				throw new ExceptionAdapter(e);
			} catch (IllegalAccessException e) {
				throw new ExceptionAdapter(e);
			}
		} catch (ClassNotFoundException e) {
			throw new ExceptionAdapter(e);
		}
				
		return inst; 
	}

}
