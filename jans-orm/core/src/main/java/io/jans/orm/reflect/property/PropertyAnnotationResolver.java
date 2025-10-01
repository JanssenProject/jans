/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.reflect.property;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.jans.orm.exception.PropertyNotFoundException;

/**
 * Defines a strategy for accessing class and propery annotations.
 *
 * @author Yuriy Movchan Date: 10.08.2010
 */
public interface PropertyAnnotationResolver {

    /**
     * Get list of class annotations
     */
    List<Annotation> getClassAnnotations(Class<?> theClass, Class<?>... allowedAnnotations);

    /**
     * Get list of property annotations
     */
    List<Annotation> getPropertyAnnotations(Class<?> theClass, String propertyName, Class<?>... allowedAnnotations)
            throws PropertyNotFoundException;

    /**
     * Get map of properties annotations
     */
    Map<String, List<Annotation>> getPropertiesAnnotations(Class<?> theClass, Class<?>... allowedAnnotations);

}
