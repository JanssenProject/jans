/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Documented
public @interface Secure {

    /**
     * @return The EL expression that should be evaluated. If it evaluates to
     *         {@code true}, access will be granted. The EL expression may reference
     *         any objects that are in any context
     */
    String value();

}
