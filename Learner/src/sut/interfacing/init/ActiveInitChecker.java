package sut.interfacing.init;

import sut.interfacing.MapperSocketWrapper;
import sut.mapper.TCPMapper;

/**
 * Class that checks if a trace is resetting by running the trace and by applying a distinguishing string at the end of it.
 * If the distinguishing string triggers an output matching a certain expression, it is distinguishing, otherwise, it isn't.
 */
public class ActiveInitChecker implements InitChecker {
	protected MapperSocketWrapper testWrapper = null;

	public ActiveInitChecker(MapperSocketWrapper wrapper) {
		this.testWrapper = wrapper;
		//CachedInitOracle oracleCopy = new CachedInitOracle(new DummyInitChecker(),new NonStoringInitCache());
		//testWrapper.setMapper(new TCPMapper(oracleCopy));
	}
	
	public ActiveInitChecker(int testPort) {
		this(new MapperSocketWrapper(testPort));
	}
	
	public boolean checkTrace(String [] traceInputs) {
		boolean isResetting = false;
		runEquivalentTrace(traceInputs);
		isResetting = testDistinguishingTrace();
		return isResetting;
	}
	
	private void runEquivalentTrace(String [] traceInputs) {
		testWrapper.sendReset();
		System.out.println("==Testing Trace== ");
		System.out.println("Step 1: Send all trace inputs");
		testWrapper.sendInputs(traceInputs);
	}
	
	private boolean testDistinguishingTrace() {
		String [] distinguishingTrace = new String[] {"SYN(V,V)"};
		String distinguishingOutputExpr = "((ACK\\+SYN)|(SYN\\+ACK))\\(FRESH,(?!FRESH).*"; //hard coded expression
		boolean isResetting = testDistinguishingTrace(distinguishingTrace, distinguishingOutputExpr);
		return isResetting;
	}
	
	private boolean testDistinguishingTrace(String [] traceInputs, String checkedOutputExpression ) {
		System.out.println("Step 2: Check distinguishing input");
		String lastOutput = testWrapper.sendInputs(traceInputs);
		System.out.println("Obtained output: "+lastOutput);
		boolean isResetting = lastOutput.matches(checkedOutputExpression);
		if(isResetting == true) 
			testWrapper.sendReset();
		return lastOutput.matches(checkedOutputExpression);
	}
	
	private TCPMapper buildTestMapper() {
		InitOracle testOracle = new CachedInitOracle(new DummyInitChecker(), new NonStoringInitCache());
		TCPMapper testMapper = new TCPMapper(testOracle);
		return testMapper;
	}
	
	/***
	 * Unfortunately, in order to continue where the previous state left off without running into an ugly loop, we have
	 * to use this ugly artifact of changing the oracle for the mapper to one that would not loop call the checker again.
	 */
	public boolean checkInitial(TCPMapper mapper) {
		TCPMapper testMapper = buildTestMapper();
		testWrapper.setMapper(testMapper);
		boolean isResetting = testDistinguishingTrace();
		System.out.println("RST="+isResetting);
	//	System.exit(0);
		return isResetting;
	}
	
}
