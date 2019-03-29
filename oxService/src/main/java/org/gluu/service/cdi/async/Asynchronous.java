package org.gluu.service.cdi.async;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * @author Yuriy Movchan Date: 07/07/2017
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
public @interface Asynchronous {
}
