/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * User
 * 
 * @author Yuriy Movchan Date: 06/10/2013
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansPerson")
public class User extends CustomEntry implements Serializable, Cloneable {

	private static final long serialVersionUID = -7779582184398161112L;

	@AttributesList(name = "name", value = "values", sortByName = true, multiValued = "multiValued",
			attributesConfiguration = {
			 @AttributeName(name = "inum", ignoreDuringUpdate = true),
			 @AttributeName(name = "uid", ignoreDuringUpdate = false), @AttributeName(name = "userPassword", ignoreDuringRead = true) })
	protected List<JansCustomAttribute> customAttributes = new ArrayList<JansCustomAttribute>();

	public List<JansCustomAttribute> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(List<JansCustomAttribute> customAttributes) {
		this.customAttributes = customAttributes;
	}

	public String getInum() {
		return getAttribute("inum");
	}

	public void setInum(String value) {
		setAttribute("inum", value);
	}

	public String getUid() {
		return getAttribute("uid");
	}

	public void setUid(String value) {
		setAttribute("uid", value);
	}

	public String getDisplayName() {
		return getAttribute("displayName");
	}

	public void setDisplayName(String value) {
		setAttribute("displayName", value);
	}
 
}
