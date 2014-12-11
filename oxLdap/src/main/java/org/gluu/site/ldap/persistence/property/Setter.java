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
 * Sets values to a particular property.
 */
public interface Setter extends Serializable {
	/**
	 * Set the property value from the given instance
	 * 
	 * @param target
	 *            The instance upon which to set the given value.
	 * @param value
	 *            The value to be set on the target.
	 * @throws LdapMappingException
	 */
	public void set(Object target, Object value) throws LdapMappingException;

	/**
	 * Optional operation (return null)
	 */
	public String getMethodName();

	/**
	 * Optional operation (return null)
	 */
	public Method getMethod();
}
