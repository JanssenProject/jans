/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.DataMapperService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the review-fixed guard rails: the bulk endpoint path derivation, rejecting a non-https
 * {@code lockAuditEndpoint} before any network call is made (CWE-319), and rejecting a missing
 * access token. {@link LockAuditClient} no longer obtains its own token — {@link LockAuditEventCollector}
 * fetches one once per drain and passes it in, so a null token here means the caller failed to get
 * one, not that this class should fetch a fresh one.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockAuditClientTest {

	@InjectMocks
	private LockAuditClient lockAuditClient;

	@Mock
	private Logger log;
	@Mock
	private AppConfiguration appConfiguration;
	@Mock
	private DataMapperService dataMapperService;

	private Fido2Configuration fido2Configuration;

	@BeforeEach
	void setUp() {
		fido2Configuration = new Fido2Configuration();
		when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
	}

	@Test
	void bulkEndpoint_appendsLogBulkUnderTheConfiguredBase() throws Exception {
		Method bulkEndpoint = LockAuditClient.class.getDeclaredMethod("bulkEndpoint", String.class);
		bulkEndpoint.setAccessible(true);

		assertEquals("https://lock.example.com/audit/log/bulk",
				bulkEndpoint.invoke(null, "https://lock.example.com/audit"));
		assertEquals("https://lock.example.com/audit/log/bulk",
				bulkEndpoint.invoke(null, "https://lock.example.com/audit/"));
	}

	@Test
	void postBatch_ifEndpointIsPlainHttp_throwsWithoutSerializingThePayload() {
		fido2Configuration.setLockAuditEndpoint("http://lock.example.com/audit");

		assertThrows(Fido2RuntimeException.class,
				() -> lockAuditClient.postBatch(Collections.emptyList(), "token-abc"));

		verifyNoInteractions(dataMapperService);
	}

	@Test
	void postBatch_ifAccessTokenIsNull_throwsWithoutSerializingThePayload() {
		fido2Configuration.setLockAuditEndpoint("https://lock.example.com/audit");

		assertThrows(Fido2RuntimeException.class,
				() -> lockAuditClient.postBatch(Collections.emptyList(), null));

		verifyNoInteractions(dataMapperService);
	}
}
