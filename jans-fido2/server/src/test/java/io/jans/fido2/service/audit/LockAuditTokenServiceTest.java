/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Covers the guard branches that return before ever constructing {@code TokenClient} or
 * {@code OpenIdConfigurationClient}. The actual client-credentials round trip is not unit-testable
 * without a running token endpoint — jans-lock's own {@code TokenEndpointServiceTest} has the same
 * limitation for the identical pattern, so this file does not attempt to cover it either.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockAuditTokenServiceTest {

	@InjectMocks
	private LockAuditTokenService lockAuditTokenService;

	@Mock
	private Logger log;
	@Mock
	private AppConfiguration appConfiguration;

	private Fido2Configuration fido2Configuration;

	@BeforeEach
	void setUp() {
		fido2Configuration = new Fido2Configuration();
		when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
	}

	@Test
	void getAccessToken_ifClientIdMissing_returnsNullWithoutContactingTheIssuer() {
		fido2Configuration.setLockAuditClientPassword("secret");
		// lockAuditClientId left unset

		assertNull(lockAuditTokenService.getAccessToken());
	}

	@Test
	void getAccessToken_ifClientPasswordMissing_returnsNullWithoutContactingTheIssuer() {
		fido2Configuration.setLockAuditClientId("client-id");
		// lockAuditClientPassword left unset

		assertNull(lockAuditTokenService.getAccessToken());
	}

	@Test
	void getAccessToken_ifIssuerNotConfigured_returnsNull() {
		fido2Configuration.setLockAuditClientId("client-id");
		fido2Configuration.setLockAuditClientPassword("secret");
		when(appConfiguration.getIssuer()).thenReturn(null);

		assertNull(lockAuditTokenService.getAccessToken());
	}
}
