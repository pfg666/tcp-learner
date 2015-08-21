package sutInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Counter;
import util.Log;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class ProbablisticOracle implements Oracle {
	private static final long serialVersionUID = 165336527L;
	private final Oracle oracle;
	private final int minimumAttempts, maximumAttempts;
	private final double minimumFraction;
	
	public ProbablisticOracle(Oracle oracle, int minimumAttempts, double minimumFraction, int maximumAttempts) {
		this.oracle = oracle;
		if (minimumAttempts > maximumAttempts) {
			throw new RuntimeException("minimum number of attempts should not be greater than maximum");
		}
		if (minimumFraction > 1 || minimumFraction < 0) {
			throw new RuntimeException("Minimum fraction should be in interval [0,1]");
		}
		this.minimumAttempts = minimumAttempts;
		this.minimumFraction = minimumFraction;
		this.maximumAttempts = maximumAttempts;
	}

	@Override
	public Word processQuery(Word inputWord) throws LearningException {
		List<Symbol> input = inputWord.getSymbolList();
		Counter<List<Symbol>> responseCounter = new Counter<List<Symbol>>();
		do {
			if (responseCounter.getTotalNumber() >= this.maximumAttempts) {
				throw new LearningException("Too much non-determinism: could not agree on input\n" +
						input + "\nResponses:\n" + responseCounter) {
					private static final long serialVersionUID = 123423L;
				};
			}
			List<Symbol> output = this.oracle.processQuery(inputWord).getSymbolList();
			responseCounter.count(output);
		} while (responseCounter.getTotalNumber() < this.minimumAttempts
				|| responseCounter.getHighestFrequencyFraction() < this.minimumFraction);
		if (responseCounter.getObjectsCounted() > 1) {
			Log.err("Non-determinism detected on input\n" + input + "\nResponses:\n" + responseCounter + "\naccepted most frequent.");
		} else {
			Log.err("Concluded unanimously in " + responseCounter.getObjectsCounted() + " attempts.");
		}
		return new WordImpl((Symbol[])responseCounter.getMostFrequent().toArray());
	}
}
