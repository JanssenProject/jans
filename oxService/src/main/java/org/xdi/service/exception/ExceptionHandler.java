package org.xdi.service.exception;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

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