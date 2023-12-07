/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.model;

import io.jans.model.JansCustomAttribute;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.DataEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Person with custom attributes
 * 
 * @author Yuriy Movchan Date: 07.13.2011
 */
@DataEntry
public class GluuSimplePerson extends CustomEntry implements Serializable {

	private static final long serialVersionUID = -2279582184398161100L;

	private String sourceServerName;

	@AttributesList(name = "name", value = "values", sortByName = true)
	private List<JansCustomAttribute> customAttributes = new ArrayList<JansCustomAttribute>();

	public List<JansCustomAttribute> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(List<JansCustomAttribute> customAttributes) {
		this.customAttributes = customAttributes;
	}

	public String getSourceServerName() {
		return sourceServerName;
	}

	public void setSourceServerName(String sourceServerName) {
		this.sourceServerName = sourceServerName;
	}

}
