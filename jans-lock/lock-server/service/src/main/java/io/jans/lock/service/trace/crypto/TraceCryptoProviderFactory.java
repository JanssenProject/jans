/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.trace.crypto.TraceCryptoImpl;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Selects the {@link TraceCryptoProvider} implementation and exposes it as the default bean that
 * the TRACE services inject. An implementation missing from the classpath yields a fail-closed
 * {@link UnavailableTraceCryptoProvider}; two implementations of one type are a deployment error.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceCryptoProviderFactory {

	@Inject
	@Any
	private Instance<TraceCryptoProvider> providers;

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Produces
	@ApplicationScoped
	public TraceCryptoProvider traceCryptoProvider() {
		TraceCryptoProvider provider = select(configuredType());
		log.info("TRACE crypto provider: type={} available={} detail={}", provider.type(), provider.isAvailable(),
				provider.describe());
		return provider;
	}

	TraceCryptoProviderType configuredType() {
		String raw = appConfiguration.getTraceConfiguration().getCryptoProvider();
		TraceCryptoProviderType type = TraceCryptoProviderType.fromString(raw);
		if (type == null) {
			log.warn("Unknown traceConfiguration.cryptoProvider '{}', using JAVA", raw);
			return TraceCryptoProviderType.JAVA;
		}
		return type;
	}

	TraceCryptoProvider select(TraceCryptoProviderType type) {
		Instance<TraceCryptoProvider> selected = providers.select(new TraceCryptoImpl.Literal(type));
		if (selected.isUnsatisfied()) {
			log.error("No TRACE crypto provider implementation of type {} on the classpath", type);
			return new UnavailableTraceCryptoProvider(type, "no implementation on the classpath");
		}
		if (selected.isAmbiguous()) {
			throw new IllegalStateException("More than one TRACE crypto provider implementation of type " + type);
		}
		return selected.get();
	}

}
