package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;


public class RunCmd {
	private static final Logger logger = Logger.getLogger(RunCmd.class);
    public static Process currentProcess; // for debugging
    
    
	private static class MyThread extends Thread {

		private volatile boolean threadAlive = false;
		private final InputStream in;
		private final OutputStream out;
		private String name;
		
		public MyThread(final InputStream in, final OutputStream out, String name ) {
			this.in=in;
			this.out=out;
			this.name=name;
		}
		
		public void mystop() {
			threadAlive = false;
	    }			
		
		public void run() {
			threadAlive = true;
			logger.debug("run process redirector: " + name);
			if ( in == null ) {
				logger.debug("no input stream in process redirector: " + name);
				return;
			}
			if ( out == null ) {
				logger.debug("no output stream in process redirector: " + name);
				return;
			}			
			try {
				while ( threadAlive ) {
					
					if(in.available() > 0) {
						//logger.debug("read:"+name);
						int c = in.read();
						if (c == -1) {
							//util.Time.millisleep(500);
							break;
						}	
							
						//logger.debug("write:"+name);
						out.write((char) c);						
					} else {						
						util.Time.millisleep(100);
					}
					

					
				}
				// read last bytes in "in" inputstream which got there during last millisleep
				// note: always quickly terminates when buffer empty!!
				while ( in.available() > 0 ) {
					int c = in.read();
					if (c == -1) {
						break;
					}	
					out.write((char) c);					
				}
				
				
				
			//} catch (InterruptedException e) {
			//	logger.debug("Problem handling Interrups of running command with java RunCmd API: " +name);
			} catch (IOException e) { // just exit
				logger.debug("Problem handling IO of running command with java RunCmd API: " +name);
				//e.printStackTrace(); // to stderr stream
			}
			logger.debug("process redirector stopped: " + name);
		}

	}    
    
	/*
	 * Function to automatically redirect output from a subprocess to another
	 * output stream.
	 * 
	 * e.g. redirectIO(process.getInputStream(), System.out);
	 * 
	 * Note: process.getInputStream() is the output from the subprocess which is
	 * input for current process!!
	 */

	public static MyThread redirectIO(final InputStream in, final OutputStream out, String name) {
		// http://stackoverflow.com/questions/60302/starting-a-process-with-inherited-stdin-stdout-stderr-in-java-6
		
		
	
		MyThread t=new MyThread(in,out, name);
		
		/*
		Thread t=new Thread(new Runnable() {
			private volatile boolean threadAlive = false;
			
			public void mystop() {
				threadAlive = false;
		    }			
			
			public void run() {
				threadAlive = true;
				try {
					while (threadAlive) {
						int c = in.read();
						if (c == -1)
							break;
						out.write((char) c);
					}
				} catch (IOException e) { // just exit
					logger.debug("Problem handling IO of running command with java RunCmd API");
					//e.printStackTrace(); // to stderr stream
				}
			}
		});
		*/
		t.start();
		return t;

	}

	

	public  static void osOpenFile(String filename) {
		String os=System.getProperty("os.name");
		if ( os.startsWith("Windows") ) {
		     String[] cmdarray= new String[3];
		     cmdarray[0]="cmd";
		     cmdarray[1]="/c";
		     cmdarray[2]=filename;
		     osDetachedRunCmd(cmdarray);
		} else if ( os.startsWith("Linux") ) {
		     String[] cmdarray= new String[2];
		     cmdarray[0]="xdg-open";
		     cmdarray[1]=filename;
		     osDetachedRunCmd(cmdarray);
		} else if ( os.startsWith("Mac") ) {   // Mac os x
		     String[] cmdarray= new String[2];
		     cmdarray[0]="open";
		     cmdarray[1]=filename;
		     osDetachedRunCmd(cmdarray);
		} else {
			logger.debug("Problem: unknown operating system, so do not know how to open file : " + filename);
		}
    }
	
	
	// run process detached from java runtime environment!
	//
	// note: with Runtime.exec you cannot start a process fully detached from
	//       the java Runtime. The only way to do it, is to start a local
	//       application launcher which launches it detached for you.
	//       see : http://stackoverflow.com/questions/931536/how-do-i-launch-a-completely-independent-process-from-a-java-program
	//
	// note: that this works ok when running command from commandline, 
	//       however if running from eclipse it fails : bug in eclipse terminate button!!
	//
	public  static void osDetachedRunCmd(final String[] cmd) {
		String os=System.getProperty("os.name");
		if ( os.startsWith("Windows") ) {
			 //List <String> cmdlist=Arrays.asList(cmd);
			 ArrayList <String> cmdlist = new ArrayList <String> ( Arrays.asList(cmd) );
			 cmdlist.add(0, "start");
			 cmdlist.add(0, "/c");
			 cmdlist.add(0, "cmd");
			 String cmdarray[]=cmdlist.toArray(new String[0]);
			 RunCmd.runCmd(cmdarray, null,null, true); // note: waits until launch command succeeded!
		} else if ( os.startsWith("Linux") ) {
		     String[] cmdarray= new String[2];
		     cmdarray[0]="xdg-open";
		     
		     //TODO
		     logger.debug("Problem: osDetachedRunCmd not yet implemented for Mac");

		} else if ( os.startsWith("Mac") ) {   // Mac os x
		     String[] cmdarray= new String[2];
		     cmdarray[0]="open";
		     
		     //TODO
		     logger.debug("Problem: osDetachedRunCmd not yet implemented for Mac");
		     
		} else {
			 logger.debug("Problem: unknown operating system, so do not know how to start cmd ");
		}
    }	
	
	

		
	public static  String  runCmd(final String cmd) {
		return runCmd(cmd,null);
	}
	
