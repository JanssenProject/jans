package io.jans.configapi.core.test.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SkipTest {
	
	/**
	 * See valid values for databases in enum {@link PersistenceType}
	 */
	String[] databases() default {};

}
