/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceClientDomainBinding;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Derives a TRACE request's evidence domain and forwarding allowlist from the already-resolved
 * submitting client (design decision D-1). The domain is <strong>always</strong> derived from the
 * client's configured binding, never from anything in the request body.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class EvidenceDomainResolver {

	static final String REASON_NO_DOMAIN_BINDING = "no_domain_binding";

	static final String REASON_INVALID_DOMAIN_ID = "invalid_domain_id";

	/** Design decision D-1: {@code evidence_domain_id} is an opaque 1-64 char id. */
	private static final Pattern DOMAIN_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

	/** Used only when no explicit binding matches; D-1 fixes this to {@code ["*"]}. */
	private static final List<String> WILDCARD_PRODUCERS = java.util.Collections.singletonList("*");

	/**
	 * Neither {@code StatService.getNodeId()} (rotates monthly and lives one layer below a
	 * general-purpose "this Lock node" identifier -- see task 11's report) nor
	 * {@code MetricService.getNodeIdentifier()} (lives in the {@code server} module, which
	 * {@code service} must not depend on) is reusable here, so this falls back to the local host
	 * name per task 11's own fallback recipe.
	 */
	private static final String FALLBACK_NODE_ID = "unknown-lock-node";

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	private String nodeId;

	@PostConstruct
	private void init() {
		nodeId = resolveNodeId();
	}

	/**
	 * @param identity the already-resolved submitting client
	 * @return the request context carrying the client's evidence domain and forwarding allowlist
	 * @throws TraceValidationException {@code client_not_bound} (403) when the client has no
	 *                                  binding and no usable default, or when the resolved domain
	 *                                  id fails the format check (D-1 fails closed)
	 */
	public TraceRequestContext resolve(SubmitterIdentity identity) {
		TraceConfiguration traceConfiguration = appConfiguration.getTraceConfiguration();

		TraceClientDomainBinding binding = findBinding(traceConfiguration, identity.getClientId());

		String evidenceDomainId;
		List<String> allowedProducerIds;
		if (binding != null) {
			evidenceDomainId = binding.getEvidenceDomainId();
			allowedProducerIds = binding.getAllowedProducerIds();
		} else if (traceConfiguration != null && StringUtils.isNotBlank(traceConfiguration.getDefaultEvidenceDomainId())) {
			evidenceDomainId = traceConfiguration.getDefaultEvidenceDomainId();
			allowedProducerIds = WILDCARD_PRODUCERS;
		} else {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_NO_DOMAIN_BINDING);
		}

		if (StringUtils.isBlank(evidenceDomainId) || !DOMAIN_ID_PATTERN.matcher(evidenceDomainId).matches()) {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_INVALID_DOMAIN_ID);
		}

		return new TraceRequestContext(identity.getClientId(), evidenceDomainId, allowedProducerIds,
				System.currentTimeMillis(), nodeId);
	}

	private TraceClientDomainBinding findBinding(TraceConfiguration traceConfiguration, String clientId) {
		if (traceConfiguration == null || traceConfiguration.getClientDomainBindings() == null) {
			return null;
		}

		for (TraceClientDomainBinding candidate : traceConfiguration.getClientDomainBindings()) {
			if (candidate != null && clientId.equals(candidate.getClientId())) {
				return candidate;
			}
		}

		return null;
	}

	private String resolveNodeId() {
		try {
			String hostName = InetAddress.getLocalHost().getHostName();
			if (StringUtils.isNotBlank(hostName)) {
				return hostName;
			}
		} catch (UnknownHostException | SecurityException e) {
			// SecurityException: a SecurityManager may deny hostname resolution; fall back either way.
			log.debug("Unable to determine local host name for the TRACE node id: {}", e.getMessage());
		}

		return FALLBACK_NODE_ID;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link AppConfiguration}.
	 */
	void setAppConfiguration(AppConfiguration appConfiguration) {
		this.appConfiguration = appConfiguration;
	}

	/**
	 * Test seam: bypasses {@link #resolveNodeId()} (host-name lookup) so tests get a deterministic
	 * node id.
	 */
	void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link Logger}.
	 */
	void setLog(Logger log) {
		this.log = log;
	}

}
