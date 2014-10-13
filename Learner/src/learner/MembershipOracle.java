package learner;

import sutInterface.SutWrapper;
import util.InputAction;
import util.OutputAction;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class MembershipOracle implements Oracle {
	private static final long serialVersionUID = -1374892499287788040L;
	private SutWrapper sutWrapper;

	public MembershipOracle(SutWrapper sutWrapper) {
		this.sutWrapper = sutWrapper;
	}

	//@Override
	public Word processQuery(Word query) throws LearningException {
		Word result = new WordImpl();
		System.out.println("LearnLib Query: " + query);

		sutWrapper.sendReset();
		InputAction input;
		OutputAction output;
		
		System.out.println("Member query number: " + ++Statistics.getStats().numMembQueries);

		for (Symbol currentSymbol : query.getSymbolList()) {
			System.out.println("Learning symbol: " + currentSymbol + " of query: " + query.getSymbolList() + " in MembershipOracle");
			input = new InputAction(currentSymbol.toString());
			
			System.out.println("Sending: " + input);

			output = sutWrapper.sendInput(input);
			System.out.println("Received: " + output.toString());

			result.addSymbol(new SymbolImpl(output.getValuesAsString()));
		}

		System.out.println("Returning to LearnLib: " + result);

		return result;
	}
}
