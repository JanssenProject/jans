/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.api.ProducerChainListResponse;
import io.jans.lock.model.trace.api.ProducerChainRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerChainResponse;
import io.jans.lock.model.trace.api.ProducerKeyListResponse;
import io.jans.lock.model.trace.api.ProducerKeyRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerKeyResponse;
import io.jans.lock.model.trace.api.ProducerKeyRevokeRequest;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.error.TraceErrors;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainRegistration;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.parse.TraceValidationException;
import io.jans.lock.service.trace.registry.ProducerChainRegistry;
import io.jans.lock.service.trace.registry.ProducerKeyRegistry;
import io.jans.lock.service.ws.rs.base.BaseResource;
import io.jans.lock.util.ServerUtil;
import io.jans.net.InetAddressUtility;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

/**
 * Admin REST implementation for TRACE producer-key and producer-chain registration, listing and
 * revocation (design §6, §8, TRACE MVP task 15). Every method follows the same shape: check
 * {@code traceConfiguration.enabled} &rarr; resolve the submitting client's identity and evidence
 * domain &rarr; delegate to the relevant registry &rarr; map the result to a wire DTO. Every
 * {@link RuntimeException} raised by the TRACE packages is translated to the structured error
 * body by {@link TraceErrors#toWebApplicationException}.
 *
 * @author Yuriy Movchan
 */
@Dependent
public class TraceAdminRestWebServiceImpl extends BaseResource implements TraceAdminRestWebService {

	private static final String REASON_MISSING_BODY = "missing_body";

	private static final String REASON_TRACE_DISABLED = "trace_disabled";

	private static final String REASON_TIMESTAMP_FORMAT = "timestamp_format";

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@Inject
	private SubmitterIdentityService submitterIdentityService;

	@Inject
	private EvidenceDomainResolver evidenceDomainResolver;

	@Inject
	private ProducerKeyRegistry producerKeyRegistry;

	@Inject
	private ProducerChainRegistry producerChainRegistry;

	@Inject
	private ApplicationAuditLogger applicationAuditLogger;

	/**
	 * Test seam: bypasses CDI injection of {@link Logger}.
	 */
	void setLog(Logger log) {
		this.log = log;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link AppConfiguration}.
	 */
	void setAppConfiguration(AppConfiguration appConfiguration) {
		this.appConfiguration = appConfiguration;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link ErrorResponseFactory}.
	 */
	void setErrorResponseFactory(ErrorResponseFactory errorResponseFactory) {
		this.errorResponseFactory = errorResponseFactory;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link SubmitterIdentityService}.
	 */
	void setSubmitterIdentityService(SubmitterIdentityService submitterIdentityService) {
		this.submitterIdentityService = submitterIdentityService;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link EvidenceDomainResolver}.
	 */
	void setEvidenceDomainResolver(EvidenceDomainResolver evidenceDomainResolver) {
		this.evidenceDomainResolver = evidenceDomainResolver;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link ProducerKeyRegistry}.
	 */
	void setProducerKeyRegistry(ProducerKeyRegistry producerKeyRegistry) {
		this.producerKeyRegistry = producerKeyRegistry;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link ProducerChainRegistry}.
	 */
	void setProducerChainRegistry(ProducerChainRegistry producerChainRegistry) {
		this.producerChainRegistry = producerChainRegistry;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link ApplicationAuditLogger}.
	 */
	void setApplicationAuditLogger(ApplicationAuditLogger applicationAuditLogger) {
		this.applicationAuditLogger = applicationAuditLogger;
	}

