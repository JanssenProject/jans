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
 * Mark that attribute contains password
 *
 * @author Yuriy Movchan Date: 05/15/2018
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    
    /**
     * (Optional) Specify that value should not be hashed if it has knows {HASH_TYPE} prefix.
     * Defaults value is false.
     */
    boolean skipHashed() default false;
}
