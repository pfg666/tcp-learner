package abslearning.exceptions;

public class BugException extends RuntimeException{
	private static final long serialVersionUID = 3349908291398696155L;

	public BugException(String message) {
		super(message);
	}
}
