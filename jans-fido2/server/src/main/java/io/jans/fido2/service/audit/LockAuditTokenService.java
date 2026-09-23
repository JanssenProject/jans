/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;

import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.service.EncryptionService;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;

/**
 * Obtains an OAuth2 access token, via the client credentials grant, for posting audit events to
 * the Lock Server. Mirrors the pattern jans-lock's own {@code TokenEndpointService} uses for the
 * same grant type against the same shared auth server, rather than introducing a new one.
 */
@ApplicationScoped
public class LockAuditTokenService {

	/** Scope the Lock Server's /audit/log endpoints require; see ApiAccessConstants.LOCK_LOG_WRITE_ACCESS in jans-lock. */
	private static final String LOCK_LOG_WRITE_SCOPE = "https://jans.io/oauth/lock/log.write";

	private static final int CONNECT_TIMEOUT_SECONDS = 5;
	private static final int READ_TIMEOUT_SECONDS = 10;

	/**
	 * Refresh this long before actual expiry, so a token that passes this check does not expire
	 * mid-flight between here and the delivery POST it is about to authorize.
	 */
	private static final long EXPIRY_SAFETY_BUFFER_SECONDS = 30;

	/**
	 * {@link TokenClient} / {@link OpenIdConfigurationClient} (both {@code BaseClient} subclasses in
	 * the shared jans-auth-client module) build their RESTEasy client with no connect/read timeout
	 * unless given an executor — see {@code BaseClient#initClient()}. BaseClient is consumed by many
	 * other services (jans-lock, jans-scim, jans-config-api, ...), so it is out of scope here; this
	 * engine is the per-instance hook {@code BaseClient} already exposes ({@code setExecutor}) to
	 * bound these two calls without touching it. Shared across calls since a pooled engine is meant
	 * to be reused.
	 */
	private static final ClientHttpEngine TIMEOUT_ENGINE = ((ResteasyClient) ClientBuilder.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build()).httpEngine();

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private EncryptionService encryptionService;

	private volatile String cachedTokenEndpoint;
	private volatile CachedToken cachedToken;

	/**
	 * @return a cached access token for {@link #LOCK_LOG_WRITE_SCOPE} if one is still usable, or a
	 *         freshly-issued one otherwise, or {@code null} if a token could not be obtained
	 *         (missing/invalid config, or the token endpoint rejected the request). Never throws —
	 *         the caller treats a {@code null} token as a delivery failure.
	 *         <p>
	 *         Without this cache, every flush (every {@code lockAuditFlushInterval}, ~20s by default)
	 *         issued a brand-new token — flagged in review on the PR that introduced this class.
	 */
	public String getAccessToken() {
		CachedToken cached = this.cachedToken;
		if (cached != null && cached.isUsable()) {
			return cached.accessToken;
		}
		return requestNewToken();
	}

	/**
	 * {@code synchronized} so two flushes racing a simultaneous expiry issue at most one new token
	 * rather than one each; re-checks the cache after acquiring the lock in case the request ahead of
	 * this one already refreshed it.
	 */
	private synchronized String requestNewToken() {
		CachedToken cached = this.cachedToken;
		if (cached != null && cached.isUsable()) {
			return cached.accessToken;
		}

		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		String clientId = fido2Configuration.getLockAuditClientId();
		String clientSecret = fido2Configuration.getLockAuditClientPassword();
		if (StringHelper.isEmpty(clientId) || StringHelper.isEmpty(clientSecret)) {
			log.error("lockAuditClientId / lockAuditClientPassword is not configured");
			return null;
		}

		// lockAuditClientPassword is stored encrypted, same as every other client secret in
		// AppConfiguration; mirrors jans-lock's TokenEndpointService#getDecryptedPassword for the
		// identical grant.
		try {
			clientSecret = encryptionService.decrypt(clientSecret);
		} catch (EncryptionException e) {
			log.error("Failed to decrypt lockAuditClientPassword", e);
			return null;
		}

		String tokenEndpoint = resolveTokenEndpoint();
		if (tokenEndpoint == null) {
			return null;
		}

		TokenClient tokenClient = new TokenClient(tokenEndpoint);
		tokenClient.setExecutor(TIMEOUT_ENGINE);
		TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(LOCK_LOG_WRITE_SCOPE, clientId, clientSecret);
		if (tokenResponse == null || tokenResponse.getStatus() != 200 || StringHelper.isEmpty(tokenResponse.getAccessToken())) {
			log.error("Failed to obtain Lock audit access token, status: {}",
					tokenResponse == null ? "no response" : tokenResponse.getStatus());
			return null;
		}

		this.cachedToken = cacheableToken(tokenResponse);
		return tokenResponse.getAccessToken();
	}

