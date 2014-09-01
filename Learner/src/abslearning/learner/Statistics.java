package abslearning.learner;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONValue;

import ru.yandex.lc.jbd.Dumper;
import util.JsonWriter;
import util.Time;
import abslearning.exceptions.OutOfTimeException;

//	String runtime_hms= runtime_hours + ":" + runtime_minutes + ":"  + runtime_seconds;
//  statisticsFileStream.println("Total running time: "  + runtime_hms + " (h:m:s)");

public class Statistics {
	private static final Logger logger = Logger.getLogger(Statistics.class);
    	
	private long startTime,endTime;
	public long maxTime=0; // in milliseconds
	public long cadpCompareTime=0; // ignore this time in maxtime
	public long cadpCompareTime_start=0;
	public long cadpCompareTime_stop=0;
	
	public String terminationReason;
	public long terminationValue=0;
	public boolean learningSuccesful=false;
	
	// singleton
	// ---------

	private static final Statistics INSTANCE = new Statistics();

	private Statistics() {
		runs = new LinkedList<SingleRun>();
		startTime=System.currentTimeMillis();
		//maxTime=300000; // 30 seconds
	}
	
	/* set maxtime in seconds */
	public void setMaxTime(long maxTime) {		
		this.maxTime=maxTime*1000;
	}		

	public boolean checkTimePassed() {
		if ( maxTime > 0  && System.currentTimeMillis() >  startTime+maxTime+cadpCompareTime ) {
			return true;
		}
		return false;		
	}
	
	public static Statistics getInstance() {
		return INSTANCE;
	}

	// object fields
	private LinkedList<SingleRun> runs;
	private SingleRun currentRun;


    public int equivQueryPollTimeIndex = 1000;
    public int equivQueryIndex;
    
    public int memQueryPollTimeIndex = 1000;
    public int memQueryIndex;    
	
	class SingleRun {
		public String phase= "before";
		public long refinementCounter = 0;
		
		public long numMemQueriesLearning = 0;
		public long numMemQueriesTesting = 0;
		public long numEquivQueries = 0;
		//public long timeMemQueries = 0;
		//public long timeEquivQueries = 0;

		public long startLearningTime = 0;
		public long stopLearningTime = 0;
		public long startTestingTime = 0;
		public long stopTestingTime = 0;
		
		public long numStates = 0;
	}

	
	public void storeNumStates(long numberStatesInHypothesis) {
		currentRun.numStates = numberStatesInHypothesis;
	}	
	
	public String getCurrentPhase() {
		return currentRun.phase;
	}	
	
	public void incRefinementCounter() {
		currentRun.refinementCounter = currentRun.refinementCounter + 1;
	}
	
	public void incMemQueries() {
		if(currentRun.startTestingTime>0){
			currentRun.numMemQueriesTesting = currentRun.numMemQueriesTesting + 1;
		}
		else {
			currentRun.numMemQueriesLearning = currentRun.numMemQueriesLearning + 1;
			
            // only poll time during real member queries, because we only have few member queries during testing 	
			memQueryIndex=memQueryIndex+1;
			if ( memQueryIndex > memQueryPollTimeIndex ) {
				  if ( checkTimePassed() ) {
					  stopLearning();
					  throw new OutOfTimeException("total learning time passed when executing a memQuery" );
				  }
	              memQueryIndex=0;			  			  
			}		
		}		
		

	}	
	
	public void decMemQueries() {
		if(currentRun.startTestingTime>0){
			currentRun.numMemQueriesTesting = currentRun.numMemQueriesTesting - 1;
		}
		else {
			currentRun.numMemQueriesLearning = currentRun.numMemQueriesLearning - 1;
		}				
	}
	
	public void incEquivQueries() {		
		currentRun.numEquivQueries = currentRun.numEquivQueries + 1;
		
		equivQueryIndex=equivQueryIndex+1;
		if ( equivQueryIndex > equivQueryPollTimeIndex ) {
			  if ( checkTimePassed() ) {
				  stopTesting();
				  throw new OutOfTimeException("total learning time passed when executing a equivQuery" );
			  }
              equivQueryIndex=0;			  			  
		}
	}
	
	public void setPhase(String phase) {		
		currentRun.phase= phase;
	}	

