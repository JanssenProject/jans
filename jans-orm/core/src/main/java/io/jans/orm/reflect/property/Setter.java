/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.reflect.property;

import java.io.Serializable;
import java.lang.reflect.Method;

import io.jans.orm.exception.BasePersistenceException;

/**
 * Sets values to a particular property.
 */
public interface Setter extends Serializable {
    /**
     * Set the property value from the given instance
     *
     * @param target
     *            The instance upon which to set the given value.
     * @param value
     *            The value to be set on the target.
     * @throws BasePersistenceException
     */
    void set(Object target, Object value) throws BasePersistenceException;

    /**
     * Optional operation (return null)
     */
    String getMethodName();

    /**
     * Optional operation (return null)
     */
    Method getMethod();
}
