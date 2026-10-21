/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Selects a {@link TraceCryptoProvider} implementation. The member is binding: CDI compares
 * {@link #value()} when resolving {@code @Inject @TraceCryptoImpl(TraceCryptoProviderType.JAVA)}
 * or {@code Instance.select(new TraceCryptoImpl.Literal(type))}.
 *
 * @author Yuriy Movchan
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface TraceCryptoImpl {

	/**
	 * @return the provider type the annotated bean implements or the injection point requests
	 */
	TraceCryptoProviderType value();

	/** Annotation literal for programmatic lookup. */
	public static final class Literal extends AnnotationLiteral<TraceCryptoImpl> implements TraceCryptoImpl {

		private static final long serialVersionUID = 1L;

		private final TraceCryptoProviderType value;

		public Literal(TraceCryptoProviderType value) {
			this.value = value;
		}

		@Override
		public TraceCryptoProviderType value() {
			return value;
		}

	}

}
