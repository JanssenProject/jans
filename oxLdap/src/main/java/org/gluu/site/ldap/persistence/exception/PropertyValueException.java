/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence.exception;

import org.xdi.util.StringHelper;

/**
 * Thrown when the (illegal) value of a property can not be persisted. There are
 * two main causes:
 * <ul>
 * <li>a property declared <tt>not-null="true"</tt> is null
 * </ul>
 */
public class PropertyValueException extends LdapMappingException {

	private static final long serialVersionUID = 6163726276249820546L;

	private final String entityName;
	private final String propertyName;

	public PropertyValueException(String s, String entityName, String propertyName) {
		super(s);
		this.entityName = entityName;
		this.propertyName = propertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + ": " + StringHelper.qualify(entityName, propertyName);
	}

}
