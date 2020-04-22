/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxtrust.auth.uma.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Marks a class as a UMA scope resource.
 * 
 * Provides service to protect Rest service endpoints with UMA scope.
 * 
 * @author Dmitry Ognyannikov
 */
@Inherited
@InterceptorBinding
@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface UmaSecure {
    /**
     * UMA scopes as single string
     */
    String scope() default "";
}
