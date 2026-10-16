/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;

import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoImpl;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import io.jans.lock.trace.crypto.bc.JavaTraceCryptoProvider;
import jakarta.enterprise.inject.Instance;

/**
 * Tests for {@link TraceCryptoProviderFactory}: implementation lookup by qualifier, the
 * fail-closed fallback and the ambiguity check. No CDI container is booted; {@link Instance} is
 * stubbed.
 */
class TraceCryptoProviderFactoryTest {

	private static final TraceCryptoImpl.Literal JAVA = new TraceCryptoImpl.Literal(TraceCryptoProviderType.JAVA);
	private static final TraceCryptoImpl.Literal NATIVE = new TraceCryptoImpl.Literal(TraceCryptoProviderType.NATIVE);

	@Test
	void testSelect_javaImplementationPresent_returnsIt() {
		TraceCryptoProvider java = TraceTestCrypto.javaProvider();
		Instance<TraceCryptoProvider> selected = instance(false, false, java);
		TraceCryptoProviderFactory factory = factory(selected);

		TraceCryptoProvider result = factory.select(TraceCryptoProviderType.JAVA);

		assertSame(java, result);
		assertInstanceOf(JavaTraceCryptoProvider.class, result);
		assertEquals(TraceCryptoProviderType.JAVA, result.type());
	}

	@Test
	void testSelect_noImplementation_failsClosedWithCryptoUnavailable() {
		TraceCryptoProviderFactory factory = factory(instance(true, false, null));

		TraceCryptoProvider result = factory.select(TraceCryptoProviderType.JAVA);

		assertInstanceOf(UnavailableTraceCryptoProvider.class, result);
		assertFalse(result.isAvailable());
		TraceCryptoException ex = assertThrows(TraceCryptoException.class, result::requireAvailable);
		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, ex.getReason());
	}

	@Test
	void testSelect_noImplementation_everyOperationFailsClosed() {
		TraceCryptoProvider result = factory(instance(true, false, null)).select(TraceCryptoProviderType.NATIVE);

		assertEquals(TraceCryptoProviderType.NATIVE, result.type());
		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
				assertThrows(TraceCryptoException.class, () -> result.parseKey(null)).getReason());
		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
				assertThrows(TraceCryptoException.class, () -> result.canonicalize(null)).getReason());
		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, assertThrows(TraceCryptoException.class,
				() -> result.receiptHash("d", 1L, 0L, "p", "r", "c", "h")).getReason());
	}

	@Test
	void testSelect_ambiguousImplementations_deploymentError() {
		TraceCryptoProviderFactory factory = factory(instance(false, true, null));

		assertThrows(IllegalStateException.class, () -> factory.select(TraceCryptoProviderType.JAVA));
	}

	@Test
	void testProducer_defaultConfiguration_selectsJava() {
		TraceCryptoProvider java = TraceTestCrypto.javaProvider();

		assertSame(java, factory(instance(false, false, java)).traceCryptoProvider());
	}

	@Test
	void testConfiguredType_unknownValue_javaAndWarn() {
		Logger log = mock(Logger.class);
		TraceCryptoProviderFactory factory = factory(instance(false, false, null), "bogus", log);

		assertEquals(TraceCryptoProviderType.JAVA, factory.configuredType());

		verify(log).warn("Unknown traceConfiguration.cryptoProvider '{}', using JAVA", "bogus");
	}

	@Test
	void testConfiguredType_lowerCasePadded_native() {
		Logger log = mock(Logger.class);
		TraceCryptoProviderFactory factory = factory(instance(false, false, null), "  native ", log);

		assertEquals(TraceCryptoProviderType.NATIVE, factory.configuredType());

		verify(log, never()).warn(any(String.class), any(Object.class));
	}

	@Test
	void testConfiguredType_blank_javaWithoutWarn() {
		Logger log = mock(Logger.class);
		TraceCryptoProviderFactory factory = factory(instance(false, false, null), " ", log);

		assertEquals(TraceCryptoProviderType.JAVA, factory.configuredType());

		verify(log, never()).warn(any(String.class), any(Object.class));
	}

	@Test
	void testProducer_nativeConfiguredButMissing_failsClosedWithoutFallback() {
		Logger log = mock(Logger.class);
		TraceCryptoProviderFactory factory = factory(instance(true, false, null), "NATIVE", log);

		TraceCryptoProvider result = factory.traceCryptoProvider();

		assertInstanceOf(UnavailableTraceCryptoProvider.class, result);
		assertEquals(TraceCryptoProviderType.NATIVE, result.type());
		assertFalse(result.isAvailable());
	}

	@SuppressWarnings("unchecked")
	private static Instance<TraceCryptoProvider> instance(boolean unsatisfied, boolean ambiguous,
			TraceCryptoProvider provider) {
		Instance<TraceCryptoProvider> all = mock(Instance.class);
		Instance<TraceCryptoProvider> selected = mock(Instance.class);
		when(all.select(any(Annotation[].class))).thenReturn(selected);
		when(all.select(JAVA)).thenReturn(selected);
		when(all.select(NATIVE)).thenReturn(selected);
		when(selected.isUnsatisfied()).thenReturn(unsatisfied);
		when(selected.isAmbiguous()).thenReturn(ambiguous);
		when(selected.get()).thenReturn(provider);
		return all;
	}

	private static TraceCryptoProviderFactory factory(Instance<TraceCryptoProvider> providers) {
		return factory(providers, null, LoggerFactory.getLogger(TraceCryptoProviderFactory.class));
	}

	private static TraceCryptoProviderFactory factory(Instance<TraceCryptoProvider> providers, String cryptoProvider,
			Logger log) {
		AppConfiguration appConfiguration = new AppConfiguration();
		if (cryptoProvider != null) {
			appConfiguration.getTraceConfiguration().setCryptoProvider(cryptoProvider);
		}
		TraceCryptoProviderFactory factory = new TraceCryptoProviderFactory();
		setField(factory, "providers", providers);
		setField(factory, "log", log);
		setField(factory, "appConfiguration", appConfiguration);
		return factory;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

}
