/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Simple custom property to hold key/value/description
 *
 * @author Yuriy Movchan Date: 02/08/2011
 */
@XmlRootElement
@JsonPropertyOrder({ "value1", "value2", "description" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleCustomProperty implements Serializable {

    private static final long serialVersionUID = -1451889014702205980L;

    private String value1;
    private String value2;
    private String description;

    public SimpleCustomProperty() {
        this("", "");
    }

    public SimpleCustomProperty(String value1, String value2) {
        this(value1, value2, "");
    }

    public SimpleCustomProperty(String value1, String value2, String description) {
        this.value1 = value1;
        this.value2 = value2;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public final String getValue1() {
        return value1;
    }

    public final void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
        result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
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
        SimpleCustomProperty other = (SimpleCustomProperty) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (value1 == null) {
            if (other.value1 != null) {
                return false;
            }
        } else if (!value1.equals(other.value1)) {
            return false;
        }
        if (value2 == null) {
            if (other.value2 != null) {
                return false;
            }
        } else if (!value2.equals(other.value2)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("SimpleCustomProperty [value1=%s, value2=%s, description=%s]", value1, value2,
                description);
    }

}
