package learner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
	private int numberOfTests;
	
	public YannakakisEquivalenceOracle (Oracle oracle) {
		this(oracle, 0);
	}
	
	public YannakakisEquivalenceOracle (Oracle oracle, int numberOfTests) {
		this.oracle = oracle;
		if (numberOfTests <= 0) {
			this.numberOfTests = Integer.MAX_VALUE - 1;
		} else {
			this.numberOfTests = numberOfTests;
		}
	}
	
	@Override
	public EquivalenceOracleOutput findCounterExample(Automaton hyp) {
		List<String> testQuery = null;
		YannakakisWrapper wrapper = new YannakakisWrapper(hyp);
		wrapper.initialize();
		String line;
		int i = 0;
		try {
			for (; i<numberOfTests; i++) {
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
							Log.err("Counterexample found after " + i + " attempts");
							return equivOracleOutput;
						}
					} catch (LearningException e) {
						Log.err("Error executing the test query: " + wordInput);
						System.exit(0);
					}
				} else {
					break;
				}
			}
		} catch (IOException e) {
			Log.err("Generated IO Exception while generating tests from stdin");
			System.exit(0);
		}
		wrapper.close();
		Log.err("No counterexample found after " + i + " attempts");
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
	
}
