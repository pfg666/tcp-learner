package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.List;

import sutInterface.tcp.Flag;
import util.Log;
import util.exceptions.BugException;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

/**
 * If the initOracle is adaptive, we can only run the trace once we have init info for all its subtraces. 
 */
public class AdaptiveTCPOracleWrapper implements Oracle {

	private static final long serialVersionUID = 1L;
	private Oracle basicOracle;
	private InitCacheManager cacheManager;
	// used for storing the last output before the distinguishing input is applied, so 
	// we don't have run the trace all over again
	private String lastOutputBeforeExtension = null;

	public AdaptiveTCPOracleWrapper(Oracle oracle, InitCacheManager cacheManager) {
		this.basicOracle = oracle;
		this.cacheManager = cacheManager;
	}

	public Word processQuery(Word word) throws LearningException {
		
		List<String> inputs = new ArrayList<String>();
		
		/* We verify that at each point when executing the query we know the "is listening" value. 
		 * In case we don't, we obtain it by applying the distinguishing input SYN(V,V) */  
		for(Symbol inputSymbol : word.getSymbolArray()) {
			inputs.add(inputSymbol.toString());
			if (! cacheManager.hasTrace(inputs)) {
				Log.info("Fetching init for trace " + inputs);
				boolean init = getInitForTrace(inputs);
				cacheManager.storeTrace(inputs, init);
			}
		}

		return basicOracle.processQuery(word);
	}
	
	// return whether the server is in the listening state after executing the given trace
	private boolean getInitForTrace(List<String> traceInputs) throws LearningException{
		boolean init;
		if (isChangeCandidate(traceInputs)) {;
			init = getInitForChangeCandidate(traceInputs);
		} else {
			init = getInitForNonChanger(traceInputs);
		}
		return init;
	}
	
	// used to filter out inputs that, when applied, can not change the "is listening" state 
	private boolean isChangeCandidate(List<String> traceInputs) {
		String lastInput = traceInputs.get(traceInputs.size()-1);
		return true || lastInput.contains(Flag.RST.name()) || lastInput.contains(Flag.SYN.name()) ;
	}

	// getting the "is listening" state for a non changer does not require a asking a query, thus
	// it speeds up the inefficient oracle
	private boolean getInitForNonChanger(List<String> traceInputs) throws LearningException{
		boolean init;
		if(traceInputs.size() == 1) {
			init = true;
		} else {
			List<String> reducedInputs = traceInputs;
			reducedInputs.remove(traceInputs.size() - 1);
			if(cacheManager.hasTrace(reducedInputs) == false) {
				init = getInitForTrace(reducedInputs);
			} else {
				init = cacheManager.getTrace(reducedInputs);
			}
		}
		return init;
	}

	private boolean getInitForChangeCandidate(List<String> traceInputs) throws LearningException{
		String distInput = "SYN(V,V)";
		List<String> extendedTraceInputs = new ArrayList<String>(traceInputs);
		extendedTraceInputs.add(distInput);
		String lastOutput = runExtendedTrace(extendedTraceInputs);
		Log.info("extended trace output: " + lastOutput);
		String distOutputExpr = "((ACK\\+SYN)|(SYN\\+ACK)).*"; //\\(FRESH,(?!FRESH).*"; // hard
		boolean isResetting = lastOutput.matches(distOutputExpr);
		Log.info("match result: " + isResetting);
		return isResetting;
	}
	
	private String runExtendedTrace(List<String> extendedTraceInputs)  throws LearningException {
		Word word = new WordImpl();
		for(String traceInput : extendedTraceInputs) {
			word.addSymbol(new SymbolImpl(traceInput));
		}
		Word outputWord = basicOracle.processQuery(word);
		if (outputWord.getSymbolArray().length < 2) { 
			throw new BugException("Invalid trace given"); 
		}
		lastOutputBeforeExtension = outputWord.getSymbolByIndex(outputWord.size()-2).toString();
		return outputWord.getSymbolByIndex(outputWord.size()-1).toString();
	}
	
	// methods used to translate between strings and LearnLib words
	private List<String> toMessages(Word word) {
		List<String> inputs = new ArrayList<String>();
		for (de.ls5.jlearn.interfaces.Symbol symbol : word.getSymbolArray()) {
			inputs.add(symbol.toString());
		}
		return inputs;
	}
	
	private Word buildWord(List<String> wordInputs) {
		Word word = new WordImpl();
		for(String wordInput : wordInputs) {
			word.addSymbol(new SymbolImpl(wordInput));
		}
		return word;
	}

}
