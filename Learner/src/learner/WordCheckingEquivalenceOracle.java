package learner;

import java.util.ArrayList;
import java.util.List;

import util.LearnlibUtils;
import util.Log;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracle;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.equivalenceoracles.EquivalenceOracleOutputImpl;

public class WordCheckingEquivalenceOracle implements EquivalenceOracle{
	
	private List<Word> wordsToCheck = new ArrayList<Word>();
	private Oracle oracle;

	public WordCheckingEquivalenceOracle(Oracle oracle, String [] traceToCheck) {
		this.wordsToCheck.add(LearnlibUtils.symbolsToWords(traceToCheck));
		this.oracle = oracle;
	}

	public EquivalenceOracleOutput findCounterExample(Automaton hyp) {
		EquivalenceOracleOutputImpl equivOracleOutput = null;
		for (Word wordInput : wordsToCheck) {
			Word hypOutput = hyp.getTraceOutput(wordInput);
			Word sutOutput;
			try {
				sutOutput = oracle.processQuery(wordInput);
				if (!hypOutput.equals(sutOutput)) {
					Log.err("Selected word counterexample \n" +
							"for input: " + wordInput + "\n" +
							"expected: " + sutOutput + "\n" +
							"received: " + hypOutput);
					equivOracleOutput = new EquivalenceOracleOutputImpl();
					equivOracleOutput.setCounterExample(wordInput);
					equivOracleOutput.setOracleOutput(sutOutput);
					break;
				}
			} catch (LearningException e) {
				e.printStackTrace();
				Log.err("Error executing the test query: " + wordInput);
				System.exit(0);
			}
		}
		return equivOracleOutput;
	}

	public void setOracle(Oracle arg0) {
		this.oracle = arg0;
		
	}
}
