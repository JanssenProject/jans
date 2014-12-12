/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.ldap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapAttributesList;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Movchan Date: 04/08/2014
 */
@LdapEntry
@LdapObjectClass(values = { "top" })
public class CustomEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = -7686468010219068788L;

	@LdapAttributesList(name = "name", value = "values", sortByName = true)
	private List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

	public List<CustomAttribute> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(List<CustomAttribute> customAttributes) {
		this.customAttributes = customAttributes;
	}

}