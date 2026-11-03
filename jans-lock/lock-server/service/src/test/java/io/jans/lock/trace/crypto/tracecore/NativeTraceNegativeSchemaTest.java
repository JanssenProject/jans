/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.TraceNegativeSchemaTest;
import io.jans.lock.trace.crypto.TraceCryptoProvider;

/**
 * The negative-fixture suite over the {@code NATIVE} provider: the parser's trial canonicalization
 * must reject the same fixtures with the same error id and reason.
 */
public class NativeTraceNegativeSchemaTest extends TraceNegativeSchemaTest {

	@Override
	protected TraceCryptoProvider cryptoProvider() {
		return TestProviders.nativeProvider(new AppConfiguration().getTraceConfiguration());
	}

}
