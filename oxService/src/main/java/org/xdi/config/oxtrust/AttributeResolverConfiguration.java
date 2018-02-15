/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.config.oxtrust;

import javax.enterprise.inject.Vetoed;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Attribute resolver configuration
 * 
 * @author Yuriy Movchan Date: 09/04/2017
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeResolverConfiguration implements Configuration {

	private String attributeBase;
	private String attributeName;
	private String nameIdType;
	private boolean enabled;

	public String getAttributeBase() {
		return attributeBase;
	}

	public void setAttributeBase(String attributeBase) {
		this.attributeBase = attributeBase;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getNameIdType() {
		return nameIdType;
	}

	public void setNameIdType(String nameIdType) {
		this.nameIdType = nameIdType;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
