package learner.exceptions;

public class OutOfTimeException extends RuntimeException{
	private static final long serialVersionUID = 3349908291398696155L;

	public OutOfTimeException(String message) {
		super(message);
	}
}

