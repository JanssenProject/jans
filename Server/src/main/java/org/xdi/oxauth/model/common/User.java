/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.util.Arrays;
import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Movchan Date: 06/11/2013
 */
@LdapEntry
@LdapObjectClass(values = { "top", "gluuPerson" })
public class User extends SimpleUser {

    private static final long serialVersionUID = 6634191420188575733L;

	public void setAttribute(String attributeName, String attributeValue) {
		CustomAttribute attribute = new CustomAttribute(attributeName, attributeValue);
		getCustomAttributes().remove(attribute);
		getCustomAttributes().add(attribute);
	}

	public void setAttribute(String attributeName, String[] attributeValues) {
		CustomAttribute attribute = new CustomAttribute(attributeName, Arrays.asList(attributeValues));
		getCustomAttributes().remove(attribute);
		getCustomAttributes().add(attribute);
	}

	public void setAttribute(String attributeName, List<String> attributeValues) {
		CustomAttribute attribute = new CustomAttribute(attributeName, attributeValues);
		getCustomAttributes().remove(attribute);
		getCustomAttributes().add(attribute);
	}

}