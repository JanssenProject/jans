/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import org.slf4j.Logger;

import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Obtains an OAuth2 access token, via the client credentials grant, for posting audit events to
 * the Lock Server. Mirrors the pattern jans-lock's own {@code TokenEndpointService} uses for the
 * same grant type against the same shared auth server, rather than introducing a new one.
 */
@ApplicationScoped
public class LockAuditTokenService {

	/** Scope the Lock Server's /audit/log endpoints require; see ApiAccessConstants.LOCK_LOG_WRITE_ACCESS in jans-lock. */
	private static final String LOCK_LOG_WRITE_SCOPE = "https://jans.io/oauth/lock/log.write";

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

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

		String tokenEndpoint = resolveTokenEndpoint();
		if (tokenEndpoint == null) {
			return null;
		}

		TokenClient tokenClient = new TokenClient(tokenEndpoint);
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
