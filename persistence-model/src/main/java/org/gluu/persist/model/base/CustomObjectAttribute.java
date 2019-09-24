/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Movchan Date: 09/16/2019
*/
public class CustomObjectAttribute implements Serializable, Comparable<CustomObjectAttribute> {

    private static final long serialVersionUID = -1238450094325306154L;

    private String name;
    private boolean multiValued;
    private List<Object> values;

    public CustomObjectAttribute() {
    }

    public CustomObjectAttribute(String name) {
        this.name = name;
    }

    public CustomObjectAttribute(String name, Object value) {
        this.name = name;
        setValue(value);
        this.multiValued = false;
    }

    public CustomObjectAttribute(String name, List<Object> values) {
        this.name = name;
        this.values = values;
        
        this.multiValued = (values != null) && (values.size() > 1); 
    }

    public Object getValue() {
        if (this.values == null) {
            return null;
        }

        if (this.values.size() > 0) {
            return this.values.get(0);
        }

        return null;
    }

    public void setValue(Object value) {
        this.values = new ArrayList<Object>();
        this.values.add(value);
        this.multiValued = false;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
        this.multiValued = true;

        this.multiValued = (values != null) && (values.size() > 1); 
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(boolean multiValued) {
		this.multiValued = multiValued;
	}

	public String getDisplayValue() {
        if (values == null) {
            return "";
        }

        if (values.size() == 1) {
            return values.get(0).toString();
        }

        StringBuilder sb = new StringBuilder(values.get(0).toString());
        for (int i = 1; i < values.size(); i++) {
            sb.append(", ").append(values.get(i).toString());
        }

        return sb.toString();
    }

	private String toStringValue() {
        if (values == null) {
            return "";
        }

        if (values.size() == 1) {
        	if (multiValued) {
        		return "[" + values.get(0).toString() + "]";
        	}
            return values.get(0).toString();
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
        	if (i > 0) {
        		sb.append(", ");
        	}
            sb.append(values.get(i).toString());
        }
        sb.append("]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomObjectAttribute that = (CustomObjectAttribute) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("Attribute [name=%s, multiValued=%s, value=%s]", name, multiValued, toStringValue());
    }

    public int compareTo(CustomObjectAttribute o) {
        return name.compareTo(o.name);
    }
}
