package org.xdi.util.exception;

/**
 * @author Yuriy Movchan Date: 10.25.2010
 */
public class InvalidAttributeException extends RuntimeException {

	private static final long serialVersionUID = -3072969232087073304L;

	public InvalidAttributeException(Throwable root) {
		super(root);
	}

	public InvalidAttributeException(String string, Throwable root) {
		super(string, root);
	}

	public InvalidAttributeException(String s) {
		super(s);
	}

}
