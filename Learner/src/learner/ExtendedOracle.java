package learner;

import util.exceptions.NonDeterminismException;
import de.ls5.jlearn.interfaces.Oracle;

public interface ExtendedOracle extends Oracle {
	/**
	 * Sends a single input String and returns an output string. This is done in the same run as
	 * the query. 
	 * @throws NonDeterminismException 
	 */
	public String sendInput(String input);
}
