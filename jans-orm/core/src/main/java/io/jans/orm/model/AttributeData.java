/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

import java.util.Arrays;

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

    public AttributeData(String name, Object[] values) {
    	this(name, values, null);
    }

    public AttributeData(String name, Object[] values, Boolean multiValued) {
        this.name = name;
        this.values = values;
        this.multiValued = multiValued;
    }

    public AttributeData(String name, Object value) {
        this.name = name;
        this.values = new Object[1];
        this.values[0] = value;
        this.multiValued = null;
    }

    public final String getName() {
        return name;
    }

    public final Object[] getValues() {
        return values;
    }

    public final String[] getStringValues() {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((multiValued == null) ? 0 : multiValued.hashCode());
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
		if ((multiValued != null) && (other.multiValued != null)) {
			if (!multiValued.equals(other.multiValued))
				return false;
		}
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
			if (!StringHelper.equals(String.valueOf(values[i]), String.valueOf(other.values[i]))) {
				return false;
			}
		}

		return true;
	}

    @Override
	public String toString() {
		return "AttributeData [name=" + name + ", values=" + Arrays.toString(values) + ", multiValued=" + multiValued + "]";
	}

}
