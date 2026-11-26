/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.util.security.SecurityProviderUtility;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TraceEmitterTest {

	@InjectMocks
	private TraceEmitter emitter;

	@Mock
	private Logger log;
	@Mock
	private AppConfiguration appConfiguration;
	@Mock
	private TraceLockTransport transport;

	@TempDir
	Path dir;

	private Fido2Configuration configuration;
	private PrivateKey key;

	@BeforeAll
	static void installProvider() {
		SecurityProviderUtility.installBCProvider(true);
	}

	@BeforeEach
	void setUp() throws Exception {
		key = KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider()).generateKeyPair().getPrivate();
		configuration = new Fido2Configuration();
		configuration.setFido2TraceEnabled(true);
		configuration.setFido2TraceProducerId("fido-node-1");
		configuration.setFido2TraceKeyStorePath(dir.resolve("keystore.bcfks").toString());
		when(appConfiguration.getFido2Configuration()).thenReturn(configuration);
		when(appConfiguration.getIssuer()).thenReturn("https://issuer.example.com");
		emitter.delivery = Runnable::run;
		emitter.keySupplier = () -> key;
	}

	@Test
	void emit_ifTraceDisabled_deliversNothing() {
		configuration.setFido2TraceEnabled(false);

		emitter.emit(TraceRecordBuilder.CeremonyType.REGISTRATION, TraceRecordBuilder.Outcome.SUCCESS, "sess-1");

		verifyNoInteractions(transport);
	}

	@Test
	void emit_ifNoSession_deliversNothing() {
		emitter.emit(TraceRecordBuilder.CeremonyType.REGISTRATION, TraceRecordBuilder.Outcome.SUCCESS, null);

		verifyNoInteractions(transport);
	}

	@Test
	void emit_ifSession_deliversSignedFidoCeremonyRecord() {
		emitter.emit(TraceRecordBuilder.CeremonyType.ASSERTION, TraceRecordBuilder.Outcome.FAILURE, "sess-1");

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(transport).deliver(captor.capture());
		String json = captor.getValue();
		assertTrue(json.contains("\"event_kind\":\"FIDO_CEREMONY\""), json);
		assertTrue(json.contains("\"session_id\":\"sess-1\""), json);
		assertTrue(json.contains("\"ceremony_outcome\":\"FAILURE\""), json);
		assertTrue(json.contains("\"signature\""), json);
	}

	@Test
	void emit_ifSigningFails_doesNotThrowAndDeliversNothing() {
		emitter.keySupplier = () -> {
			throw new IllegalStateException("no key");
		};

		emitter.emit(TraceRecordBuilder.CeremonyType.REGISTRATION, TraceRecordBuilder.Outcome.SUCCESS, "sess-1");

		verifyNoInteractions(transport);
	}
}
