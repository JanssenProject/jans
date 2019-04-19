/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model;

import java.util.Arrays;

import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;

/**
 * LDAP Attribute
 *
 * @author Yuriy Movchan Date: 10.10.2010
 */
public class AttributeData {
    private final String name;
    private final Object[] values;

    public AttributeData(String name, Object[] values) {
        this.name = name;
        this.values = values;
    }

    public AttributeData(String name, Object value) {
        this.name = name;
        this.values = new Object[1];
        this.values[0] = value;
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
        
        if (this.values.length == 0) {
        	return new String[0];
        }
        
        String[] result = new String[this.values.length];
        
        for (int i = 0; i < result.length; i++) {
        	result[i] = String.valueOf(this.values[i]);
        }

        return result;
    }

    public Object getValue() {
        if ((this.values == null) || (this.values.length == 0)) {
            return null;
        }

        return this.values[0];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AttributeData other = (AttributeData) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (!ArrayHelper.equalsIgnoreOrder(values, other.values)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("Attribute [name=%s, values=%s]", name, Arrays.toString(values));
    }

}
