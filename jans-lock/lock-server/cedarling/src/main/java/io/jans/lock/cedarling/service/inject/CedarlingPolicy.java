/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.cedarling.service.inject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@Documented
public @interface CedarlingPolicy {

    final class Literal extends AnnotationLiteral<CedarlingPolicy> implements CedarlingPolicy {

        public static final CedarlingPolicy.Literal INSTANCE = new CedarlingPolicy.Literal();

        private static final long serialVersionUID = 1L;

    }
}
