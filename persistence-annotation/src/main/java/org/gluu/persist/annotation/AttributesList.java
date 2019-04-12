/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Persistance Attributes List
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AttributesList {

    /**
     * (Required) The class property name which contains Persistance attribute name.
     */
    String name();

    /**
     * (Required) The class property name which contains Persistance attribute value.
     */
    String value();

    /**
     * (Optional) Holds additional configuration for Persistance attributes. Defaults
     * value not provides additional configuration.
     */
    AttributeName[] attributesConfiguration() default {};

    /**
     * (Optional) Specify if attributes should be sorted by property name value.
     */
    boolean sortByName() default false;

}
