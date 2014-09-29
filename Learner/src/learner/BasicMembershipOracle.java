package learner;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import sut.info.SutInfo;
import sut.info.SutInterface;

import sut.action.InputAction;
import sut.action.OutputAction;
import sut.action.Parameter;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class BasicMembershipOracle implements Oracle {
	private static final Statistics statistics= Statistics.getInstance();
	private static final Logger logger = Logger.getLogger(BasicMembershipOracle.class);
	private static final Logger logger_memQueries = Logger.getLogger("memQueries");
	
	
	private static final long serialVersionUID = -1374892499287788040L;
	private SutInterface sutWrapper;
	private int numMembQueries = 0;

	public BasicMembershipOracle() {
		sutWrapper = SutInfo.newSutWrapper(true);
	}

	
	// @Override
	public Word processQuery(Word query) throws LearningException {
		
		
		if ( logger_memQueries.isDebugEnabled() ) logger_memQueries.debug("\nMembership query " + numMembQueries + ": "  + query);
				
		logger.debug("LearnLib MQuery: " + query);
		
		Word result = new WordImpl();
		sutWrapper.sendReset();
		InputAction input;
		OutputAction output;


		numMembQueries++;
		statistics.incMemQueries();
		logger.debug("Member query number: " + numMembQueries);		

		for (Symbol currentSymbol : query.getSymbolList()) {
			logger.debug("Learning symbol: " + currentSymbol + " of query: " + query.getSymbolList() + " in MembershipOracle");
			
			input = new InputAction(currentSymbol.toString(),new ArrayList<Parameter>());
			
			logger.debug("Sending: " + input);
			output = sutWrapper.sendInput(input);
			logger.debug("Received: " + output.toString());

			result.addSymbol(new SymbolImpl(output.serialize()));
		}

		if ( logger.isDebugEnabled() ) logger.debug("Returning to LearnLib: " + result);
		if ( logger_memQueries.isDebugEnabled() )  logger_memQueries.debug("Output: " + result);
		return result;
	}

	// 
	public boolean isValidCounterExample(EquivalenceOracleOutput counterexample)  {

		Word ceInputs = counterexample.getCounterExample(); // get inputs of counterexample
		Word ceOutputs = counterexample.getOracleOutput(); // get outputs of counterexample

		sutWrapper.sendReset();
		InputAction input;
		OutputAction output;

		for (int i = 0; i < ceInputs.size(); i++) {

			Symbol currentSymbol = ceInputs.getSymbolByIndex(i);
			input = new InputAction(currentSymbol.toString(),new ArrayList<Parameter>());			
			logger.debug("Sending: " + input);
			output = sutWrapper.sendInput(input);
			logger.debug("Received: " + output.toString());


			Symbol outputSut = new SymbolImpl(output.toString());
			Symbol outputCE = ceOutputs.getSymbolByIndex(i);
			if (outputSut.equals(outputCE)) {
				continue;
			}
			logger.info("Valid Counterexample at index " + i +  " at input '" + ceInputs.getSymbolByIndex(i).toString()  + " different outputs are :  " + outputSut + "(sut) versus " + outputCE + "(ce)");
			return true;
		}
		logger.info("Invalid Counterexample: sut gives same output as outpus in counterexample");
		return false;
		

	}


}
