/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import org.slf4j.Logger;

import io.jans.orm.model.fido2.Fido2AuthenticationEntry;

/**
 * Call-site helper for the ceremony services. Maps the ceremony's audit failure to a TRACE outcome and
 * isolates the ceremony from TRACE errors: a failure to queue the record is logged and never changes the
 * ceremony's response.
 */
public final class TraceCeremonyHook {

	private TraceCeremonyHook() {
	}

	/**
	 * @param failure the exception the Lock audit event uses, or {@code null} when the ceremony succeeded
	 */
	public static void emit(TraceEmitter emitter, Logger log, TraceRecordBuilder.CeremonyType type, Exception failure,
			String sessionId) {
		try {
			emitter.emit(type, failure == null ? TraceRecordBuilder.Outcome.SUCCESS : TraceRecordBuilder.Outcome.FAILURE,
					sessionId);
		} catch (RuntimeException e) {
			log.error("TRACE emission failed; the ceremony outcome is unchanged", e);
		}
	}

	public static void emit(TraceEmitter emitter, Logger log, TraceRecordBuilder.CeremonyType type, Exception failure,
			Fido2AuthenticationEntry entry) {
		emit(emitter, log, type, failure, entry == null ? null : entry.getSessionStateId());
	}
}
