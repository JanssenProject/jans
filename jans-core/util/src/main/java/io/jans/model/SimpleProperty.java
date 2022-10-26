/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Simple property to hold value
 *
 * @author Yuriy Movchan Date: 08.02.2011
 */
@XmlRootElement
@JsonPropertyOrder({ "value" })
public class SimpleProperty implements Serializable {

    private static final long serialVersionUID = -1451889014702205980L;

    private String value;

    public SimpleProperty() {
    }

    public SimpleProperty(String value) {
        this.value = value;
    }

    public final String getValue() {
        return value;
    }

    public final void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        SimpleProperty other = (SimpleProperty) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SimpleProperty [value=" + value + "]";
    }

}
