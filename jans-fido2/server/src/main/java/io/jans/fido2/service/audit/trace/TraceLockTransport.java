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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.app.ConfigurationReloading;
import io.jans.fido2.service.audit.LockAuditTokenService;
import io.jans.fido2.service.audit.LockAuditUrlValidator;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Sends one signed TRACE record to the Lock Server's {@code POST /audit/trace} endpoint with the
 * {@code trace.write} scope.
 * <p>
 * Retry follows the GovOps TRACE Collector API §10.3 (RTY-003 to RTY-005): the same bytes are resent
 * after a delay with full jitter. The delay starts at 1 s, doubles per retry and is capped at 5 min.
 * A {@code Retry-After} header raises the delay to at least its value. A {@code retryable} member in
 * the Problem Details body decides retryability when present, checked inside {@code error} first and
 * then at the top level. Without one, 429 and 5xx are retried and other statuses are not.
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

	private static final String ERROR_MEMBER = "error";
	private static final String RETRYABLE_MEMBER = "retryable";
	private static final String RETRY_AFTER_HEADER = "Retry-After";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public enum Outcome {
		/** Lock answered 2xx. */
		ACCEPTED,
		/** Lock answered a terminal rejection (not retryable, for example 401, 403, 409). */
		REJECTED,
		/** Retries ran out, or Retry-After asked for longer than the cap. The record is still the caller's to keep. */
		EXHAUSTED,
		/** The endpoint is missing or insecure, the configuration changed, so nothing was sent. */
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
		PostResult post(String endpoint, String accessToken, String recordJson);
	}

	/** Package-visible so tests can skip the real wait. */
	interface Sleeper {
		void sleep(long millis) throws InterruptedException;
	}

	/**
	 * The endpoint, issuer and client credentials of one delivery, read once. Every attempt uses these
	 * values, so the token and the endpoint always come from the same configuration.
	 */
	static final class Snapshot {
		final long generation;
		final String endpoint;
		final String issuer;
		final String clientId;
		final String encryptedClientSecret;

		Snapshot(long generation, String endpoint, String issuer, String clientId, String encryptedClientSecret) {
			this.generation = generation;
			this.endpoint = endpoint;
			this.issuer = issuer;
			this.clientId = clientId;
			this.encryptedClientSecret = encryptedClientSecret;
		}
	}

	/** Result of one attempt. An {@code outcome} ends the delivery; otherwise the next attempt waits {@code delayMillis}. */
	static final class Attempt {
		final Outcome outcome;
		final long delayMillis;
		final String failure;

		private Attempt(Outcome outcome, long delayMillis, String failure) {
			this.outcome = outcome;
			this.delayMillis = delayMillis;
			this.failure = failure;
		}

		static Attempt done(Outcome outcome) {
			return new Attempt(outcome, 0, null);
		}

		static Attempt retry(String failure, long delayMillis) {
			return new Attempt(null, delayMillis, failure);
		}
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

	/**
	 * Bumped before a reloaded configuration is published (see {@link ConfigurationReloading}). A delivery
	 * that sees a different value stops before it requests a token or posts.
	 */
	private final AtomicLong configGeneration = new AtomicLong();

	/**
	 * @param recordJson one complete signed TRACE record, as the JSON text that was signed plus its signature
	 */
	public Outcome deliver(String recordJson) {
		Snapshot snapshot = takeSnapshot();
		if (snapshot == null) {
			return Outcome.NOT_CONFIGURED;
		}

		long delayMillis = 0;
		String lastFailure = "none";
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			if (attempt > 1 && !pause(delayMillis)) {
				return Outcome.EXHAUSTED;
			}
			Attempt result = attemptOnce(snapshot, recordJson, attempt);
			if (result.outcome != null) {
				return result.outcome;
			}
			delayMillis = result.delayMillis;
			lastFailure = result.failure;
		}

		log.warn("TRACE record not delivered after {} attempts, last failure: {}", MAX_ATTEMPTS, lastFailure);
		return Outcome.EXHAUSTED;
	}

	/** Invalidates in-flight deliveries: they check this generation before every token request and POST. */
	public void onConfigurationReloading(@Observes @ConfigurationReloading AppConfiguration previousConfiguration) {
		configGeneration.incrementAndGet();
	}

	/** @return the snapshot to deliver under, or {@code null} with the reason logged if the endpoint is unusable. */
	private Snapshot takeSnapshot() {
		long generation = configGeneration.get();
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		String endpoint = fido2Configuration == null ? null : fido2Configuration.getFido2TraceEndpoint();
		if (StringHelper.isEmpty(endpoint)) {
			log.error("fido2TraceEndpoint is not configured, TRACE record not sent");
			return null;
		}
		try {
			LockAuditUrlValidator.requireSecure(endpoint, "fido2TraceEndpoint");
		} catch (RuntimeException e) {
			log.error("fido2TraceEndpoint is not usable, TRACE record not sent", e);
			return null;
		}
		return new Snapshot(generation, endpoint, appConfiguration.getIssuer(),
				fido2Configuration.getLockAuditClientId(), fido2Configuration.getLockAuditClientPassword());
	}

	private Attempt attemptOnce(Snapshot snapshot, String recordJson, int attempt) {
		if (configuredChanged(snapshot)) {
			return Attempt.done(Outcome.NOT_CONFIGURED);
		}

		String accessToken = lockAuditTokenService.getTraceAccessToken(snapshot.issuer, snapshot.clientId,
				snapshot.encryptedClientSecret);
		if (accessToken == null) {
			return Attempt.retry("no trace.write access token", jitteredDelay(attempt));
		}

		if (configuredChanged(snapshot)) {
			return Attempt.done(Outcome.NOT_CONFIGURED);
		}

		PostResult result;
		try {
			result = poster.post(snapshot.endpoint, accessToken, recordJson);
		} catch (ProcessingException | IllegalArgumentException | IllegalStateException e) {
			return Attempt.retry(e.getClass().getSimpleName(), jitteredDelay(attempt));
		}

		if (isAccepted(result.status)) {
			log.debug("TRACE record accepted by Lock, status: {}, attempt: {}", result.status, attempt);
			return Attempt.done(Outcome.ACCEPTED);
		}
		if (!isRetryable(result.status, result.body)) {
			log.warn("Lock rejected TRACE record, status: {}", result.status);
			return Attempt.done(Outcome.REJECTED);
		}

		long retryAfterMillis = parseRetryAfterMillis(result.retryAfter, Instant.now());
		if (retryAfterMillis > MAX_BACKOFF_MS) {
			log.warn("Lock asked to retry TRACE record after {} ms, beyond the {} ms cap, giving up", retryAfterMillis, MAX_BACKOFF_MS);
			return Attempt.done(Outcome.EXHAUSTED);
		}
		return Attempt.retry("status " + result.status, Math.max(jitteredDelay(attempt), retryAfterMillis));
	}

	private boolean configuredChanged(Snapshot snapshot) {
		if (configGeneration.get() == snapshot.generation) {
			return false;
		}
		log.warn("TRACE configuration changed during delivery, record not sent");
		return true;
	}

	/** @return {@code false} if the wait was interrupted, in which case the delivery gives up. */
	private boolean pause(long millis) {
		try {
			sleeper.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Interrupted while retrying TRACE delivery, giving up");
			return false;
		}
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
		return declaredRetryable(body).orElseGet(() -> status == 429 || status >= 500);
	}

	/**
	 * @return the {@code retryable} member of a JSON error body, looked up inside {@code error} first and then
	 *         at the top level, or empty if neither declares it.
	 */
	static Optional<Boolean> declaredRetryable(String body) {
		if (StringHelper.isEmpty(body)) {
			return Optional.empty();
		}
		try {
			JsonNode node = MAPPER.readTree(body);
			JsonNode error = node.get(ERROR_MEMBER);
			Optional<Boolean> nested = error != null && error.isObject() ? booleanMember(error) : Optional.empty();
			return nested.isPresent() ? nested : booleanMember(node);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			return Optional.empty();
		}
	}

	private static Optional<Boolean> booleanMember(JsonNode node) {
		JsonNode member = node.get(RETRYABLE_MEMBER);
		return member != null && member.isBoolean() ? Optional.of(member.booleanValue()) : Optional.empty();
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
		} catch (RuntimeException e) {
			return 0;
		}
	}

	/**
	 * Reads the response body. A failure to read it must not lose the status: the status alone decides
	 * the outcome when the body is unavailable.
	 */
	static String readBodyQuietly(Response response) {
		try {
			return response.hasEntity() ? response.readEntity(String.class) : null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private PostResult httpPost(String endpoint, String accessToken, String recordJson) {
		Client client = ClientBuilder.newBuilder()
				.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build();
		try {
			Response response = client.target(endpoint).request()
					.header("Authorization", "Bearer " + accessToken)
					.post(Entity.entity(recordJson, MediaType.APPLICATION_JSON));
			try {
				return new PostResult(response.getStatus(), response.getHeaderString(RETRY_AFTER_HEADER), readBodyQuietly(response));
			} finally {
				response.close();
			}
		} finally {
			client.close();
		}
	}
}
