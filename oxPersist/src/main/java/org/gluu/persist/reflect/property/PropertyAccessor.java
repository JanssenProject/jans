/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.reflect.property;

import org.gluu.persist.exception.PropertyNotFoundException;

/**
 * Abstracts the notion of a "property". Defines a strategy for accessing the
 * value of an attribute.
 */
public interface PropertyAccessor {
    /**
     * Create a "getter" for the named attribute
     */
    Getter getGetter(Class<?> theClass, String propertyName) throws PropertyNotFoundException;

    /**
     * Create a "setter" for the named attribute
     */
    Setter getSetter(Class<?> theClass, String propertyName) throws PropertyNotFoundException;
}