	public static String runCmd(final String[] cmdarray) {
		return runCmd(null,cmdarray);
	}
	
	public static String runCmd(final String cmd,final String[] cmdarray) {		
		//PrintStream outstream=new PrintStream(new ByteArrayOutputStream());
		ByteArrayOutputStream outstream=new ByteArrayOutputStream();
		ByteArrayOutputStream errstream=new ByteArrayOutputStream();
		runCmd(cmd,cmdarray,outstream,errstream,true);
		
//		try {
//			outstream.flush();
//			errstream.flush();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		//outstream.flush();
		return outstream.toString();
	}
	
	
	public  static Thread runCmd(final String cmdstr,final OutputStream outstream, final OutputStream errstream, final boolean wait)  {
		return runCmd(cmdstr,null,outstream,errstream,wait);
	}
	public  static Thread runCmd(final String[] cmdarray,final OutputStream outstream, final OutputStream errstream, final boolean wait)  {
		return runCmd(null,cmdarray,outstream,errstream,wait);
	}	


	
	/*
	 * Run a command in a separate process and redirect all its output to the
	 * given PrintStream
	 * 
	 * eg. runCmd("python plot.py",System.out,true);
	 * 
	 * runCmd("python plot.py",stdout,false);try {Thread.sleep(2000);} catch
	 * (Exception e) {};System.exit(-1);
	 * runCmd("python plot.py",stdout,true);System.exit(-1);
	 */
	public static Thread runCmd(final String cmd,final String[] cmdarray, final OutputStream outstream, final OutputStream errstream, final boolean wait) {
		logger.debug("enter runCmd function");
		final boolean done=false;
		String str;
		if ( cmd!=null ) {
    	    str=cmd;
    	} else {
    		//str=cmdarray[0];
    		str=util.JavaUtil.join(cmdarray, " ");
    	}		
		logger.debug("run command: " + str);
		Thread t = new Thread(new Runnable() {
			public void run() {
	    		MyThread redirectout = null;
	    		MyThread redirecterr = null;
	    		Process process=null;
				String str=null;
				try {
					if ( cmd!=null ) {
			    	    process = Runtime.getRuntime().exec(cmd);
			    	    str=cmd;
			    	} else {
			    		process = Runtime.getRuntime().exec(cmdarray);
			    		//str=cmdarray[0];
			    		str=util.JavaUtil.join(cmdarray, " ");
			    	}
					currentProcess=process;
		    		process.getOutputStream().close();		// Solution: closes output of subproces which is directed to input of cmd which is run by subprocess. -> cmd cannot wait for input!!	    		
					
		    		
		    		// redirect output/error streams 
			    	if (outstream != null){
			    		logger.debug("redirect out");
			    		redirectout=redirectIO(process.getInputStream(), outstream,"out");
			    	} else {
			    		logger.debug("no redirect out");
			    		process.getInputStream().close();
			    	}
			    	if (errstream != null){
			    		logger.debug("redirect err");
			    		redirecterr=redirectIO(process.getErrorStream(), errstream,"err");
			    	} else {
			    		logger.debug("no redirect err");
			    		process.getErrorStream().close();
			    	}		
			    	
			    	
			    	
			    	logger.debug("start waiting for cmd");
			    	process.waitFor();
			    	logger.debug("finished waiting for cmd");
			    	
		    	
			    	// test if process really finished					
					if (process.exitValue() != 0) {
						
						logger.error("\nExit(" + process.exitValue() + "): Problem  running command : " + str);
						throw new RuntimeException("\nExit(" + process.exitValue() + "): Problem  running command : " + str);
						// System.exit(process.exitValue());
					}
					
					logger.debug("stop cmd redirectors for stdout/stder to RunCmd's outstream/errstream ");
					// stop stream redirector processes
					if (outstream != null){
						if (redirectout.isAlive()) {
							redirectout.mystop();
							//util.Time.millisleep(2000);
							while ( redirectout.isAlive() ) { util.Time.millisleep(100); } 
						}		
						
						//logger.debug("redirector stdout alive: " + redirectout.isAlive());
					}
					if (errstream != null){
						if (redirecterr.isAlive()) {
							redirecterr.mystop();
							//util.Time.millisleep(2000);
						}	
						while ( redirecterr.isAlive() ) { util.Time.millisleep(100); } 
						//logger.debug("redirector stderr alive: " + redirecterr.isAlive());
					}					
					
					logger.debug("flush RunCmd's outstream/errstream ");
			    	// flush streams when process finished
			    	// note: do no close them, because it can be System.out/System.err which must
			    	//       be used later
			    	if (outstream != null) {
			    		  outstream.flush();
			    	}
			    	if (errstream != null) {
			    		  errstream.flush();
			    	}		
			    	
			    	
			    	logger.debug("finished cmd : " + str);
					
				} catch (InterruptedException e) {
					// if calling t.interrupt you get here
					// check process is still running, if so destroy it
					
					logger.debug("Interrupt called on process running command : " + str);
					try {
						process.exitValue();      
					} catch  ( IllegalThreadStateException  exc ){
						// process not exited yet
						logger.debug("command still running : we destroy it");
						//process.destroy();
						//System.exit(0);
					}			
					// interrupt caused by user by calling t.interrupt and therefore expected
					
				} catch (IOException e) {
					System.err.println("Problem handling IO of running command : " + e.getMessage());
					logger.debug("Problem handling IO of running command : " + str);
					try {
						process.exitValue();      
					} catch  ( IllegalThreadStateException  exc ){
						// process not exited yet
						logger.debug("command still running : we destroy it");
						process.destroy();
					}
					// unexpected interrupt : throw to end user
					throw new ExceptionAdapter(e);
									
				} catch (Exception e) {
					// if calling t.interrupt you get here, note process is not killed then.
					
					logger.debug("Problem with running command : " + str);
					try {
						process.exitValue();      
					} catch  ( IllegalThreadStateException  exc ){
						// process not exited yet
						logger.debug("command still running : we destroy it");
						process.destroy();
					}			
					// unexpected interrupt : throw to end user										
					throw new ExceptionAdapter(e);
				}
		
				
			
			}
		});
	
		logger.debug("start thread which runs cmd: ");
		t.start();
		
		if (wait) {
			logger.debug("wait for cmd to finish by waiting for thread ");			
			try {				
				t.join();
				logger.debug("thread is finished waiting, meaning cmd is finished");
			} catch (InterruptedException e) {
				t.interrupt();
				logger.debug("\nExit: Problem with thread running cmd : " + str);
				throw new ExceptionAdapter(e);
			}
			// note: outstream and errstream are flushed when cmd is finished above in Thread.run method!
			
//			// flush outstreams to be sure buffer of stream is made empty (note: already flushed after process ended above) 
//			try {
//				logger.debug("out/err flush ");
//				if (outstream != null ) outstream.flush();
//				if (errstream != null ) errstream.flush();
//				logger.debug("out/err flush done ");
//			} catch (IOException e) {
//				throw new ExceptionAdapter(e);
//			}
			
		} else {
			logger.debug("DO NOT wait for thread running the cmd to be finished");
		}
		logger.debug("leave runCmd function");

		
		return t;
	}	

	
	public static void listThreads() {
		ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
		RunCmd.listThreads(rootGroup,System.err,"   ");		
	}
	// List all threads and recursively list all subgroup
    // src: http://stackoverflow.com/questions/1323408/get-a-list-of-all-threads-currently-running-in-java
	public static void listThreads(ThreadGroup group, PrintStream stream, String indentStep ) {
    	String indent="";
		stream.println(indent + "Group[" + group.getName() + 
                        ":" + group.getClass()+"]");
        int nt = group.activeCount();
        Thread[] threads = new Thread[nt*2 + 10]; //nt is not accurate
        nt = group.enumerate(threads, false);

        // List every thread in the group
        for (int i=0; i<nt; i++) {
            Thread t = threads[i];
            stream.println(indent + "  Thread[" + t.getName() 
                        + ":" + t.getClass() + "]  daemon:" + t.isDaemon() );
        }

        // Recursively list all subgroups
        int ng = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[ng*2 + 10];
        ng = group.enumerate(groups, false);

        for (int i=0; i<ng; i++) {
            listThreads(groups[i], stream, indent + indentStep);
        }
    }

}
