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

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import io.jans.as.client.TokenResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.service.EncryptionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the guard branches that return before ever constructing {@code TokenClient} or
 * {@code OpenIdConfigurationClient}, plus the token-caching logic added after review on the PR that
 * introduced this class flagged a fresh token being issued on every flush (~every
 * {@code lockAuditFlushInterval}) instead of being reused until it actually expires. The
 * client-credentials round trip itself is not unit-testable without a running token endpoint —
 * jans-lock's own {@code TokenEndpointServiceTest} has the same limitation for the identical
 * {@code new TokenClient(url)} pattern, so this file does not attempt to cover that part either.
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
	@Mock
	private EncryptionService encryptionService;

	private Fido2Configuration fido2Configuration;

	@BeforeEach
	void setUp() {
		fido2Configuration = new Fido2Configuration();
		when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
	}

	/**
	 * The client secret is sent to whatever the issuer's discovery document resolves to; an insecure
	 * issuer must be rejected before that request is ever made, not just before the final delivery
	 * call in {@code LockAuditClient}.
	 */
	@Test
	void getAccessToken_ifIssuerIsPlainHttp_returnsNullWithoutDiscovery() throws Exception {
		fido2Configuration.setLockAuditClientId("client-id");
		fido2Configuration.setLockAuditClientPassword("encrypted-secret");
		when(encryptionService.decrypt("encrypted-secret")).thenReturn("secret");
		when(appConfiguration.getIssuer()).thenReturn("http://issuer.example.com");

		assertNull(lockAuditTokenService.getAccessToken());
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

	/**
	 * This is the actual behavior the caching exists to enable: a token with real time left on it
	 * must be treated as usable, not discarded and re-requested.
	 */
	@Test
	void cacheableToken_withAmpleExpiresIn_producesAnImmediatelyUsableCachedToken() {
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("token-abc");
		tokenResponse.setExpiresIn(300);

		LockAuditTokenService.CachedToken cached = LockAuditTokenService.cacheableToken(tokenResponse);

		assertTrue(cached.isUsable());
	}

	/**
	 * The previous test only proved a 300s-lifetime token is usable "now" — that assertion alone would
	 * still pass if the 30-second safety-buffer subtraction were deleted entirely (a 300s-out expiry is
	 * also "usable now"). This one pins that the buffer is actually subtracted from the cached expiry,
	 * not just that the result happens to still be in the future.
	 */
	@Test
	void cacheableToken_appliesTheSafetyBufferToTheCachedExpiry() {
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("token-abc");
		tokenResponse.setExpiresIn(300);

		LockAuditTokenService.CachedToken cached = LockAuditTokenService.cacheableToken(tokenResponse);

		Instant expectedExpiry = Instant.now().plusSeconds(300 - 30);
		long driftSeconds = Math.abs(Duration.between(expectedExpiry, cached.expiresAt()).getSeconds());
		assertTrue(driftSeconds <= 2,
				"expected expiresAt within 2s of now+270s (300s expiresIn minus the 30s buffer), was off by " + driftSeconds + "s");
	}

	/**
	 * Some token endpoints omit {@code expires_in}. Without a trustworthy expiry, caching indefinitely
	 * would risk serving a token past whatever lifetime the server actually gave it — safer to skip
	 * the cache entirely for that response than to guess.
	 */
	@Test
	void cacheableToken_withoutExpiresIn_isNotCached() {
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("token-abc");
		// expiresIn left unset

		assertNull(LockAuditTokenService.cacheableToken(tokenResponse));
	}

	/**
	 * A token whose entire lifetime is at or under the refresh safety buffer would already be
	 * considered expired the instant it is cached — pins that this is treated as "don't cache" rather
	 * than producing a {@link LockAuditTokenService.CachedToken} that is never usable.
	 */
	@Test
	void cacheableToken_withExpiresInAtTheSafetyBuffer_isNotCached() {
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("token-abc");
		tokenResponse.setExpiresIn(30);

		assertNull(LockAuditTokenService.cacheableToken(tokenResponse));
	}

	/**
	 * Pins the other half of the boundary a token that has already passed its (safety-buffered)
	 * expiry must never be reported usable, regardless of how it was constructed.
	 */
	@Test
	void cachedToken_withExpiryInThePast_isNotUsable() {
		LockAuditTokenService.CachedToken cached = new LockAuditTokenService.CachedToken("token-abc", Instant.now().minusSeconds(1));

		assertFalse(cached.isUsable());
	}

	/**
	 * The caller-path proof that caching actually short-circuits {@code getAccessToken()}: with a
	 * usable token already cached, no field on {@code fido2Configuration} is even read, so
	 * {@code appConfiguration}/{@code encryptionService}/{@code log} see no interactions at all.
	 * Reflection is the only way to seed the cache here — {@code requestNewToken()}'s own path is not
	 * reachable without a live token endpoint, the same limitation as every test above it.
	 */
	@Test
	@SuppressWarnings("unchecked")
	void getAccessToken_ifCachedTokenIsUsable_reusesItWithoutTouchingConfigEncryptionOrLog() throws Exception {
		LockAuditTokenService.CachedToken cached = new LockAuditTokenService.CachedToken("cached-token-xyz", Instant.now().plusSeconds(60));
		Field cachedTokenField = LockAuditTokenService.class.getDeclaredField("cachedToken");
		cachedTokenField.setAccessible(true);
		((AtomicReference<LockAuditTokenService.CachedToken>) cachedTokenField.get(lockAuditTokenService)).set(cached);

		String accessToken = lockAuditTokenService.getAccessToken();

		assertEquals("cached-token-xyz", accessToken);
		verifyNoInteractions(appConfiguration, encryptionService, log);
	}
}
