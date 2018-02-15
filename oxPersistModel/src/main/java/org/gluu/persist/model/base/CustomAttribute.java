/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Javier Rojas Date: 12.5.2011
 * @author Yuriy Movchan Date: 04/08/2014
*/
public class CustomAttribute implements Serializable, Comparable<CustomAttribute> {

    private static final long serialVersionUID = 1468450094325306154L;

    private static final Logger LOG = LoggerFactory.getLogger(CustomAttribute.class);

    private String name;
    private List<String> values;

    public CustomAttribute() {
    }

    public CustomAttribute(String name) {
        this.name = name;
    }

    public CustomAttribute(String name, String value) {
        this.name = name;
        setValue(value);
    }

    public CustomAttribute(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    public String getValue() {
        if (this.values == null) {
            return null;
        }

        if (this.values.size() > 0) {
            return this.values.get(0);
        }

        return null;
    }

    public void setValue(String value) {
        this.values = new ArrayList<String>();
        this.values.add(value);
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
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
            return values.get(0);
        }

        StringBuilder sb = new StringBuilder(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            sb.append(", ").append(values.get(i));
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

        CustomAttribute that = (CustomAttribute) o;

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

    public int compareTo(CustomAttribute o) {
        return name.compareTo(o.name);
    }
}
