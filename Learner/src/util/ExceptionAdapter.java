package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.PrintStream;

/* ExceptionAdapter - adapter class to make an unchecked exception from a checked exception
 * 
 * Java forces you to deal with checked exceptions. Sometimes during programming you want to 
 * bothered with this at that specific time. You can resolve them then by changing
 * them into an unchecked exception. The exception is not gone, but is just not enforced 
 * to be dealed with directly. The exception is still thrown but then unchecked!
 * Note: this is better than catching them and continue, because then when exception
 *       happens it is just ignored (hidden).
 * 
 * source : http://www.mindview.net/Etc/Discussions/CheckedExceptions
   
   The original exception is stored in originalException, so you can always recover it. 
   In addition, its stack trace information is extracted into the stackTrace string, which 
   will then be printed using the usual printStackTrace() if the exception gets all the way out 
   to the console. However, you can also put a catch clause at a higher level in your program to 
   catch an ExceptionAdapter and look for particular types of exceptions, like this:

	
	catch(ExceptionAdapter ea) {
	  try {
	    ea.rethrow();
	  } catch(IllegalArgumentException e) {
	    // ...
	  } catch(FileNotFoundException e) {
	    // ...
	  }
	  // etc.
	} 
 
  
    Here, you're still able to catch the specific type of exception but you're not forced to put in 
    all the exception specifications and try-catch clauses everywhere between the origin of the exception 
    and the place that it's caught. An even more importantly, no one writing code is tempted to swallow 
    the exception and thus erase it. If you forget to catch some exception, it will show up at the top level. 
    If you want to catch exceptions somewhere in between, you can.
   
    Here's some test code, just to make sure it works (not the way I suggest using it, however):

		public class ExceptionAdapterTest {
		  public static void main(String[] args) {
		    try {
		      try {
		        throw new java.io.FileNotFoundException("Bla");
		      } catch(Exception ex) {
		        ex.printStackTrace();
		        throw new ExceptionAdapter(ex);
		      }   
		    } catch(RuntimeException e) {
		      e.printStackTrace();
		    }
		    System.out.println("That's all!");
		  }
		}
		
 		
	By using this tool you can get the benefits of the unchecked exception
	approach (less code, cleaner code) without losing the core of the information
	about the exception.
	
	If you were writing code where you wanted to throw a particular type of
	checked exception, you could use (or modify, if it isn't already possible) the
	ExceptionAdapter like this:

       if(futzedUp)
         throw new ExceptionAdapter(new CloneNotSupportedException());
         
    This means you can easily use all the exceptions in their original role, but with unchecked-style coding.
    
*/
public class ExceptionAdapter extends RuntimeException {
  private final String stackTrace;
  public Exception originalException;
  public ExceptionAdapter(Exception e) {
    super(e.toString());
    originalException = e;
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    stackTrace = sw.toString();
  }
  public void printStackTrace() { 
    printStackTrace(System.err);
  }
  public void printStackTrace(PrintStream s) { 
    synchronized(s) {
      s.print(getClass().getName() + ": ");
      s.print(stackTrace);
    }
  }
  public void printStackTrace(PrintWriter s) { 
    synchronized(s) {
      s.print(getClass().getName() + ": ");
      s.print(stackTrace);
    }
  }
  public void rethrow() throws Exception { throw originalException; }
} 