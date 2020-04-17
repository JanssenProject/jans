package org.gluu.service.document.store;

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
 * @author Yuriy Movchan on 04/10/2020
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD })
@Documented
public @interface LocalDocumentStore {

    final class Literal extends AnnotationLiteral<LocalDocumentStore> implements LocalDocumentStore {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
