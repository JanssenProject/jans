package org.gluu.site.ldap.persistence.exception;

/**
 * The base {@link Throwable} type for LDAP Mapping.
 */
public class LdapMappingException extends RuntimeException {

	private static final long serialVersionUID = 1071769232087073304L;

	public LdapMappingException(Throwable root) {
		super(root);
	}

	public LdapMappingException(String string, Throwable root) {
		super(string, root);
	}

	public LdapMappingException(String s) {
		super(s);
	}

}
