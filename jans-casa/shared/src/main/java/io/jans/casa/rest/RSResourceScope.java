package io.jans.casa.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to decorate JAX-RS resources (eg. classes annotated with <code>@jakarta.ws.rs.Path</code>) in order
 * to specify whether the resource should be treated as a singleton or if you want your class be instantiated upon every
 * request. By default, if this annotation is not used, it is assumed the resource is a singleton.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RSResourceScope {

    /**
     * Whether the resource class will be instantiated only one time or once every request.
     * @return Boolean value
     */
    boolean singleton() default true;

}
