/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cdi.qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import jakarta.inject.Qualifier;

/**
 * @author Yuriy Movchan on 12/30/2023
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Implementation {

    /** The name.
     * @return the name.
     */
    String value() default "";
}