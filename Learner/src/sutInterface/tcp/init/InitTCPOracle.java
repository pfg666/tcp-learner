package sutInterface.tcp.init;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;


public class InitTCPOracle implements Oracle {
	private Oracle basicOracle;
	public InitTCPOracle(Oracle oracle) {
		this.basicOracle = oracle;
	}
	
	
	@Override
	public Word processQuery(Word arg0) throws LearningException {
		// TODO Auto-generated method stub
		return null;
	}
}
