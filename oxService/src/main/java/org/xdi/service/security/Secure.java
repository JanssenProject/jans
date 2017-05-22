package org.xdi.service.security;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Secure {

	/**
     * @return The EL expression that should be evaluated. If it evaluates to {@code true}, access will be granted.
     * The EL expression may reference any objects that are in any context
     */
	String value();

}