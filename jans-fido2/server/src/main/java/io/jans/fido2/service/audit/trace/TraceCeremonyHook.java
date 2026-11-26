/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import org.slf4j.Logger;

import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationData;

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
		emit(emitter, log, type, failure, sessionId, (Fido2RegistrationData) null);
	}

	public static void emit(TraceEmitter emitter, Logger log, TraceRecordBuilder.CeremonyType type, Exception failure,
			Fido2AuthenticationEntry entry) {
		emit(emitter, log, type, failure, entry == null ? null : entry.getSessionStateId(), (Fido2RegistrationData) null);
	}

	/**
	 * @param data the credential's registration data; supplies authenticator_attachment and the credential ID. May be null
	 */
	public static void emit(TraceEmitter emitter, Logger log, TraceRecordBuilder.CeremonyType type, Exception failure,
			String sessionId, Fido2RegistrationData data) {
		try {
			emitter.emit(type, failure == null ? TraceRecordBuilder.Outcome.SUCCESS : TraceRecordBuilder.Outcome.FAILURE,
					sessionId, data == null ? null : data.getAuthentictatorAttachment(),
					data == null ? null : data.getPublicKeyId());
		} catch (RuntimeException e) {
			log.error("TRACE emission failed; the ceremony outcome is unchanged", e);
		}
	}

	public static void emit(TraceEmitter emitter, Logger log, TraceRecordBuilder.CeremonyType type, Exception failure,
			Fido2AuthenticationEntry entry, Fido2RegistrationData data) {
		emit(emitter, log, type, failure, entry == null ? null : entry.getSessionStateId(), data);
	}
}
