package tcp.learner;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;


public class TCPOracleWrapper implements Oracle {

	private static final long serialVersionUID = 1L;
	private Oracle basicOracle;
	public TCPOracleWrapper(Oracle oracle) {
		this.basicOracle = oracle;
	}
	
	
	@Override
	public Word processQuery(Word word) throws LearningException {
		return basicOracle.processQuery(word);
	}
}
