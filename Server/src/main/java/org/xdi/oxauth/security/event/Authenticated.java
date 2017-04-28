package org.xdi.oxauth.security.event;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Documented
public @interface Authenticated {

	public static final class Literal extends AnnotationLiteral<Authenticated> implements Authenticated {

		public static final Literal INSTANCE = new Literal();

		private static final long serialVersionUID = 1L;

	}

}
