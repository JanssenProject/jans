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
 * Validates {@code RUNTIME_EFFECT} records (design §7.2): an observed effect. {@code effect_status}
 * is required and must be one of {@code planned}, {@code dispatched}, {@code confirmed},
 * {@code failed} or {@code reverted}. {@code outcome} is required for every status except
 * {@code planned}, where it is optional but still a non-empty string when present.
 * {@code result_digest} is required for {@code confirmed} and optional otherwise; when present it
 * is always a {@code sha256:<64 lowercase hex>} string. Lock checks these rules from the record
 * bytes alone and does not verify that the digest matches any real result. The
 * {@code decision}/{@code decisions} and
 * {@code invocation}/{@code invocations} facts and {@code trace.policy} contradict this kind and
 * are rejected; {@code trace.subject} is optional and validated when present. A
 * missing {@code produced_effect} parent edge is a warning, not a rejection: the design describes
 * it as the normal but not mandatory shape.
 *
 * @author Yuriy Movchan
 */
public final class RuntimeEffectValidator implements EventKindValidator {

	private static final Logger LOG = LoggerFactory.getLogger(RuntimeEffectValidator.class);

	private static final String PRODUCED_EFFECT_RELATIONSHIP = "produced_effect";

	private static final String STATUS_PLANNED = "planned";

	private static final String STATUS_CONFIRMED = "confirmed";

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

		String effectStatus = EventKindValidationSupport.requireEnum(event, "effect_status",
				"trace.event.effect_status", EventKindValidationSupport.EFFECT_STATUSES);
		if (STATUS_PLANNED.equals(effectStatus)) {
			EventKindValidationSupport.optionalNonEmptyString(event, "outcome", "trace.event.outcome");
		} else {
			EventKindValidationSupport.requireNonEmptyString(event, "outcome", "trace.event.outcome", null);
		}

		for (String field : new String[] { "decision", "decisions", "invocation", "invocations" }) {
			EventKindValidationSupport.forbidField(event, field, "trace.event." + field);
		}
		EventKindValidationSupport.validateOptionalSubject(trace);
		EventKindValidationSupport.forbidField(trace, "policy", "trace.policy");

		if (STATUS_CONFIRMED.equals(effectStatus)) {
			EventKindValidationSupport.requireHashString(event, "result_digest", "trace.event.result_digest");
		} else {
			EventKindValidationSupport.optionalHashString(event, "result_digest", "trace.event.result_digest");
		}
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
