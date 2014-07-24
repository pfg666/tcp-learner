package util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {

	static public String getTimestamp() {
		   Date now;
		   DateFormat formatter;
		   formatter = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss");
		
		   now=new Date();
		   return formatter.format(now);
	 }	
	
	static public String millisecond2timestamp(long ms) {
		   DateFormat formatter;
		   formatter = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss");
		   return formatter.format(new Date(ms));		
	}
	
	static public String millisecond2humanDateString(long ms) {
		   DateFormat formatter;
		   formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		   return formatter.format(new Date(ms));		
	}	
	
	static public void millisleep(int ms) 
	{
	
		try {
			Thread.currentThread();
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new ExceptionAdapter(e);
		}	
		
	}	
	/* converts a number between 60 */
	static public String secondsToString(long runtime) {
		String result = runtime +""; 
		if (runtime < 10 ) result="0" + result;
		return result;
	}
	
	static public String millisec2string(long runtime) {
	    long runtime_seconds= runtime/1000;
	    long runtime_minutes= runtime_seconds/60;
	    long runtime_hours= runtime_minutes/60;
	    runtime_seconds=runtime_seconds % 60;  // mod 60
	    runtime_minutes=runtime_minutes % 60;  // mod 60 
	    
	    if ( (runtime % 1000) > 499 ) runtime_seconds=runtime_seconds+1;
	   
	    
	    String runtime_hms= runtime_hours + ":" + secondsToString(runtime_minutes) + ":"  + secondsToString(runtime_seconds);	
	    return runtime_hms;
    }
	
	static public String formatTime(long runtime) {
		return runtime + " ms = " + millisec2string(runtime) +" (h:m:s)";
	}
}
