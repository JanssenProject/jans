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
 * Persistance Entry Expiration
 *
 * @author Yuriy Movchan Date: 03/26/2020
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Expiration {
    /**
     * (Optional) Specify that we ignore this Persistance attribute during update.
     * Defaults value is false.
     */
    boolean ignoreDuringUpdate() default false;

}
