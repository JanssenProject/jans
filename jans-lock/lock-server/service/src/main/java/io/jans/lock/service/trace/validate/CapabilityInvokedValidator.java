/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.parse.ParsedAssertion;

/**
 * Validates {@code CAPABILITY_INVOKED} records (design §7.1, §7.2): a PEP/gateway
 * invocation signing exactly one of a singular {@code invocation} or a batch {@code invocations[]}
 * of capability facts ({@code action}, {@code resource_type}, an {@code outcome} of {@code SUCCESS}
 * or {@code FAILURE} (GovOps profile §8); no resource id and no record-level outcome), a
 * {@code trace.subject.workload_id} and {@code enforcement_point_id}. The optional operation-binding
 * digests {@code request_digest}, {@code tool_call_digest} and {@code result_digest} are validated
 * when present. {@code trace.policy} is optional: an enforcement point that knows the policy store
 * it enforced under can sign it, and it is then validated like a decision's.
 *
 * @author Yuriy Movchan
 */
public final class CapabilityInvokedValidator implements EventKindValidator {

	@Override
	public String kind() {
		return TraceConstants.EVENT_KIND_CAPABILITY_INVOKED;
	}

	@Override
	public void validate(ParsedAssertion assertion, CorrelationInputs.Builder out) {
		JsonNode trace = assertion.getRoot().get("trace");
		JsonNode event = trace.get("event");

		EventKindValidationSupport.requireWorkloadSubject(trace);
		EventKindValidationSupport.requireCapabilityFacts(event, "invocation", "invocations", "invocation_id",
				EventKindValidationSupport.INVOCATION_OUTCOMES);
		EventKindValidationSupport.requireNonEmptyString(event, "enforcement_point_id",
				"trace.event.enforcement_point_id", null);

		EventKindValidationSupport.optionalDigests(event, "request_digest", "tool_call_digest", "result_digest");

		if (EventKindValidationSupport.present(trace, "policy")) {
			AuthorizationDecisionValidator.validatePolicy(trace);
		}

		List<TokenRef> tokenRefs = EventKindValidationSupport.extractTokens(event, "trace.event.tokens");

		out.tokenRefs(tokenRefs);
	}

}
