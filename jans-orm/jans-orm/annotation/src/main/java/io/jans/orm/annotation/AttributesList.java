/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.annotation;

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
     * (Optional) The class property name which contains Persistance attribute value.
     */
    String multiValued() default "";

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
