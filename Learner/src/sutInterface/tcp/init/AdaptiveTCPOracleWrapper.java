package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.List;

import sutInterface.tcp.Flag;
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
	private Word extendedResult;

	public AdaptiveTCPOracleWrapper(Oracle oracle) {
		this.basicOracle = oracle;
		this.cacheManager = new InitCacheManager();
	}

	public Word processQuery(Word word) throws LearningException {
		List<String> inputs = new ArrayList<String>();
		extendedResult = null;
		for(Symbol inputSymbol : word.getSymbolArray()) {
			inputs.add(inputSymbol.toString());
			boolean hasTrace = cacheManager.hasTrace(inputs);
			if (hasTrace == false ) {
				boolean init = getInitForTrace(inputs);
				cacheManager.storeTrace(inputs, init);
			}
		}
		
		// if no init processing was done, 
		// 		then normally process word
		//	else (an extended result should have been obtained)
		//		remove the distinguishing output from it 							
		if(extendedResult == null) {
			return basicOracle.processQuery(word);	
		} else {
			List<String> outputs = toMessages(word);
			outputs.remove(outputs.size()-1);
			return buildWord(outputs);
		}
	}
	
	private boolean getInitForTrace(List<String> traceInputs) throws LearningException{
		boolean init;
		if (isChangeCandidate(traceInputs) == true) {;
			init = getInitForChangeCandidate(traceInputs);
		} else {
			init = getInitForNonChanger(traceInputs);
		}
		return init;
	}
	
	// Used to filter out inputs that, when applied, can not change the init state 
	private boolean isChangeCandidate(List<String> traceInputs) {
		String lastInput = traceInputs.get(traceInputs.size()-1);
		return lastInput.contains(Flag.RST.name()) || lastInput.contains(Flag.SYN.name()) ;
	}

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
		String distInput = "SYN(INV,INV)";
		traceInputs.add(distInput);
		String lastOutput = runExtendedTrace(traceInputs);
		String distOutputExpr = "((ACK\\+SYN)|(SYN\\+ACK))\\(FRESH,(?!FRESH).*"; // hard
		boolean isResetting = lastOutput.matches(distOutputExpr);
		return isResetting;
	}
	
	private String runExtendedTrace(List<String> extendedTraceInputs)  throws LearningException {
		Word word = new WordImpl();
		for(String traceInput : extendedTraceInputs) {
			word.addSymbol(new SymbolImpl(traceInput));
		}
		Word outputWord = basicOracle.processQuery(word);
		extendedResult = outputWord;
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
