/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.jans.lock.service.trace.parse.ParsedAssertion;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dispatches to the {@link EventKindValidator} for a common-field-validated assertion's
 * {@code trace.event_kind} and assembles the resulting {@link CorrelationInputs}. The common
 * fields (design §7.1) that every kind shares — execution identity, chain position, parents,
 * event kind and signing time — are filled in here, once, from {@link ParsedAssertion}; each
 * validator only adds what it extracts from {@code trace.event} (design §7.2/§7.3).
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class EventKindValidatorRegistry {

	private final Map<String, EventKindValidator> validatorsByKind;

	public EventKindValidatorRegistry() {
		this(Arrays.asList(new AuthorizationDecisionValidator(), new CapabilityInvokedValidator(),
				new RuntimeEffectValidator()));
	}

	/** Test seam: inject a custom set of validators instead of the MVP catalog. */
	EventKindValidatorRegistry(List<EventKindValidator> validators) {
		Map<String, EventKindValidator> byKind = new LinkedHashMap<>();
		for (EventKindValidator validator : validators) {
			byKind.put(validator.kind(), validator);
		}
		this.validatorsByKind = Collections.unmodifiableMap(byKind);
	}

	/**
	 * @param assertion an assertion that has already passed {@code CommonAssertionValidator}
	 * @return the correlation inputs extracted for this record
	 * @throws IllegalStateException if {@code assertion.getEventKind()} has no registered
	 *         validator; unreachable once {@code CommonAssertionValidator} has run, since it
	 *         already restricts {@code trace.event_kind} to the MVP catalog
	 */
	public CorrelationInputs validate(ParsedAssertion assertion) {
		EventKindValidator validator = validatorsByKind.get(assertion.getEventKind());
		if (validator == null) {
			throw new IllegalStateException("No EventKindValidator registered for kind '"
					+ assertion.getEventKind() + "'; CommonAssertionValidator should have rejected it already");
		}

		CorrelationInputs.Builder builder = new CorrelationInputs.Builder()
				.executionAuthority(assertion.getExecutionAuthority())
				.traceExecutionId(assertion.getTraceExecutionId())
				.chainPosition(assertion.getProducerChain())
				.parents(assertion.getParentRecordIds())
				.eventKind(assertion.getEventKind())
				.signedAt(assertion.getSignedAt());

		validator.validate(assertion, builder);

		return builder.build();
	}

}
