package util.exceptions;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.WordImpl;

public class NonDeterminismException extends LearningException {
	private static final long serialVersionUID = 9265414L;
	private final Word inputs;
	
	public NonDeterminismException(Word inputs) {
		super();
		this.inputs = inputs;
	}
	
	public NonDeterminismException(String message, Word inputs) {
		super(message);
		this.inputs = inputs;
	}
	
	public Word getInputs() {
		return this.inputs;
	}
}
