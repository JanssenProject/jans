/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.exception;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Yuriy Movchan Date: 11/05/2018
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ExceptionHandler {

	/**
     * @return The list of exceptions which exception handler can catch
     */
    @Nonbinding
	Class<? extends Throwable>[] value();

}