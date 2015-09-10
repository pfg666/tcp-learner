package util.exceptions;

import de.ls5.jlearn.abstractclasses.LearningException;

public class NonDeterminismException extends LearningException {
	private static final long serialVersionUID = 9265414L;
	
	public NonDeterminismException() {
		super();
	}
	
	public NonDeterminismException(String message) {
		super(message);
	}
}
