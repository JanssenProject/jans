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
        this.values = values;
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
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
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
        return String.format("Attribute [name=%s, values=%s]", name, values);
    }

    public int compareTo(CustomObjectAttribute o) {
        return name.compareTo(o.name);
    }
}
