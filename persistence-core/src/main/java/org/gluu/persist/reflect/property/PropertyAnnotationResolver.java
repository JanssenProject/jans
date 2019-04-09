/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.reflect.property;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.gluu.persist.exception.PropertyNotFoundException;

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