	public void startLearning() {
		memQueryIndex=0; 
		currentRun = new SingleRun();
		runs.add(currentRun);
		currentRun.startLearningTime = System.currentTimeMillis();
		currentRun.phase= "learning";
	}

	public void stopLearning() {
		currentRun.stopLearningTime = System.currentTimeMillis();
		currentRun.phase= "afterLearning";
	}

	public void startTesting() {
		equivQueryIndex=0; 
		currentRun.startTestingTime = System.currentTimeMillis();
		currentRun.phase= "testing";
	}

	public void stopTesting() {
		currentRun.stopTestingTime = System.currentTimeMillis();
		currentRun.phase= "afterTesting";
	}
	
	public void startCadpCompare() {
		cadpCompareTime_start =  System.currentTimeMillis();
	}

	public void stopCadpCompare() {
		cadpCompareTime_stop = System.currentTimeMillis();
		cadpCompareTime = cadpCompareTime + ( cadpCompareTime_stop - cadpCompareTime_start );
	}	
	
	public void startAddCounterexample() {
		currentRun.phase= "addCounterexample";
	}

	public void stopAddCounterexample() {
		currentRun.phase= "afterCounterexample";
	}


	
	
	public void printStatistics( PrintStream stream, boolean withTiming) {
		logger.info("total time running cadp compare: " + cadpCompareTime/1000);
		printStatisticsRunsOrig(stream,withTiming);
		printStatisticsSummaryOrig(stream,withTiming);
	}		
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void printJsonStatistics( PrintStream outstream, boolean withTiming) {
		
		 //PrintWriter stream=new PrintWriter();

		 JsonWriter stream = new JsonWriter(new OutputStreamWriter (outstream));
		//printStatisticsRuns(stream,withTiming);
		//printStatisticsSummary(stream,withTiming);
		
		
		List jsonruns=new LinkedList();
		for ( int i=0 ; i < runs.size() ; i++ ) {
			SingleRun run=runs.get(i);
		//for (SingleRun run : runs) {
			Map jsonrun=new LinkedHashMap(); 
			long runMemTime = run.stopLearningTime - run.startLearningTime;
			long runTestTime = run.stopTestingTime - run.startTestingTime;

			jsonrun.put("runNumber", i+1 );
			jsonrun.put("numMemQueriesLearning", run.numMemQueriesLearning);
			if (withTiming)  jsonrun.put("runMemTime", runMemTime);
		    jsonrun.put("numEquivQueries", run.numEquivQueries);
			jsonrun.put("numMemQueriesTesting", run.numMemQueriesTesting);
			if (withTiming)  jsonrun.put("runTestTime", runTestTime);
			jsonrun.put("refinementCounter", run.refinementCounter);
			jsonrun.put("numStates", run.numStates);
			
			jsonruns.add(jsonrun);
		}
			
		Map summary=getJsonStatisticsSummary(withTiming);
		
		Map root=new LinkedHashMap(); // keeps key order!!
		
		root.put("runs", jsonruns);
		root.put("summary",summary);
		
		String jsonText = JSONValue.toJSONString(root);
		try {
			stream.write(jsonText);
			//stream.write(jsonText);
			stream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public  LinkedHashMap getJsonStatisticsSummary( boolean withTiming) {
		long totalMemQueriesLearning = 0;
		long totalMemQueriesTesting = 0;
		long totalEquivQueries = 0;
		long totalTimeLearning = 0;
		long totalTimeTesting = 0;
		long finalNumStates=0;
		long totalRefinementCounter = 0;
		

		endTime=System.currentTimeMillis();
		long totalRealTime= endTime-startTime;		
		
		SingleRun lastRun=runs.getLast();
		for ( int index=0 ; index < (runs.size()-1) ; index++) {
			SingleRun run=runs.get(index); 
			long runMemTime = run.stopLearningTime - run.startLearningTime;
			long runTestTime = run.stopTestingTime - run.startTestingTime;
			totalMemQueriesLearning = totalMemQueriesLearning + run.numMemQueriesLearning;
			totalMemQueriesTesting = totalMemQueriesTesting + run.numMemQueriesTesting;
			totalEquivQueries = totalEquivQueries + run.numEquivQueries;
			totalTimeLearning = totalTimeLearning + runMemTime;
			totalTimeTesting = totalTimeTesting + runTestTime;
			
			totalRefinementCounter = totalRefinementCounter + run.refinementCounter;
			finalNumStates=run.numStates;
		}
		long runMemTime = lastRun.stopLearningTime - lastRun.startLearningTime;
		long runTestTime = lastRun.stopTestingTime - lastRun.startTestingTime;
		totalMemQueriesLearning = totalMemQueriesLearning + lastRun.numMemQueriesLearning;
		totalMemQueriesTesting = totalMemQueriesTesting + lastRun.numMemQueriesTesting;
		totalTimeLearning = totalTimeLearning + runMemTime;	
		totalRefinementCounter = totalRefinementCounter + lastRun.refinementCounter;
		finalNumStates=lastRun.numStates;		

		long totalTimeEquivQueriesWithLastRun=totalTimeTesting+ runTestTime;
		long totalEquivQueriesWithLastRun=totalEquivQueries + lastRun.numEquivQueries;
		
		
		LinkedHashMap summary=new LinkedHashMap();
		
		if (withTiming) {
			summary.put("totalRunningTime",totalTimeLearning+totalTimeTesting); 
			summary.put("totalRunningTimeWithLastRun",totalTimeLearning+totalTimeEquivQueriesWithLastRun); 

			summary.put("totalTimeLearning",totalTimeLearning);
			summary.put("totalTimeTesting",totalTimeTesting);
			summary.put("totalTimeTestingWithLastRun",totalTimeEquivQueriesWithLastRun);
			
			summary.put("realTotalTime",totalRealTime);
			summary.put("realStartTime",startTime);
			summary.put("realEndTime",endTime);
			
		}
		
		summary.put("numRuns",runs.size());
		summary.put("totalMemQueriesLearning",totalMemQueriesLearning);
		summary.put("totalEquivQueries",totalEquivQueries);
		summary.put("totalEquivQueriesWithLastRun",totalEquivQueriesWithLastRun);
		summary.put("totalMemQueriesTesting",totalMemQueriesTesting);
		summary.put("totalRefinementCounter",totalRefinementCounter);
		summary.put("finalNumStates",finalNumStates);
		
		summary.put("terminationReason",terminationReason);
		summary.put("terminationValue",terminationValue);
		summary.put("learningSuccesful",learningSuccesful);
		
		return summary;
	}	
	
	public void dumpStatistics(Writer outstream) {
		PrintWriter stream=new PrintWriter(outstream);
		Dumper dmpr = new Dumper();
		dmpr.setMaxDepth(2);  // do not follow this in inner class!
		stream.write(dmpr.dump(runs));
		if ( stream.checkError() )  logger.error("problem writing statistics to File");
	}	
	
	public void printStatisticsRunsOrig( PrintStream stream, boolean withTiming) {
		
		stream.println("-------------------------------------------------------------------------------------");
		stream.println("                           RUNS");


		for ( int i=0 ; i < runs.size() ; i++ ) {
			SingleRun run=runs.get(i);
		//for (SingleRun run : runs) {
			long runMemTime = run.stopLearningTime - run.startLearningTime;
			long runTestTime = run.stopTestingTime - run.startTestingTime;

			stream.println("-------------------------------------------------------------------------------------");
			stream.println("Run number                                     : " + (i+1) );	
			stream.println("Membership queries                             : " + run.numMemQueriesLearning);
			if (withTiming) stream.println("Running time of membership queries             : " + Time.formatTime(runMemTime) );
			stream.println("Testing equivalence queries                    : " + run.numEquivQueries);
			stream.println("Testing membership queries                     : " + run.numMemQueriesTesting);
			if (withTiming) stream.println("Running time of testing queries                : " + Time.formatTime(runTestTime) );
			stream.println("Abstraction refinement done                    : " + run.refinementCounter);
			stream.println("States in hypothesis                           : " + run.numStates );						
		}
		if ( stream.checkError() )  logger.error("problem writing statistics to File");
	
	}

	public void printStatisticsSummaryOrig( PrintStream stream, boolean withTiming) {
		long totalMemQueriesLearning = 0;
		long totalMemQueriesTesting = 0;
		long totalEquivQueries = 0;
		long totalTimeLearning = 0;
		long totalTimeTesting = 0;
		long finalNumStates=0;
		long totalRefinementCounter = 0;
		
		endTime=System.currentTimeMillis();
		long totalRealTime= endTime-startTime;
		
		SingleRun lastRun=runs.getLast();
		for ( int index=0 ; index < (runs.size()-1) ; index++) {
			SingleRun run=runs.get(index); 
			long runMemTime = run.stopLearningTime - run.startLearningTime;
			long runTestTime = run.stopTestingTime - run.startTestingTime;
			totalMemQueriesLearning = totalMemQueriesLearning + run.numMemQueriesLearning;
			totalMemQueriesTesting = totalMemQueriesTesting + run.numMemQueriesTesting;
			totalEquivQueries = totalEquivQueries + run.numEquivQueries;
			totalTimeLearning = totalTimeLearning + runMemTime;
			totalTimeTesting = totalTimeTesting + runTestTime;
			
			totalRefinementCounter = totalRefinementCounter + run.refinementCounter;
			finalNumStates=run.numStates;
		}
		long runMemTime = lastRun.stopLearningTime - lastRun.startLearningTime;
		long runTestTime = lastRun.stopTestingTime - lastRun.startTestingTime;
		totalMemQueriesLearning = totalMemQueriesLearning + lastRun.numMemQueriesLearning;
		totalMemQueriesTesting = totalMemQueriesTesting + lastRun.numMemQueriesTesting;
		totalTimeLearning = totalTimeLearning + runMemTime;	
		totalRefinementCounter = totalRefinementCounter + lastRun.refinementCounter;
		finalNumStates=lastRun.numStates;		

		long totalTimeEquivQueriesWithLastRun=totalTimeTesting+ runTestTime;
		long totalEquivQueriesWithLastRun=totalEquivQueries + lastRun.numEquivQueries;
		
		stream.println("-------------------------------------------------------------------------------------");
		stream.println("                           SUMMARY");
		stream.println("-------------------------------------------------------------------------------------");
		if (withTiming) stream.println("Total running time                             : " +  Time.formatTime(totalTimeLearning+totalTimeTesting) ); 
		if (withTiming) stream.println("               `-> with last run               : " +  Time.formatTime(totalTimeLearning+totalTimeEquivQueriesWithLastRun) ); 

		if (withTiming) stream.println("Total running time of Membership queries       : " + Time.formatTime(totalTimeLearning));
		if (withTiming) stream.println("Total running time of Testing queries          : " + Time.formatTime(totalTimeTesting));
		if (withTiming) stream.println("                             `-> with last run : " + Time.formatTime(totalTimeEquivQueriesWithLastRun));
		if (withTiming) stream.println("");
		
		if (withTiming) stream.println("total realTime                                  : " + Time.formatTime(totalRealTime));
		if (withTiming) stream.println("real start time                                 : " + startTime + " ms since Jan 1, 1970 GMT  = " + Time.millisecond2humanDateString(startTime)  );	
		if (withTiming) stream.println("real end time                                   : " + endTime + " ms since Jan 1, 1970 GMT  = " + Time.millisecond2humanDateString(endTime)  );	
		if (withTiming) stream.println("");
		
		stream.println("Total runs (include last test run)             : " + runs.size());
		stream.println("Total membership queries                       : " + totalMemQueriesLearning);
		//stream.println("Total Membership queries in Equivalence query: " + totalEquivQueries);
		stream.println("Total Testing equivalence queries              : " + totalEquivQueries);
		stream.println("                  `-> with last run            : " + totalEquivQueriesWithLastRun);		
		stream.println("Total Testing membership queries               : " + totalMemQueriesTesting);		
		
		stream.println("Total abstraction refinements                  : " + totalRefinementCounter );
		stream.println("Total states in learned abstract Mealy machine : " +  finalNumStates );
		

		stream.println("Termination Reason                             : " + terminationReason);
		stream.println("Termination Value                              : " + terminationValue);
		stream.println("Learning Succesful                             : " + learningSuccesful);
				
		
		if ( stream.checkError() )  logger.error("problem writing statistics to File");
	}	
	

}
