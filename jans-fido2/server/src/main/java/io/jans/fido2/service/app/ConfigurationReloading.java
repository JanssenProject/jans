/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Fired by {@link ConfigurationFactory} before a reloaded configuration is published. Observers that must
 * stop work started under the old configuration run here, so the state they see is already invalid when
 * the new configuration becomes readable.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Documented
public @interface ConfigurationReloading {

	final class Literal extends AnnotationLiteral<ConfigurationReloading> implements ConfigurationReloading {
		public static final Literal INSTANCE = new Literal();
		private static final long serialVersionUID = 1L;
	}
}
