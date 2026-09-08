/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import io.jans.orm.util.StringHelper;

/**
 * LDAP Attribute
 *
 * @author Yuriy Movchan Date: 10.10.2010
 */
public class AttributeData {
    private final String name;
    private final Object[] values;
    private Boolean multiValued;
    private Boolean jsonValue;
    private Boolean binaryValue;

    public AttributeData(String name, Object[] values) {
    	this(name, values, null);
    }

    public AttributeData(String name, Object[] values, Boolean multiValued) {
        this.name = name;
        this.values = values;
        this.multiValued = multiValued;
    }

    public AttributeData(String name, Object[] values, Boolean multiValued, Boolean jsonValue) {
        this.name = name;
        this.values = values;
        this.multiValued = multiValued;
        this.jsonValue = jsonValue;
    }

    public AttributeData(String name, Object[] values, Boolean multiValued, Boolean jsonValue, Boolean binaryValue) {
        this.name = name;
        this.values = values;
        this.multiValued = multiValued;
        this.jsonValue = jsonValue;
        this.binaryValue = binaryValue;
    }

    public AttributeData(String name, Object value) {
        this.name = name;
        this.values = new Object[1];
        this.values[0] = value;
        this.multiValued = null;
    }

    public AttributeData(String name, Object value, Boolean multiValued, Boolean jsonValue) {
        this.name = name;
        this.values = new Object[1];
        this.values[0] = value;
        this.multiValued = multiValued;
        this.jsonValue = jsonValue;
    }

    public AttributeData(String name, Object value, Boolean multiValued, Boolean jsonValue, Boolean binaryValue) {
        this.name = name;
        this.values = new Object[1];
        this.values[0] = value;
        this.multiValued = multiValued;
        this.jsonValue = jsonValue;
        this.binaryValue = binaryValue;
    }

    public final String getName() {
        return name;
    }

    public final Object[] getValues() {
        return values;
    }

    public final String[] getStringValues() {
    	if (this.values == null) {
    		return null;
    	}

    	// Binary values should be converted to base64 strings for backends without native binary support
    	boolean hasBinaryValues = false;
		for (Object value : this.values) {
			if (value instanceof byte[]) {
				hasBinaryValues = true;
				break;
			}
		}

		if (hasBinaryValues) {
	    	String[] result = new String[this.values.length];
	    	for (int i = 0; i < this.values.length; i++) {
	    		if (this.values[i] instanceof byte[]) {
	    			result[i] = Base64.encodeBase64String((byte[]) this.values[i]);
	    		} else {
	    			result[i] = (this.values[i] == null) ? null : String.valueOf(this.values[i]);
	    		}
	    	}

	    	return result;
		}

    	return StringHelper.toStringArray(this.values);
    }

    public Object getValue() {
        if ((this.values == null) || (this.values.length == 0)) {
            return null;
        }

        return this.values[0];
    }

	public void setMultiValued(Boolean multiValued) {
		this.multiValued = multiValued;
	}

	public Boolean getMultiValued() {
		return multiValued;
	}

	public Boolean getJsonValue() {
		return jsonValue;
	}

	public void setJsonValue(Boolean jsonValue) {
		this.jsonValue = jsonValue;
	}

	public Boolean getBinaryValue() {
		return binaryValue;
	}

	public void setBinaryValue(Boolean binaryValue) {
		this.binaryValue = binaryValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.deepHashCode(values);
		return result;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		AttributeData other = (AttributeData) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if ((values == null) && (other.values == null)) {
			return true;
		}
		
		if ((values == null) || (other.values == null) || values.length != other.values.length) {
			return false;
		}
		
		for (int i = 0; i < values.length; i++) {
			if (!equalsValue(values[i], other.values[i])) {
				return false;
			}
		}

		return true;
	}

	private static boolean equalsValue(Object value1, Object value2) {
		if ((value1 instanceof byte[]) || (value2 instanceof byte[])) {
			if ((value1 instanceof byte[]) && (value2 instanceof byte[])) {
				return Arrays.equals((byte[]) value1, (byte[]) value2);
			}

			// One value is byte[] and another one is base64 string when DB stores binary data in string column
			byte[] binaryValue = (value1 instanceof byte[]) ? (byte[]) value1 : (byte[]) value2;
			Object stringValue = (value1 instanceof byte[]) ? value2 : value1;
			if (stringValue instanceof String) {
				return StringHelper.equals(Base64.encodeBase64String(binaryValue), (String) stringValue);
			}

			return false;
		}

		return StringHelper.equals(String.valueOf(value1), String.valueOf(value2));
	}


    @Override
	public String toString() {
		return "AttributeData [name=" + name + ", values=" + Arrays.toString(values) + ", multiValued=" + multiValued
				+ ", jsonValue=" + jsonValue + ", binaryValue=" + binaryValue + "]";
	}

}
