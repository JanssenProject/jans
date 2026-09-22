/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.audit.LockAuditEvent;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.DataMapperService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Delivers a batch of {@link LockAuditEvent} to the Lock Server's {@code /audit/log/bulk}
 * endpoint. Transport only — batching/scheduling lives in {@link LockAuditEventCollector}, which
 * is also what turns any exception thrown here into a dropped-batch, never a propagated failure.
 */
@ApplicationScoped
public class LockAuditClient {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private LockAuditTokenService lockAuditTokenService;

	@Inject
	private DataMapperService dataMapperService;

	private static final int CONNECT_TIMEOUT_SECONDS = 5;
	private static final int READ_TIMEOUT_SECONDS = 10;

	private final ClientBuilder clientBuilder = ResteasyClientBuilder.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

	/**
	 * @throws Fido2RuntimeException on any failure to obtain a token, serialize, or deliver the
	 *         batch. The caller ({@link LockAuditEventCollector}) is responsible for catching this
	 *         and dropping the batch rather than retrying inline or propagating it further.
	 */
	public void postBatch(List<LockAuditEvent> events) {
		String endpoint = appConfiguration.getFido2Configuration().getLockAuditEndpoint();
		if (StringHelper.isEmpty(endpoint)) {
			throw new Fido2RuntimeException("lockAuditEndpoint is not configured");
		}
		requireSecureEndpoint(endpoint);

		String accessToken = lockAuditTokenService.getAccessToken();
		if (accessToken == null) {
			throw new Fido2RuntimeException("Unable to obtain a Lock audit access token");
		}

		String payload;
		try {
			payload = dataMapperService.writeValueAsString(events);
		} catch (Exception e) {
			throw new Fido2RuntimeException("Failed to serialize Lock audit event batch", e);
		}

		Client client = clientBuilder.build();
		try {
			WebTarget target = client.target(bulkEndpoint(endpoint));
			Response response = target.request().header("Authorization", "Bearer " + accessToken)
					.post(Entity.entity(payload, MediaType.APPLICATION_JSON));
			try {
				int status = response.getStatus();
				if (status < 200 || status >= 300) {
					throw new Fido2RuntimeException(
							String.format("Lock audit bulk delivery failed, status: %s, endpoint: %s", status, endpoint));
				}
				log.debug("Delivered {} Lock audit event(s)", events.size());
			} finally {
				response.close();
			}
		} finally {
			client.close();
		}
	}

	private static String bulkEndpoint(String endpoint) {
		return endpoint.endsWith("/") ? endpoint + "log/bulk" : endpoint + "/log/bulk";
	}

	/**
	 * The bearer token and audit payload must never go over the wire in clear text (CWE-319).
	 * {@code http://localhost}/{@code http://127.0.0.1}/{@code http://[::1]} is allowed for local
	 * development against a Lock Server run without TLS; any other {@code http://} endpoint is
	 * rejected.
	 */
	private static void requireSecureEndpoint(String endpoint) {
		URI uri;
		try {
			uri = new URI(endpoint);
		} catch (URISyntaxException e) {
			throw new Fido2RuntimeException("lockAuditEndpoint is not a valid URI: " + endpoint, e);
		}

		String scheme = uri.getScheme();
		if ("https".equalsIgnoreCase(scheme)) {
			return;
		}
		if ("http".equalsIgnoreCase(scheme) && isLoopback(uri.getHost())) {
			return;
		}
		throw new Fido2RuntimeException(
				"lockAuditEndpoint must use https (loopback http permitted for local development): " + endpoint);
	}

	private static boolean isLoopback(String host) {
		return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
	}
}
