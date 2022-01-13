package io.jans.scim2.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate a method or a class in order to disable tests based on the underlying database in use.
 * See https://github.com/JanssenProject/jans-scim/issues/12
 * Be careful when a test method depends on a method annotated with <code>SkipTest</code>: in testng
 * disabling actually "deletes" the method from the execution graph. See https://github.com/cbeust/testng/issues/2546  
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SkipTest {
	
	/**
	 * See valid values for databases in enum {@link PersistenceType}
	 */
	String[] databases() default {};

}
