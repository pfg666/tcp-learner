package learner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import util.Container;
import util.LearnlibUtils;
import util.Log;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.equivalenceoracles.EquivalenceOracleOutputImpl;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracle;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class YannakakisEquivalenceOracle implements EquivalenceOracle{
	
	private Oracle oracle;
	private final int numberOfTests;
	private final boolean uniqueOnly;
	private final Container<Integer> uniqueCounter;
	private int hypTestNumber = 0;
	
	public YannakakisEquivalenceOracle (Oracle oracle, int numberOfTests) {
		this(oracle, numberOfTests, null);
	}
	
	public YannakakisEquivalenceOracle (Oracle oracle, int numberOfTests, Container<Integer> uniqueCounter) {
		this.oracle = oracle;
		if (numberOfTests <= 0) {
			this.numberOfTests = Integer.MAX_VALUE - 1;
		} else {
			this.numberOfTests = numberOfTests;
		}
		this.uniqueCounter = uniqueCounter;
		this.uniqueOnly = uniqueCounter != null;
	}
	
	@Override
	public EquivalenceOracleOutput findCounterExample(Automaton hyp) {
		List<String> testQuery = null;
		YannakakisWrapper wrapper = new YannakakisWrapper(hyp);
		wrapper.initialize();
		String line;
		int uniqueValueStart = uniqueOnly ? uniqueCounter.value : 0;
		try {
			for (hypTestNumber = 0;	hypTestNumber < numberOfTests; hypTestNumber = uniqueOnly ? uniqueCounter.value - uniqueValueStart : hypTestNumber + 1) {
				Log.info("Equivalence test " + hypTestNumber + " for this hypothesis");
				line = wrapper.out().readLine();
			
				if ( line != null) {
					testQuery = getNextTestFromLine(line);
					
					Word wordInput = LearnlibUtils.symbolsToWords(testQuery); 
					Word hypOutput = hyp.getTraceOutput(wordInput);
					Word sutOutput;
					try {
						sutOutput = oracle.processQuery(wordInput);
						if (!hypOutput.equals(sutOutput)) {
							Log.err("Yannakakis counterexample \n" +
									"for input: " + wordInput + "\n" +
									"expected: " + sutOutput + "\n" +
									"received: " + hypOutput);
							EquivalenceOracleOutputImpl equivOracleOutput = new EquivalenceOracleOutputImpl();
							equivOracleOutput.setCounterExample(wordInput);
							equivOracleOutput.setOracleOutput(sutOutput);
							wrapper.close();
							Log.err("Counterexample found after " + hypTestNumber + " attempts");
							return equivOracleOutput;
						}
					} catch (LearningException e) {
						e.printStackTrace();
						throw new RuntimeException("Error executing the test query: " + wordInput);
					}
				} else {
					Log.err("Yannakakis did not produce enough equivalence queries");
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Generated IO Exception while generating tests from stdin");
		}
		wrapper.close();
		Log.err("No counterexample found after " + hypTestNumber + " attempts");
		return null;
	}
	
	public List<String> getNextTestFromLine(String line) {
		ArrayList<String> testQuery = new ArrayList<String>();

		Scanner s = new Scanner(line);
		while(s.hasNext()) {
			testQuery.add(s.next());
		}
		s.close();
		
		return testQuery;
	}

	public void setOracle(Oracle arg0) {
		this.oracle = arg0;
	}
	
	/**
	 * Gets the number of tests done for the current/most recent hypothesis
	 * @return
	 */
	public int getNrHypthesisTests() {
		return this.hypTestNumber;
	}
}
