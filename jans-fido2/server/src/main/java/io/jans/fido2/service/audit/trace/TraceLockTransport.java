/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.audit.LockAuditTokenService;
import io.jans.fido2.service.audit.LockAuditUrlValidator;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Sends one signed TRACE record to the Lock Server's {@code POST /audit/trace} endpoint with the
 * {@code trace.write} scope.
 * <p>
 * Retry follows the GovOps TRACE Collector API (§10.3, RTY-003 to RTY-005): the same bytes are resent
 * after a delay with full jitter, the delay starts at 1 s, doubles per retry and is capped at 5 min.
 * A {@code Retry-After} header raises the delay to at least its value. A {@code retryable} member in
 * the Problem Details body decides retryability when present; otherwise the status code does
 * (429 and 5xx are retryable, other statuses are not). A record is never altered between attempts.
 * <p>
 * Never throws: callers get an {@link Outcome} and decide what to do with a record that was not
 * accepted.
 */
@ApplicationScoped
public class TraceLockTransport {

	/** Total attempts per record, the first one included. Bounded, as the standard requires for retries. */
	static final int MAX_ATTEMPTS = 5;
	/** RTY-005: initial delay, growth factor 2, cap. */
	static final long BASE_BACKOFF_MS = 1_000;
	static final long MAX_BACKOFF_MS = 300_000;

	private static final int CONNECT_TIMEOUT_SECONDS = 5;
	private static final int READ_TIMEOUT_SECONDS = 10;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public enum Outcome {
		/** Lock answered 2xx. */
		ACCEPTED,
		/** Lock answered a terminal rejection (not retryable, for example 401, 403, 409). */
		REJECTED,
		/** Retries ran out, or Retry-After asked for longer than the cap. The record is still the caller's to keep. */
		EXHAUSTED,
		/** The endpoint is missing or insecure, so nothing was sent. */
		NOT_CONFIGURED
	}

	/** Result of one POST: status, the raw Retry-After header and the body, each possibly null. */
	static final class PostResult {
		final int status;
		final String retryAfter;
		final String body;

		PostResult(int status, String retryAfter, String body) {
			this.status = status;
			this.retryAfter = retryAfter;
			this.body = body;
		}
	}

	/** Performs one HTTP POST. Package-visible so tests can replace the network with a fake. */
	interface RecordPoster {
		PostResult post(String endpoint, String accessToken, String recordJson) throws Exception;
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
	/** Full jitter draws from [0, 1). Package-visible so tests can pin it. */
	DoubleSupplier jitter = Math::random;

	/** Bumped on every configuration reload. A delivery that sees a different value stops before posting. */
	private final AtomicLong configGeneration = new AtomicLong();

	private final ClientBuilder clientBuilder = ResteasyClientBuilder.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

	/**
	 * @param recordJson one complete signed TRACE record, as the JSON text that was signed plus its signature
	 */
	public Outcome deliver(String recordJson) {
		// One snapshot for the whole delivery: the endpoint, the issuer and the client credentials are read
		// here once, and every attempt uses these values. The token is requested for these same credentials,
		// so it can never be posted to an endpoint from a different configuration.
		long generation = configGeneration.get();
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		String endpoint = fido2Configuration == null ? null : fido2Configuration.getFido2TraceEndpoint();
		String issuer = appConfiguration.getIssuer();
		String clientId = fido2Configuration == null ? null : fido2Configuration.getLockAuditClientId();
		String encryptedClientSecret = fido2Configuration == null ? null : fido2Configuration.getLockAuditClientPassword();
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
		long delayBeforeNextAttempt = 0;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			if (attempt > 1) {
				try {
					sleeper.sleep(delayBeforeNextAttempt);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.warn("Interrupted while retrying TRACE delivery, giving up after {} attempt(s)", attempt - 1);
					return Outcome.EXHAUSTED;
				}
			}

			// A reload stops the delivery. The snapshot already keeps the token and endpoint consistent,
			// so this check is about not continuing under a configuration that was replaced.
			if (configGeneration.get() != generation) {
				log.warn("TRACE configuration changed during delivery, record not sent");
				return Outcome.NOT_CONFIGURED;
			}

			String accessToken = lockAuditTokenService.getTraceAccessToken(issuer, clientId, encryptedClientSecret);
			if (accessToken == null) {
				lastFailure = "no trace.write access token";
				delayBeforeNextAttempt = jitteredDelay(attempt);
				continue;
			}

			if (configGeneration.get() != generation) {
				log.warn("TRACE configuration changed during delivery, record not sent");
				return Outcome.NOT_CONFIGURED;
			}

			PostResult result;
			try {
				result = poster.post(endpoint, accessToken, recordJson);
			} catch (Exception e) {
				lastFailure = e.getClass().getSimpleName();
				delayBeforeNextAttempt = jitteredDelay(attempt);
				continue;
			}

			if (isAccepted(result.status)) {
				log.debug("TRACE record accepted by Lock, status: {}, attempt: {}", result.status, attempt);
				return Outcome.ACCEPTED;
			}
			if (!isRetryable(result.status, result.body)) {
				log.warn("Lock rejected TRACE record, status: {}", result.status);
				return Outcome.REJECTED;
			}

			long retryAfterMillis = parseRetryAfterMillis(result.retryAfter, Instant.now());
			if (retryAfterMillis > MAX_BACKOFF_MS) {
				log.warn("Lock asked to retry TRACE record after {} ms, beyond the {} ms cap, giving up", retryAfterMillis, MAX_BACKOFF_MS);
				return Outcome.EXHAUSTED;
			}
			delayBeforeNextAttempt = Math.max(jitteredDelay(attempt), retryAfterMillis);
			lastFailure = "status " + result.status;
		}

