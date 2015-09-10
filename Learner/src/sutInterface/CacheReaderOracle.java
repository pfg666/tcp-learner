package sutInterface;

import util.ObservationTree;
import util.StringColorizer;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class CacheReaderOracle implements Oracle {
	private final Oracle oracle;
	private final ObservationTree tree;
	
	public CacheReaderOracle(ObservationTree tree, Oracle oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}
	
	@Override
	public Word processQuery(Word input) throws LearningException {
		Word cachedOutput = tree.getObservation(input);
		if (cachedOutput != null) {
			System.out.println(StringColorizer.toColor("Cache hit for " + input.getSymbolList() + " -> " + cachedOutput.getSymbolList(), StringColorizer.TextColor.CYAN));
			return cachedOutput;
		} else {
			return oracle.processQuery(input);
		}
	}
}
