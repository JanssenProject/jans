/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

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
	private DataMapperService dataMapperService;

	private static final int CONNECT_TIMEOUT_SECONDS = 5;
	private static final int READ_TIMEOUT_SECONDS = 10;

	private final ClientBuilder clientBuilder = ResteasyClientBuilder.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

	/**
	 * @param accessToken a token already obtained by the caller — {@link LockAuditEventCollector}
	 *        fetches one once per drain and reuses it across every chunk of that flush, rather than
	 *        this method obtaining a fresh one per call (a drain split into N chunks previously meant
	 *        N client-credentials grants against the auth server for a single flush).
	 * @throws Fido2RuntimeException on any failure to validate the endpoint, serialize, or deliver
	 *         the batch. The caller is responsible for catching this and dropping the chunk rather
	 *         than retrying inline or propagating it further.
	 */
	public void postBatch(List<LockAuditEvent> events, String accessToken) {
		String endpoint = appConfiguration.getFido2Configuration().getLockAuditEndpoint();
		if (StringHelper.isEmpty(endpoint)) {
			throw new Fido2RuntimeException("lockAuditEndpoint is not configured");
		}
		LockAuditUrlValidator.requireSecure(endpoint, "lockAuditEndpoint");

		if (accessToken == null) {
			throw new Fido2RuntimeException("No Lock audit access token available");
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
}
