package learner;

import sutInterface.SutWrapper;
import util.InputAction;
import util.OutputAction;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class EquivalenceOracle implements ExtendedOracle {
	private static final long serialVersionUID = -5409624854115451929L;
	private SutWrapper sutWrapper;

	public EquivalenceOracle(SutWrapper sutWrapper) {
		this.sutWrapper = sutWrapper;
	}

	public Word processQuery(Word query) throws LearningException {
		Word result = new WordImpl();
		System.out.println("LearnLib Query: " + query);

		sutWrapper.sendReset();

		System.out.println("Equivalence query number: " + ++Statistics.getStats().totalEquivQueries);

		for (Symbol currentSymbol : query.getSymbolList()) {
			String outputString = sendInput(currentSymbol.toString());
			result.addSymbol(new SymbolImpl(outputString));
		}

		System.out.println("Returning to LearnLib: " + result);
		return result;
	}

	public String sendInput(String inputString) {
		InputAction input = new InputAction(inputString);
		System.out.println("Sending: " + inputString);

		OutputAction output = sutWrapper.sendInput(input);
		if (output != null) {
			String outputString = output.getValuesAsString();
			System.out.println("Received: " + outputString);
			return outputString;
		} else {
			return null;
		}
		
	}
}
