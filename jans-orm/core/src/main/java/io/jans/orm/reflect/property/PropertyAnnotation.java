/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.reflect.property;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Hold property with their annotations
 *
 * @author Yuriy Movchan Date: 04.14.2011
 */
public class PropertyAnnotation implements Comparable<PropertyAnnotation>, Serializable {

    private static final long serialVersionUID = 4620529664753916995L;

    private final String propertyName;
    private final transient List<Annotation> annotations;

    private transient Class<?> parameterType;

    public PropertyAnnotation(String property, List<Annotation> annotations) {
        this.propertyName = property;
        this.annotations = annotations;
        this.parameterType = null;
    }

	public String getPropertyName() {
        return propertyName;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public Class<?> getParameterType() {
		return parameterType;
	}

	public void setParameterType(Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        PropertyAnnotation other = (PropertyAnnotation) obj;
        if (propertyName == null) {
            if (other.propertyName != null) {
                return false;
            }
        } else if (!propertyName.equals(other.propertyName)) {
            return false;
        }

        return true;
    }

    public int compareTo(PropertyAnnotation other) {
        if ((other == null) || (other.getPropertyName() == null)) {
            return (propertyName == null) ? 0 : 1;
        } else {
            return (propertyName == null) ? -1 : propertyName.compareTo(other.getPropertyName());
        }
    }

    @Override
    public String toString() {
        return String.format("PropertyAnnotation [propertyName=%s, annotations=%s]", propertyName, annotations);
    }

}
