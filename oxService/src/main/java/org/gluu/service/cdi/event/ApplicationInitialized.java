package org.gluu.service.cdi.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized.Literal;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;

/**
 * @author Yuriy Movchan Date: 04/13/2017
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Documented
public @interface ApplicationInitialized {

    /**
     * The scope for which to observe initialization
     */
    Class<? extends Annotation> value();

    public static final class Literal extends AnnotationLiteral<ApplicationInitialized> implements ApplicationInitialized {

        public static final Literal APPLICATION = of(ApplicationScoped.class);

        private static final long serialVersionUID = 1L;

        private final Class<? extends Annotation> value;

        public static Literal of(Class<? extends Annotation> value) {
            return new Literal(value);
        }

        private Literal(Class<? extends Annotation> value) {
            this.value = value;
        }

        @Override
        public Class<? extends Annotation> value() {
            return value;
        }
    }

}
