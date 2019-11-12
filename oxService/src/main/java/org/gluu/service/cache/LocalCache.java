package org.gluu.service.cache;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * @author Yuriy Movchan Date: 11/12/2019
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD })
@Documented
public @interface LocalCache {

    final class Literal extends AnnotationLiteral<LocalCache> implements LocalCache {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
