package io.jans.lock.cedarling.service.security.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ProtectedCedarlingApi {
	
	/**
     * @return action name
     */
	String action() default "";

	/**
     * @return resource name
     */
	String resource() default "";

	/**
     * @return resource id
     */
	String id() default "";

	/**
     * @return path to resource
     */
	String path() default "";

}
