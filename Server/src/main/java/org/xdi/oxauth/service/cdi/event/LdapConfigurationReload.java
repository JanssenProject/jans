package org.xdi.oxauth.service.cdi.event;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Yuriy Movchan Date: 04/13/2017
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Documented
public @interface LdapConfigurationReload {

	public static final class Literal extends AnnotationLiteral<LdapConfigurationReload> implements LdapConfigurationReload {

		public static final Literal INSTANCE = new Literal();

		private static final long serialVersionUID = 1L;

	}

}
