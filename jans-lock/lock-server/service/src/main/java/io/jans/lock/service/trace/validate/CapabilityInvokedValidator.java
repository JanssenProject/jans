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
 * Validates {@code CAPABILITY_INVOKED} records (design §7.2): a PEP/gateway invocation, requiring
 * non-empty {@code capability_ids[]}, {@code enforcement_point_id} and a free-form invocation
 * {@code outcome} (the design fixes no invocation-outcome vocabulary). {@code trace.policy} is
 * absent by construction of this kind — carrying it contradicts {@code CAPABILITY_INVOKED} and is
 * rejected.
 *
 * @author Yuriy Movchan
 */
public final class CapabilityInvokedValidator implements EventKindValidator {

	private static final int MAX_OUTCOME_LENGTH = 64;

	@Override
	public String kind() {
		return TraceConstants.EVENT_KIND_CAPABILITY_INVOKED;
	}

	@Override
	public void validate(ParsedAssertion assertion, CorrelationInputs.Builder out) {
		JsonNode trace = assertion.getRoot().get("trace");
		JsonNode event = trace.get("event");

		List<String> capabilityIds = EventKindValidationSupport.requireCapabilityIds(event,
				"trace.event.capability_ids");
		EventKindValidationSupport.requireNonEmptyString(event, "enforcement_point_id",
				"trace.event.enforcement_point_id", null);
		EventKindValidationSupport.requireNonEmptyString(event, "outcome", "trace.event.outcome", MAX_OUTCOME_LENGTH);

		EventKindValidationSupport.forbidField(trace, "policy", "trace.policy");

		List<TokenRef> tokenRefs = EventKindValidationSupport.extractTokens(event, "trace.event.tokens");

		out.capabilityIds(capabilityIds);
		out.tokenRefs(tokenRefs);
	}

}
