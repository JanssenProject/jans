package org.xdi.oxauth.service.inject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Documented
public @interface Scheduled {

	public static final class Literal extends AnnotationLiteral<Scheduled> implements Scheduled {

		public static final Literal INSTANCE = new Literal();

		private static final long serialVersionUID = 1L;

	}

}
