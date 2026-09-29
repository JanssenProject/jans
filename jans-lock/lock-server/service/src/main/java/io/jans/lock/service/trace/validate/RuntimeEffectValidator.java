/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.parse.ParsedAssertion;

/**
 * Validates {@code RUNTIME_EFFECT} records (design §7.2): an observed effect, with {@code outcome}
 * as the only hard requirement, matching the full design's intentionally minimal schema for this
 * kind. {@code capability_ids} and {@code trace.policy} contradict this kind and are rejected. A
 * missing {@code produced_effect} parent edge is a warning, not a rejection: the design describes
 * it as the normal but not mandatory shape.
 *
 * @author Yuriy Movchan
 */
public final class RuntimeEffectValidator implements EventKindValidator {

	private static final Logger LOG = LoggerFactory.getLogger(RuntimeEffectValidator.class);

	private static final String PRODUCED_EFFECT_RELATIONSHIP = "produced_effect";

	/** design §8: surfaced as a warning, logged at DEBUG only, never rejected. */
	static final String WARNING_RUNTIME_EFFECT_WITHOUT_PRODUCED_EFFECT_PARENT = "runtime_effect_without_produced_effect_parent";

	@Override
	public String kind() {
		return TraceConstants.EVENT_KIND_RUNTIME_EFFECT;
	}

	@Override
	public void validate(ParsedAssertion assertion, CorrelationInputs.Builder out) {
		JsonNode trace = assertion.getRoot().get("trace");
		JsonNode event = trace.get("event");

		EventKindValidationSupport.requireNonEmptyString(event, "outcome", "trace.event.outcome", null);

		EventKindValidationSupport.forbidField(event, "capability_ids", "trace.event.capability_ids");
		EventKindValidationSupport.forbidField(trace, "policy", "trace.policy");

		EventKindValidationSupport.optionalHashString(event, "result_digest", "trace.event.result_digest");
		EventKindValidationSupport.optionalString(event, "target_id", "trace.event.target_id");
		EventKindValidationSupport.optionalString(event, "result_id", "trace.event.result_id");

		List<TokenRef> tokenRefs = EventKindValidationSupport.extractTokens(event, "trace.event.tokens");
		out.tokenRefs(tokenRefs);

		boolean hasProducedEffectParent = assertion.getParentRecordIds().stream()
				.anyMatch(parent -> PRODUCED_EFFECT_RELATIONSHIP.equals(parent.getRelationshipType()));
		if (!hasProducedEffectParent) {
			out.addWarning(WARNING_RUNTIME_EFFECT_WITHOUT_PRODUCED_EFFECT_PARENT);
			LOG.debug("RUNTIME_EFFECT record {} has no produced_effect parent", assertion.getRecordId());
		}
	}

}
