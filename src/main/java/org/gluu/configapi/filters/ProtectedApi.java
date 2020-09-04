/**
 * 
 */
package org.gluu.configapi.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;

/**
 * @author Mougang T.Gasmyr
 *
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ProtectedApi {

	/**
	 * @return UMA scopes which application should have to access oxauth-config api.
	 */
	String[] scopes() default {};

}
