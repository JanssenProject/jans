/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence.property;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.gluu.site.ldap.persistence.exception.LdapMappingException;

/**
 * Gets values of a particular property
 */
public interface Getter extends Serializable {
	/**
	 * Get the property value from the given instance.
	 * 
	 * @param owner
	 *            The instance containing the value to be retreived.
	 * @return The extracted value.
	 * @throws LdapMappingException
	 */
	public Object get(Object owner) throws LdapMappingException;

	/**
	 * Get the declared Java type
	 */
	public Class<?> getReturnType();

	/**
	 * Optional operation (return null)
	 */
	public String getMethodName();

	/**
	 * Optional operation (return null)
	 */
	public Method getMethod();
}
