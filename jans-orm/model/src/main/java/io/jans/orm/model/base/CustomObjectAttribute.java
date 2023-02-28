/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

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
    }

    public CustomObjectAttribute(String name, List<Object> values) {
        this.name = name;
        setValues(values);
    }

    public Object getValue() {
        if (this.values == null) {
            return null;
        }

        if (!this.values.isEmpty()) {
            return this.values.get(0);
        }

        return null;
    }

    public void setValue(Object value) {
        this.values = new ArrayList<>();
        this.values.add(value);
        this.multiValued = false;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
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

	public CustomObjectAttribute setMultiValued(boolean multiValued) {
		this.multiValued = multiValued;
		
		return this;
	}

	public String getDisplayValue() {
        if (values == null) {
            return "";
        }

        if (values.size() == 1) {
            return (values.get(0)!=null ? values.get(0).toString() : "");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if(i==0) {
                sb.append(values.get(i).toString());
            }else {
                sb.append(", ").append(values.get(i).toString());
            }
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

	public CustomObjectAttribute multiValued() {
		this.multiValued = true;
		
		return this;
	}
}
