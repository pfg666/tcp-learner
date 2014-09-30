package learner;

import java.util.ArrayList;
import java.util.List;

import sutInterface.ActionSignature;
import sutInterface.SutInfo;
import sutInterface.SutWrapper;
import util.InputAction;
import util.OutputAction;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Alphabet;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.AlphabetImpl;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class EquivalenceOracle implements Oracle {
	private static final long serialVersionUID = -5409624854115451929L;
	private SutWrapper sutWrapper;
	private MembershipOracle memberOracle;
	private int numEquivQueries = 0;

	public EquivalenceOracle(SutWrapper sutWrapper) {
		this.sutWrapper = sutWrapper;
	}

	public Word processQuery(Word query) throws LearningException {
		Word result = new WordImpl();
		System.out.println("LearnLib Query: " + query);

		sutWrapper.sendReset();

		InputAction input;
		OutputAction output;

		numEquivQueries++;
		System.out.println("Equivalence query number: " + numEquivQueries);

		for (Symbol currentSymbol : query.getSymbolList()) {
			System.out.println("Learning symbol: " + currentSymbol
					+ " of query: " + query.getSymbolList() + " in Mapper");
			input = new InputAction(currentSymbol.toString());
			System.out.println("Sending: " + input);

			output = sutWrapper.sendInput(input);
			System.out.println("Received: " + output.toString());

			result.addSymbol(new SymbolImpl(output.getValuesAsString()));
		}

		System.out.println("Returning to LearnLib: " + result);
		return result;
	}

	public Alphabet generateInputAlphabet() {
		Alphabet result = new AlphabetImpl();

		for (ActionSignature sig : SutInfo.getInputSignatures()) {
			List<String> currentAlpha = new ArrayList<String>();
			currentAlpha.add(sig.getMethodName());
			for (String currentSymbol : currentAlpha) {
				result.addSymbol(new SymbolImpl(currentSymbol));
			}
		}
		return result;
	}

	public Alphabet generateOutputAlphabet() {
		Alphabet result = new AlphabetImpl();

		for (ActionSignature sig : SutInfo.getOutputSignatures()) {
			result.addSymbol(new SymbolImpl(sig.getMethodName()));
		}
		return result;
	}

	public void setMembershipOracle(MembershipOracle memberOracle) {
		this.memberOracle = memberOracle;
	}

	public MembershipOracle getMembershipOracle() {
		return memberOracle;
	}

	public int getNumEquivQueries() {
		return numEquivQueries;
	}
}
