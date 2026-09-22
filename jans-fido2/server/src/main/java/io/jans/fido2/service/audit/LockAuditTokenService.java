/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

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

	/**
	 * @return a fresh access token for {@link #LOCK_LOG_WRITE_SCOPE}, or {@code null} if the token
	 *         could not be obtained (missing/invalid config, or the token endpoint rejected the
	 *         request). Never throws — the caller treats a {@code null} token as a delivery failure.
	 */
	public String getAccessToken() {
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

		return tokenResponse.getAccessToken();
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
			OpenIdConfigurationClient client = new OpenIdConfigurationClient(issuer + "/.well-known/openid-configuration");
			client.setExecutor(TIMEOUT_ENGINE);
			OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
			if (response == null || response.getStatus() != 200 || StringHelper.isEmpty(response.getTokenEndpoint())) {
				log.error("Failed to discover OpenID configuration for Lock audit delivery");
				return null;
			}
			cachedTokenEndpoint = response.getTokenEndpoint();
			return cachedTokenEndpoint;
		} catch (Exception e) {
			log.error("Failed to discover OpenID configuration for Lock audit delivery", e);
			return null;
		}
	}
}