		log.warn("TRACE record not delivered after {} attempts, last failure: {}", MAX_ATTEMPTS, lastFailure);
		return Outcome.EXHAUSTED;
	}

	/** Invalidates in-flight deliveries: they check this generation before every POST. */
	public void onConfigurationUpdate(@Observes @ConfigurationUpdate AppConfiguration updatedConfiguration) {
		configGeneration.incrementAndGet();
	}

	/** Delay before the retry that follows {@code failedAttempt}: full jitter over the exponential ceiling. */
	long jitteredDelay(int failedAttempt) {
		return (long) (jitter.getAsDouble() * backoffCeilingMillis(failedAttempt));
	}

	static boolean isAccepted(int status) {
		return status >= 200 && status < 300;
	}

	/**
	 * A {@code retryable} member in the Problem Details body wins when present (PRB-005). Without one,
	 * 429 and 5xx are transient and other statuses are terminal.
	 */
	static boolean isRetryable(int status, String body) {
		Boolean declared = declaredRetryable(body);
		if (declared != null) {
			return declared;
		}
		return status == 429 || status >= 500;
	}

	/** @return the {@code retryable} member of a JSON error body, or {@code null} if the body has none. */
	static Boolean declaredRetryable(String body) {
		if (StringHelper.isEmpty(body)) {
			return null;
		}
		try {
			JsonNode node = MAPPER.readTree(body);
			JsonNode error = node.has("error") && node.get("error").isObject() ? node.get("error") : node;
			JsonNode retryable = error.get("retryable");
			return retryable != null && retryable.isBoolean() ? retryable.booleanValue() : null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Upper bound of the jitter window before retry {@code failedAttempt}: base * 2^(failedAttempt-1), capped.
	 */
	static long backoffCeilingMillis(int failedAttempt) {
		long shift = Math.min(failedAttempt - 1, 20);
		return Math.min(BASE_BACKOFF_MS << shift, MAX_BACKOFF_MS);
	}

	/**
	 * RFC 9110 §10.2.3: {@code Retry-After} is either delay-seconds or an HTTP-date.
	 *
	 * @return the wait in milliseconds, 0 when absent, unparseable or already past
	 */
	static long parseRetryAfterMillis(String value, Instant now) {
		if (StringHelper.isEmpty(value)) {
			return 0;
		}
		String trimmed = value.trim();
		try {
			return Math.max(0L, Long.parseLong(trimmed) * 1_000L);
		} catch (NumberFormatException e) {
			// not delay-seconds, try an HTTP-date below
		}
		try {
			Instant at = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
			return Math.max(0L, Duration.between(now, at).toMillis());
		} catch (Exception e) {
			return 0;
		}
	}

	private PostResult httpPost(String endpoint, String accessToken, String recordJson) throws Exception {
		Client client = clientBuilder.build();
		try {
			Response response = client.target(endpoint).request()
					.header("Authorization", "Bearer " + accessToken)
					.post(Entity.entity(recordJson, MediaType.APPLICATION_JSON));
			try {
				String body = response.hasEntity() ? response.readEntity(String.class) : null;
				return new PostResult(response.getStatus(), response.getHeaderString("Retry-After"), body);
			} finally {
				response.close();
			}
		} finally {
			client.close();
		}
	}
}
