/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.event;


import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Yuriy Zabrovarnyy
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
@Documented
public @interface CryptoProviderEvent {

    final class Literal extends AnnotationLiteral<CryptoProviderEvent> implements CryptoProviderEvent {

        public static final CryptoProviderEvent.Literal INSTANCE = new CryptoProviderEvent.Literal();

        private static final long serialVersionUID = 1L;

    }
}
