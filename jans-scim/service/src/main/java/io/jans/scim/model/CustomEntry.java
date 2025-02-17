/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.util.StringHelper;

/**
 * Entry with custom attributes and custom object classes lists
 * 
 * @author Yuriy Movchan Date: 07.05.2011
 */
public abstract class CustomEntry extends Entry implements Serializable, Cloneable {

	private static final long serialVersionUID = 5079582184398161111L;

	@CustomObjectClass
	private String[] customObjectClasses;

	public abstract List<JansCustomAttribute> getCustomAttributes();

	public abstract void setCustomAttributes(List<JansCustomAttribute> customAttributes);

	public String[] getCustomObjectClasses() {
		return customObjectClasses;
	}

	public void setCustomObjectClasses(String[] customObjectClasses) {
		this.customObjectClasses = customObjectClasses;
	}

	public String[] getAttributes(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		String[] values = null;
		for (JansCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				values = attribute.getValues();
				break;
			}
		}
		return values;
	}

	public String getAttribute(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		String value = null;
		for (JansCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				value = attribute.getValue();
				break;
			}
		}
		return value;
	}

	public String getAttribute(String attributeName, String defaultValue) {
		String result = getAttribute(attributeName);
		if (StringHelper.isEmpty(result)) {
			result = defaultValue;
		}

		return result;
	}

	public void setAttribute(String attributeName, String attributeValue) {
		setAttribute(new JansCustomAttribute(attributeName, attributeValue));
	}

	public void setAttribute(String attributeName, String[] attributeValue) {
		setAttribute(new JansCustomAttribute(attributeName, attributeValue));
	}

	public void setAttribute(JansCustomAttribute attribute) {
		List<JansCustomAttribute> customAttributes = getCustomAttributes();
		customAttributes.remove(attribute);
		customAttributes.add(attribute);
	}

	@Override
	public String toString() {
		return String.format("CustomEntry [customAttributes=%s, customObjectClasses=%s, toString()=%s]", getCustomAttributes(),
				Arrays.toString(customObjectClasses), super.toString());
	}

}