	/**
	 * @return a {@link CachedToken} if {@code tokenResponse} carries an {@code expires_in} the cache
	 *         can trust, or {@code null} to skip caching entirely when it does not — serving a token
	 *         past an expiry we never actually confirmed is worse than re-issuing one every call.
	 *         Package-visible so a test can drive it directly without a live token endpoint.
	 */
	static CachedToken cacheableToken(TokenResponse tokenResponse) {
		Integer expiresIn = tokenResponse.getExpiresIn();
		if (expiresIn == null || expiresIn <= EXPIRY_SAFETY_BUFFER_SECONDS) {
			return null;
		}
		Instant expiresAt = Instant.now().plusSeconds(expiresIn - EXPIRY_SAFETY_BUFFER_SECONDS);
		return new CachedToken(tokenResponse.getAccessToken(), expiresAt);
	}

	/** Package-visible (class and constructor) for the same reason as {@link #cacheableToken}. */
	static final class CachedToken {
		private final String accessToken;
		private final Instant expiresAt;

		CachedToken(String accessToken, Instant expiresAt) {
			this.accessToken = accessToken;
			this.expiresAt = expiresAt;
		}

		boolean isUsable() {
			return Instant.now().isBefore(expiresAt);
		}

		/** Package-visible so a test can assert the safety buffer was actually applied. */
		Instant expiresAt() {
			return expiresAt;
		}
	}

	/**
	 * Discovered once per node and cached, same as jans-lock's OpenIdService — the issuer's token
	 * endpoint does not change without a redeploy.
	 */
	private String resolveTokenEndpoint() {
		if (cachedTokenEndpoint != null) {
			return cachedTokenEndpoint;
		}

		String issuer = appConfiguration.getIssuer();
		if (StringHelper.isEmpty(issuer)) {
			log.error("Issuer is not configured, cannot discover the token endpoint for Lock audit delivery");
			return null;
		}

		try {
			// The discovery response is unauthenticated at this layer, so an insecure issuer would let a
			// network attacker hand back an arbitrary token_endpoint; reject before the request is even made.
			LockAuditUrlValidator.requireSecure(issuer, "issuer");

			OpenIdConfigurationClient client = new OpenIdConfigurationClient(issuer + "/.well-known/openid-configuration");
			client.setExecutor(TIMEOUT_ENGINE);
			OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
			if (response == null || response.getStatus() != 200 || StringHelper.isEmpty(response.getTokenEndpoint())) {
				log.error("Failed to discover OpenID configuration for Lock audit delivery");
				return null;
			}

			// The client secret is sent to whatever this resolves to; validate independently of the
			// issuer check above in case a misconfigured/compromised discovery document names an
			// insecure endpoint even though the issuer itself is https.
			LockAuditUrlValidator.requireSecure(response.getTokenEndpoint(), "token_endpoint");

			cachedTokenEndpoint = response.getTokenEndpoint();
			return cachedTokenEndpoint;
		} catch (Exception e) {
			log.error("Failed to discover OpenID configuration for Lock audit delivery", e);
			return null;
		}
	}
}
