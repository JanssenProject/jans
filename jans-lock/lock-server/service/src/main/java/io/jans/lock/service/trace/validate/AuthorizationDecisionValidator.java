/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.parse.ParsedAssertion;

/**
 * Validates {@code AUTHORIZATION_DECISION} records (design §7.1, §7.2): a PDP
 * decision signing exactly one of a singular {@code decision} or a batch {@code decisions[]} of
 * capability facts ({@code action}, {@code resource_type}, {@code outcome}; no resource id and no
 * record-level outcome), a {@code trace.subject.workload_id}, a complete {@code trace.policy}
 * block and {@code trace.runtime.pdp_id}. The optional operation-binding digests
 * {@code request_digest}, {@code input_digest} and {@code tool_call_digest} are validated when
 * present, as is the {@code trace.policy.enforcement_mode} member (one of {@code enforce},
 * {@code advisory}, {@code silent}, {@code declared}).
 *
 * @author Yuriy Movchan
 */
public final class AuthorizationDecisionValidator implements EventKindValidator {

	/**
	 * A generic {@code algorithm:digest} form, accepted alongside {@link TraceConstants#HASH_PATTERN}
	 * for {@code bundle_hash} (the design leaves the exact
	 * digest algorithm for a policy bundle open, so any prefixed digest is accepted, not only
	 * {@code sha256:<64 hex>}).
	 */
	private static final Pattern GENERIC_DIGEST_PATTERN = Pattern.compile("^[a-z0-9-]+:[A-Za-z0-9+/=_-]+$");

	private static final int MAX_POLICY_FIELD_LENGTH = 255;

	/** {@code trace.policy.enforcement_mode} vocabulary (GovOps profile §4, base TRACE v0.2). */
	private static final Set<String> ENFORCEMENT_MODES = Collections
			.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("enforce", "advisory", "silent", "declared")));

	/**
	 * Validates the complete {@code trace.policy} block (design §7.2): required for a decision,
	 * optional but validated when present for an invocation.
	 */
	static PolicyRef validatePolicy(JsonNode trace) {
		JsonNode policy = EventKindValidationSupport.requireObject(trace, "policy", "trace.policy");
		String policyStoreId = EventKindValidationSupport.requireNonEmptyString(policy, "policy_store_id",
				"trace.policy.policy_store_id", MAX_POLICY_FIELD_LENGTH);
		String policyStoreVersion = EventKindValidationSupport.requireNonEmptyString(policy, "policy_store_version",
				"trace.policy.policy_store_version", MAX_POLICY_FIELD_LENGTH);
		EventKindValidationSupport.requireNonEmptyString(policy, "policy_language", "trace.policy.policy_language",
				null);
		EventKindValidationSupport.requireNonEmptyString(policy, "policy_language_version",
				"trace.policy.policy_language_version", null);
		String bundleHash = EventKindValidationSupport.requireNonEmptyString(policy, "bundle_hash",
				"trace.policy.bundle_hash", MAX_POLICY_FIELD_LENGTH);
		boolean valid = bundleHash.startsWith("sha256:") ? TraceConstants.HASH_PATTERN.matcher(bundleHash).matches()
				: GENERIC_DIGEST_PATTERN.matcher(bundleHash).matches();
		if (!valid) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"invalid:trace.policy.bundle_hash");
		}
		if (EventKindValidationSupport.present(policy, "enforcement_mode")) {
			EventKindValidationSupport.requireEnum(policy, "enforcement_mode", "trace.policy.enforcement_mode",
					ENFORCEMENT_MODES);
		}
		return new PolicyRef(policyStoreId, policyStoreVersion, bundleHash);
	}

	@Override
	public String kind() {
		return TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION;
	}

	@Override
	public void validate(ParsedAssertion assertion, CorrelationInputs.Builder out) {
		JsonNode trace = assertion.getRoot().get("trace");
		JsonNode event = trace.get("event");

		EventKindValidationSupport.requireWorkloadSubject(trace);
		EventKindValidationSupport.requireCapabilityFacts(event, "decision", "decisions", "decision_id",
				EventKindValidationSupport.DECISION_OUTCOMES);
		EventKindValidationSupport.optionalDigests(event, "request_digest", "input_digest", "tool_call_digest");

		out.policy(validatePolicy(trace));

		JsonNode runtime = EventKindValidationSupport.requireObject(trace, "runtime", "trace.runtime");
		EventKindValidationSupport.requireNonEmptyString(runtime, "pdp_id", "trace.runtime.pdp_id", null);

		List<TokenRef> tokenRefs = EventKindValidationSupport.extractTokens(event, "trace.event.tokens");

		out.tokenRefs(tokenRefs);
	}

}