	@Override
	public Response registerProducerKey(ProducerKeyRegistrationRequest request) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_ADMIN_KEY_WRITE);

		Response response = null;
		try {
			log.debug("Registering TRACE producer key");
			checkEnabled();
			TraceRequestContext context = resolveContext();
			requireBody(request);

			long validFromMs = parseRequiredTimestamp(request.getValidFrom());
			Long validUntilMs = parseOptionalTimestamp(request.getValidUntil());

			ProducerKey key = producerKeyRegistry.register(context.getEvidenceDomainId(), request.getProducerId(),
					request.getKid(), request.getPublicKeyJwk(), validFromMs, validUntilMs, context.getClientId(),
					context.getReceivedAtMs());

			response = Response.status(Response.Status.CREATED).cacheControl(ServerUtil.cacheControl(true))
					.entity(toResponse(key)).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response listProducerKeys(String producerId) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_ADMIN_KEY_READ);

		Response response = null;
		try {
			log.debug("Listing TRACE producer keys, producerId: {}", producerId);
			checkEnabled();
			TraceRequestContext context = resolveContext();

			List<ProducerKey> keys = producerKeyRegistry.list(context.getEvidenceDomainId(), producerId);

			ProducerKeyListResponse body = new ProducerKeyListResponse();
			body.setKeys(keys.stream().map(this::toResponse).collect(Collectors.toList()));

			response = Response.ok(body).cacheControl(ServerUtil.cacheControl(true)).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response revokeProducerKey(ProducerKeyRevokeRequest request) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_ADMIN_KEY_WRITE);

		Response response = null;
		try {
			log.debug("Revoking TRACE producer key");
			checkEnabled();
			TraceRequestContext context = resolveContext();
			requireBody(request);
			requireNonBlank(request.getProducerId(), "producer_id");
			requireNonBlank(request.getKid(), "kid");

			Optional<ProducerKey> revoked = producerKeyRegistry.revoke(context.getEvidenceDomainId(),
					request.getProducerId(), request.getKid(), context.getReceivedAtMs());
			ProducerKey key = revoked.orElseThrow(() -> new TraceValidationException(
					TraceErrorResponseType.RECORD_NOT_FOUND, "key_not_registered"));

			response = Response.ok(toResponse(key)).cacheControl(ServerUtil.cacheControl(true)).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response registerProducerChain(ProducerChainRegistrationRequest request) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_ADMIN_CHAIN_WRITE);

		Response response = null;
		try {
			log.debug("Registering TRACE producer chain");
			checkEnabled();
			TraceRequestContext context = resolveContext();
			requireBody(request);

			ChainIdentity chainIdentity = new ChainIdentity(context.getEvidenceDomainId(),
					Objects.toString(request.getProducerId(), ""),
					Objects.toString(request.getProducerInstanceId(), ""),
					Objects.toString(request.getProducerChainId(), ""));

			ChainRegistration registration = producerChainRegistry.register(chainIdentity, context.getClientId(),
					context.getReceivedAtMs());

			response = Response.status(Response.Status.CREATED).cacheControl(ServerUtil.cacheControl(true))
					.entity(toResponse(registration)).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response listProducerChains(String producerId) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_ADMIN_CHAIN_READ);

		Response response = null;
		try {
			log.debug("Listing TRACE producer chains, producerId: {}", producerId);
			checkEnabled();
			TraceRequestContext context = resolveContext();

			List<ChainRegistration> chains = producerChainRegistry.list(context.getEvidenceDomainId(), producerId);

			ProducerChainListResponse body = new ProducerChainListResponse();
			body.setChains(chains.stream().map(this::toResponse).collect(Collectors.toList()));

			response = Response.ok(body).cacheControl(ServerUtil.cacheControl(true)).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	// -- request flow helpers --------------------------------------------------------------------

	private void checkEnabled() {
		if (!appConfiguration.getTraceConfiguration().isEnabled()) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_TRACE_DISABLED);
		}
	}

	private TraceRequestContext resolveContext() {
		SubmitterIdentity identity = submitterIdentityService.resolve(getHttpHeaders());
		return evidenceDomainResolver.resolve(identity);
	}

	private void requireBody(Object request) {
		if (request == null) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_MISSING_BODY);
		}
	}

	private void requireNonBlank(String value, String fieldName) {
		if (StringUtils.isBlank(value)) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, "empty:" + fieldName);
		}
	}

	private long parseRequiredTimestamp(String value) {
		if (StringUtils.isBlank(value)) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_KEY, REASON_TIMESTAMP_FORMAT);
		}
		return parseTimestamp(value);
	}

	private Long parseOptionalTimestamp(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		return parseTimestamp(value);
	}

	private long parseTimestamp(String value) {
		try {
			return OffsetDateTime.parse(value).toInstant().toEpochMilli();
		} catch (DateTimeParseException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_KEY, REASON_TIMESTAMP_FORMAT, ex);
		}
	}

	private AuditLogEntry newAuditLogEntry(AuditActionType actionType) {
		return new AuditLogEntry(getClientIpAddress(), actionType);
	}

	private String getClientIpAddress() {
		HttpServletRequest request = getHttpRequest();
		return request == null ? null : InetAddressUtility.getIpAddress(request);
	}

	// -- DTO mapping ------------------------------------------------------------------------------

	private ProducerKeyResponse toResponse(ProducerKey key) {
		ProducerKeyResponse response = new ProducerKeyResponse();
		response.setEvidenceDomainId(key.getDomainId());
		response.setProducerId(key.getProducerId());
		response.setKid(key.getKid());
		response.setPublicKeyJwk(filterJwk(key.getPublicKeyJwk()));
		response.setValidFrom(formatInstant(key.getValidFromMs()));
		response.setValidUntil(key.getValidUntilMs() == null ? null : formatInstant(key.getValidUntilMs()));
		response.setRevokedAt(key.getRevokedAtMs() == null ? null : formatInstant(key.getRevokedAtMs()));
		response.setRegisteredBy(key.getRegisteredBy());
		response.setCreatedAt(formatInstant(key.getCreatedAtMs()));
		return response;
	}

	private ProducerChainResponse toResponse(ChainRegistration registration) {
		ChainIdentity identity = registration.getChainIdentity();

		ProducerChainResponse response = new ProducerChainResponse();
		response.setEvidenceDomainId(identity.getDomainId());
		response.setProducerId(identity.getProducerId());
		response.setProducerInstanceId(identity.getProducerInstanceId());
		response.setProducerChainId(identity.getProducerChainId());
		response.setRegisteredBy(registration.getRegisteredBy());
		response.setCreatedAt(formatInstant(registration.getRegisteredAtMs()));
		return response;
	}

	private static Map<String, String> filterJwk(Map<String, String> jwk) {
		Map<String, String> filtered = new LinkedHashMap<>();
		if (jwk != null) {
			copyIfPresent(jwk, filtered, "kty");
			copyIfPresent(jwk, filtered, "crv");
			copyIfPresent(jwk, filtered, "x");
		}
		return filtered;
	}

	private static void copyIfPresent(Map<String, String> source, Map<String, String> target, String member) {
		if (source.containsKey(member)) {
			target.put(member, source.get(member));
		}
	}

	private static String formatInstant(long epochMs) {
		return Instant.ofEpochMilli(epochMs).toString();
	}

}
