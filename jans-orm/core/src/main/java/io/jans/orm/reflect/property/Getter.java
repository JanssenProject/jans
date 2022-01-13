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
 * Gets values of a particular property
 */
public interface Getter extends Serializable {
    /**
     * Get the property value from the given instance.
     *
     * @param owner
     *            The instance containing the value to be retreived.
     * @return The extracted value.
     * @throws BasePersistenceException
     */
    Object get(Object owner) throws BasePersistenceException;

    /**
     * Get the declared Java type
     */
    Class<?> getReturnType();

    /**
     * Optional operation (return null)
     */
    String getMethodName();

    /**
     * Optional operation (return null)
     */
    Method getMethod();
}
