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
 * Validates {@code CAPABILITY_INVOKED} records (design §7.1, §7.2, decision D-20): a PEP/gateway
 * invocation signing exactly one of a singular {@code invocation} or a batch {@code invocations[]}
 * of capability facts ({@code action}, {@code resource_type}, a free-form {@code outcome} since the
 * design fixes no invocation-outcome vocabulary; no resource id and no record-level outcome), a
 * {@code trace.subject.workload_id} and {@code enforcement_point_id}. {@code trace.policy} is
 * absent by construction of this kind — carrying it contradicts {@code CAPABILITY_INVOKED} and is
 * rejected.
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
		EventKindValidationSupport.requireCapabilityFacts(event, "invocation", "invocations", "invocation_id", null);
		EventKindValidationSupport.requireNonEmptyString(event, "enforcement_point_id",
				"trace.event.enforcement_point_id", null);

		EventKindValidationSupport.forbidField(trace, "policy", "trace.policy");

		List<TokenRef> tokenRefs = EventKindValidationSupport.extractTokens(event, "trace.event.tokens");

		out.tokenRefs(tokenRefs);
	}

}
