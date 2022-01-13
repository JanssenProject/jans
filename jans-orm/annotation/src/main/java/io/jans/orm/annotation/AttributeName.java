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
 * Persistance Attribute
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AttributeName {

    /**
     * (Optional) The name of the Persistance attribute. Defaults to the field name.
     */
    String name() default "";

    /**
     * (Optional) Specify that we ignore this Persistance attribute during read.
     * Defaults value is false.
     */
    boolean ignoreDuringRead() default false;

    /**
     * (Optional) Specify that we ignore this Persistance attribute during update.
     * Defaults value is false.
     */
    boolean ignoreDuringUpdate() default false;

    /**
     * (Optional) Specify that we will only update this attribute, and never
     * remove it (set to null). Use this with health status attributes.
     */
    boolean updateOnly() default false;

    /**
     * (Optional) Specify that search request should wait for indexes update
     * Defaults value is false.
     */
    boolean consistency() default false;
}
