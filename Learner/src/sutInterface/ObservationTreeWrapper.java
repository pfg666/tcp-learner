package sutInterface;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

import util.LearnlibUtils;
import util.ObservationTree;
import util.exceptions.InconsistencyException;
import util.exceptions.NonDeterminismException;

/**
 * Creates a consistent observation tree. Implies that it checks for non-determinism.
 * @author Ramon
 */
public class ObservationTreeWrapper implements Oracle, Serializable {
	private static final long serialVersionUID = 71137973L;
	
	private final Oracle oracle;
	private final ObservationTree tree;
	
	public ObservationTreeWrapper(Oracle oracle) {
		this(new ObservationTree(), oracle);
	}
	
	public ObservationTreeWrapper(ObservationTree tree, Oracle oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output = this.oracle.processQuery(input);
		try {
			this.tree.addObservation(input, output);
			return output;
		} catch (InconsistencyException e) {
			throw new NonDeterminismException("Non-determinism detected for input\n" + input.getSymbolList() + "\n" + e.getMessage());
		}
	}
	
	public ObservationTree getObservationTree() {
		return tree;
	}
}
