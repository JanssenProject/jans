/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import org.slf4j.Logger;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.lock.service.OpenIdService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Builds and exposes the {@link IntrospectionService} client used by
 * {@link SubmitterIdentityService} to resolve opaque bearer tokens (design decision D-2).
 *
 * <p>This class exists only to extract the introspection-client construction that
 * {@code OpenIdProtectionService} already performs privately in its own {@code @PostConstruct}
 * (task 11's codebase-context note): duplicating the construction here means
 * {@code OpenIdProtectionService} itself is never touched, so its existing filter behavior cannot
 * regress. It intentionally mirrors that construction rather than reusing it directly, since
 * {@code OpenIdProtectionService} does not expose its {@code IntrospectionService} instance.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class IntrospectionClientProvider {

	@Inject
	private Logger log;

	@Inject
	private OpenIdService openIdService;

	private IntrospectionService introspectionService;

	@PostConstruct
	private void init() {
		try {
			OpenIdConfigurationResponse oidcConfig = openIdService.getOpenIdConfiguration();
			String introspectionEndpoint = oidcConfig.getIntrospectionEndpoint();
			introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint,
					ClientFactory.instance().createEngine());
		} catch (Exception e) {
			log.error("Failed to initialize the TRACE introspection client: {}", e.getMessage(), e);
		}
	}

	/**
	 * @param authorization the {@code Authorization} header value used to call the introspection
	 *                       endpoint itself (not the token being introspected)
	 * @param token          the opaque token to introspect
	 * @return the introspection response
	 * @throws IllegalStateException if the client failed to initialize (mapped by the caller to a
	 *                                403, never a 500 — see design decision D-2's defense-in-depth
	 *                                note)
	 */
	public IntrospectionResponse introspectToken(String authorization, String token) {
		if (introspectionService == null) {
			throw new IllegalStateException("TRACE introspection client is not initialized");
		}
		return introspectionService.introspectToken(authorization, token);
	}

	/**
	 * Test seam: injects a pre-built {@link IntrospectionService} without going through
	 * {@link #init()} (which talks to the network).
	 */
	void setIntrospectionService(IntrospectionService introspectionService) {
		this.introspectionService = introspectionService;
	}

}
