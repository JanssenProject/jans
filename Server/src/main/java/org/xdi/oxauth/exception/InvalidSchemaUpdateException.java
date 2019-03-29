/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.exception;

/**
 * @author Yuriy Movchan Date: 10.15.2010
 */
public class InvalidSchemaUpdateException extends RuntimeException {

	private static final long serialVersionUID = 3071969232087073304L;

	public InvalidSchemaUpdateException(Throwable root) {
		super(root);
	}

	public InvalidSchemaUpdateException(String string, Throwable root) {
		super(string, root);
	}

	public InvalidSchemaUpdateException(String s) {
		super(s);
	}

}
