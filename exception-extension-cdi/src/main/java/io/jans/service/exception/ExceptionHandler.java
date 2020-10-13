/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.exception;

import javax.interceptor.InterceptorBinding;
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
	Class<? extends Throwable>[] value();

}