package org.gluu.site.ldap.persistence.exception;

/**
 * Indicates that an expected getter or setter method could not be found on a
 * class.
 */
public class PropertyNotFoundException extends MappingException {

	private static final long serialVersionUID = 2351260797243441135L;

	public PropertyNotFoundException(String s) {
		super(s);
	}

}
