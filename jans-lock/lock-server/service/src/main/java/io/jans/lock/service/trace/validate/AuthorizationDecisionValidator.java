/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.parse.ParsedAssertion;

/**
 * Validates {@code AUTHORIZATION_DECISION} records (design §7.2): a PDP decision, requiring a
 * complete {@code trace.policy} block and {@code trace.runtime.pdp_id}, in addition to the
 * {@code outcome} and non-empty {@code capability_ids[]} common to decision/invocation events.
 *
 * @author Yuriy Movchan
 */
public final class AuthorizationDecisionValidator implements EventKindValidator {

	/**
	 * A generic {@code algorithm:digest} form, accepted alongside {@link TraceConstants#HASH_PATTERN}
	 * for {@code bundle_hash} (design decision recorded in task 09: the design leaves the exact
	 * digest algorithm for a policy bundle open, so any prefixed digest is accepted, not only
	 * {@code sha256:<64 hex>}).
	 */
	private static final Pattern GENERIC_DIGEST_PATTERN = Pattern.compile("^[a-z0-9-]+:[A-Za-z0-9+/=_-]+$");

	@Override
	public String kind() {
		return TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION;
	}

	@Override
	public void validate(ParsedAssertion assertion, CorrelationInputs.Builder out) {
		JsonNode trace = assertion.getRoot().get("trace");
		JsonNode event = trace.get("event");

		EventKindValidationSupport.requireEnum(event, "outcome", "trace.event.outcome",
				EventKindValidationSupport.DECISION_OUTCOMES);
		List<String> capabilityIds = EventKindValidationSupport.requireCapabilityIds(event,
				"trace.event.capability_ids");

		JsonNode policy = EventKindValidationSupport.requireObject(trace, "policy", "trace.policy");
		EventKindValidationSupport.requireNonEmptyString(policy, "policy_store_id", "trace.policy.policy_store_id",
				null);
		EventKindValidationSupport.requireNonEmptyString(policy, "policy_store_version",
				"trace.policy.policy_store_version", null);
		EventKindValidationSupport.requireNonEmptyString(policy, "policy_language", "trace.policy.policy_language",
				null);
		EventKindValidationSupport.requireNonEmptyString(policy, "policy_language_version",
				"trace.policy.policy_language_version", null);
		String bundleHash = EventKindValidationSupport.requireNonEmptyString(policy, "bundle_hash",
				"trace.policy.bundle_hash", null);
		if (!TraceConstants.HASH_PATTERN.matcher(bundleHash).matches()
				&& !GENERIC_DIGEST_PATTERN.matcher(bundleHash).matches()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"invalid:trace.policy.bundle_hash");
		}

		JsonNode runtime = EventKindValidationSupport.requireObject(trace, "runtime", "trace.runtime");
		EventKindValidationSupport.requireNonEmptyString(runtime, "pdp_id", "trace.runtime.pdp_id", null);

		List<TokenRef> tokenRefs = EventKindValidationSupport.extractTokens(event, "trace.event.tokens");

		out.capabilityIds(capabilityIds);
		out.tokenRefs(tokenRefs);
	}

}
