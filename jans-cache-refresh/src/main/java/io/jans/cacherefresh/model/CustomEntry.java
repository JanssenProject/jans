/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.model;

import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.model.base.Entry;
import io.jans.util.StringHelper;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Entry with custom attributes and custom object classes lists
 * 
 * @author Yuriy Movchan Date: 07.05.2011
 */
public abstract class CustomEntry extends Entry implements Serializable, Cloneable {

	private static final long serialVersionUID = 5079582184398161111L;

	@CustomObjectClass
	private String[] customObjectClasses;

	public abstract List<GluuCustomAttribute> getCustomAttributes();

	public abstract void setCustomAttributes(List<GluuCustomAttribute> customAttributes);

	public String[] getCustomObjectClasses() {
		return customObjectClasses;
	}

	public void setCustomObjectClasses(String[] customObjectClasses) {
		this.customObjectClasses = customObjectClasses;
	}

	public Object[] getAttributes(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		Object[] values = null;
		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				values = attribute.getValues();
				break;
			}
		}
		return values;
	}

	public Object getAttribute(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		Object value = null;
		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				value = attribute.getValue();
				break;
			}
		}
		return value;
	}

	public Object getAttribute(String attributeName, Object defaultValue) {
		Object result = getAttribute(attributeName);
		if (StringHelper.isEmptyString(result)) {
			result = defaultValue;
		}

		return result;
	}

	public String[] getStringAttributes(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		String[] values = null;
		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				values = attribute.getStringValues();
				break;
			}
		}
		return values;
	}

	public String getStringAttribute(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		String value = null;
		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				value = attribute.getStringValue();
				break;
			}
		}
		return value;
	}

	public GluuCustomAttribute getCustomAttribute(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				return attribute;
			}
		}
		
		return null;
	}

    public String[] getAttributeStringValues(String attributeName) {
    	GluuCustomAttribute customAttribute = getCustomAttribute(attributeName);
    	if (customAttribute == null) {
    		return null;
    	}
    	
    	return customAttribute.getStringValues();
    }

	public void setAttribute(String attributeName, String attributeValue) {
		setAttribute(new GluuCustomAttribute(attributeName, attributeValue));
	}

	public void setAttribute(String attributeName, String[] attributeValue) {
		setAttribute(new GluuCustomAttribute(attributeName, attributeValue));
	}

	public void setAttribute(GluuCustomAttribute attribute) {
		List<GluuCustomAttribute> customAttributes = getCustomAttributes();
		customAttributes.remove(attribute);
		customAttributes.add(attribute);
	}

	@Override
	public String toString() {
		return String.format("CustomEntry [customAttributes=%s, customObjectClasses=%s, toString()=%s]", getCustomAttributes(),
				Arrays.toString(customObjectClasses), super.toString());
	}

}
