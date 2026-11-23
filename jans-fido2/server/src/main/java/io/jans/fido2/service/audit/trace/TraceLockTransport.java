/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.audit.LockAuditTokenService;
import io.jans.fido2.service.audit.LockAuditUrlValidator;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Sends one signed TRACE record to the Lock Server's {@code POST /audit/trace} endpoint with the
 * {@code trace.write} scope. Retries transient failures (network errors, 429, 5xx) with capped
 * exponential backoff. Any other status is a rejection and is not retried. The record is sent
 * unchanged, so a retry never alters what Lock hashes or stores.
 * <p>
 * Never throws: callers get an {@link Outcome} and decide what to do with a record that was not
 * accepted.
 */
@ApplicationScoped
public class TraceLockTransport {

	/** Total attempts per record, the first one included. */
	static final int MAX_ATTEMPTS = 5;
	static final long BASE_BACKOFF_MS = 200;
	static final long MAX_BACKOFF_MS = 5_000;

	private static final int CONNECT_TIMEOUT_SECONDS = 5;
	private static final int READ_TIMEOUT_SECONDS = 10;

	public enum Outcome {
		/** Lock answered 2xx. */
		ACCEPTED,
		/** Lock answered a non-retryable status (4xx other than 429, for example 401, 403, 409). */
		REJECTED,
		/** Every attempt failed with a retryable error. */
		EXHAUSTED,
		/** The endpoint is missing or insecure, so nothing was sent. */
		NOT_CONFIGURED
	}

	/** Performs one HTTP POST. Package-visible so tests can replace the network with a fake. */
	interface RecordPoster {
		int post(String endpoint, String accessToken, String recordJson) throws Exception;
	}

	/** Package-visible so tests can skip the real wait. */
	interface Sleeper {
		void sleep(long millis) throws InterruptedException;
	}

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private LockAuditTokenService lockAuditTokenService;

	RecordPoster poster = this::httpPost;
	Sleeper sleeper = Thread::sleep;

	private final ClientBuilder clientBuilder = ResteasyClientBuilder.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

	/**
	 * @param recordJson one complete signed TRACE record, as the JSON text that was signed plus its signature
	 */
	public Outcome deliver(String recordJson) {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		String endpoint = fido2Configuration == null ? null : fido2Configuration.getFido2TraceEndpoint();
		if (StringHelper.isEmpty(endpoint)) {
			log.error("fido2TraceEndpoint is not configured, TRACE record not sent");
			return Outcome.NOT_CONFIGURED;
		}
		try {
			LockAuditUrlValidator.requireSecure(endpoint, "fido2TraceEndpoint");
		} catch (RuntimeException e) {
			log.error("fido2TraceEndpoint is not usable, TRACE record not sent", e);
			return Outcome.NOT_CONFIGURED;
		}

		String lastFailure = "none";
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			if (attempt > 1) {
				try {
					sleeper.sleep(backoffMillis(attempt - 1));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.warn("Interrupted while retrying TRACE delivery, giving up after {} attempt(s)", attempt - 1);
					return Outcome.EXHAUSTED;
				}
			}

			String accessToken = lockAuditTokenService.getTraceAccessToken();
			if (accessToken == null) {
				lastFailure = "no trace.write access token";
				continue;
			}

			int status;
			try {
				status = poster.post(endpoint, accessToken, recordJson);
			} catch (Exception e) {
				lastFailure = e.getClass().getSimpleName();
				continue;
			}

			if (isAccepted(status)) {
				log.debug("TRACE record accepted by Lock, status: {}, attempt: {}", status, attempt);
				return Outcome.ACCEPTED;
			}
			if (!isRetryable(status)) {
				log.warn("Lock rejected TRACE record, status: {}", status);
				return Outcome.REJECTED;
			}
			lastFailure = "status " + status;
		}

		log.warn("TRACE record not delivered after {} attempts, last failure: {}", MAX_ATTEMPTS, lastFailure);
		return Outcome.EXHAUSTED;
	}

	static boolean isAccepted(int status) {
		return status >= 200 && status < 300;
	}

	/** 429 and 5xx are transient. Other 4xx mean the request itself is wrong and will fail the same way again. */
	static boolean isRetryable(int status) {
		return status == 429 || status >= 500;
	}

	/** Backoff before retry {@code retryIndex} (1 for the first retry): base * 2^(retryIndex-1), capped. */
	static long backoffMillis(int retryIndex) {
		long shift = Math.min(retryIndex - 1, 20);
		return Math.min(BASE_BACKOFF_MS << shift, MAX_BACKOFF_MS);
	}

	private int httpPost(String endpoint, String accessToken, String recordJson) throws Exception {
		Client client = clientBuilder.build();
		try {
			Response response = client.target(endpoint).request()
					.header("Authorization", "Bearer " + accessToken)
					.post(Entity.entity(recordJson, MediaType.APPLICATION_JSON));
			try {
				return response.getStatus();
			} finally {
				response.close();
			}
		} finally {
			client.close();
		}
	}
}
