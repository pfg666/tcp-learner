package abslearning.learner;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import sut.action.InputAction;
import sut.action.OutputAction;
import sut.action.Parameter;
import sut.info.SutInfo;
import sut.info.SutInterface;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;


public class BasicEquivalenceOracle implements Oracle {
	private static final Statistics statistics= Statistics.getInstance();
	private static final Logger logger = Logger.getLogger(BasicEquivalenceOracle.class);
	private static final Logger logger_equivQueries = Logger.getLogger("equivQueries");	
	
	private static final long serialVersionUID = -5409624854115451929L;
	private SutInterface sutWrapper;
	private int numEquivQueries = 0;

	public BasicEquivalenceOracle() {
		sutWrapper = SutInfo.newSutWrapper(true);		
	}

	public Word processQuery(Word query) throws LearningException {
		if ( logger_equivQueries.isDebugEnabled() ) logger_equivQueries.debug("\nEquivalence query " + numEquivQueries + ": "  + query);
		logger.debug("LearnLib Query: " + query);
		
		Word result = new WordImpl();

		
		
		sutWrapper.sendReset();

		InputAction input;
		OutputAction output;

		statistics.incEquivQueries();
		numEquivQueries++;
		
		logger.debug("Equivalence query number: " + numEquivQueries);
		

		for (Symbol currentSymbol : query.getSymbolList()) {
			logger.debug("Learning symbol: " + currentSymbol + " of Equivalence query: " + query.getSymbolList() );
			
			input = new InputAction(currentSymbol.toString(),new ArrayList<Parameter>());
			
			logger.debug("Sending: " + input);

			output = sutWrapper.sendInput(input);
			logger.debug("Received: " + output.toString());

			result.addSymbol(new SymbolImpl(output.serialize()));
		}

		if ( logger.isDebugEnabled() ) logger.debug("Returning to LearnLib: " + result);		
		if ( logger_equivQueries.isDebugEnabled() ) logger_equivQueries.debug("Output: " + result);	
		return result;
	}
}
