/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

import io.jans.orm.util.StringHelper;

/**
 * A problem occurred accessing a property of an instance of a persistent class
 * by reflection. There are a number of possible underlying causes, including
 * <ul>
 * <li>failure of a security check
 * <li>an exception occurring inside the getter or setter method
 * </ul>
 */
public class PropertyAccessException extends MappingException {

    private static final long serialVersionUID = 1076767768405558202L;

    private final Class<?> persistentClass;
    private final String propertyName;
    private final boolean wasSetter;

    public PropertyAccessException(Throwable root, String s, boolean wasSetter, Class<?> persistentClass, String propertyName) {
        super(s, root);
        this.persistentClass = persistentClass;
        this.wasSetter = wasSetter;
        this.propertyName = propertyName;
    }

    public Class<?> getPersistentClass() {
        return persistentClass;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (wasSetter ? " setter of " : " getter of ")
                + StringHelper.qualify(persistentClass.getName(), propertyName);
    }
}
