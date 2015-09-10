package util.exceptions;

import java.util.List;

import de.ls5.jlearn.interfaces.Symbol;

public class InconsistencyException extends Exception {
	private static final long serialVersionUID = 14324L;

	public InconsistencyException(Object oldObj, Object newObj) {
		super("previously encountered\n" +
				oldObj + "\nNow encountering\n" + newObj);
	}
}
