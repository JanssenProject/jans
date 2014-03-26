package org.gluu.site.ldap.persistence.exception;

/**
 * An exception is a result of something wrong in input parameters.
 */
public class InvalidArgumentException extends LdapMappingException {

	private static final long serialVersionUID = -2223352885909511209L;

	public InvalidArgumentException(String msg, Throwable root) {
		super(msg, root);
	}

	public InvalidArgumentException(Throwable root) {
		super(root);
	}

	public InvalidArgumentException(String s) {
		super(s);
	}

}
