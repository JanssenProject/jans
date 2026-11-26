/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.jans.orm.model.fido2.Fido2AuthenticationEntry;

class TraceCeremonyHookTest {

	@Test
	void emit_ifNoFailure_recordsSuccess() {
		TraceEmitter emitter = mock(TraceEmitter.class);
		Logger log = mock(Logger.class);

		TraceCeremonyHook.emit(emitter, log, TraceRecordBuilder.CeremonyType.REGISTRATION, null, "sess-1");

		verify(emitter).emit(TraceRecordBuilder.CeremonyType.REGISTRATION, TraceRecordBuilder.Outcome.SUCCESS, "sess-1");
	}

	@Test
	void emit_ifFailure_recordsFailure() {
		TraceEmitter emitter = mock(TraceEmitter.class);
		Logger log = mock(Logger.class);

		TraceCeremonyHook.emit(emitter, log, TraceRecordBuilder.CeremonyType.ASSERTION, new IllegalStateException("rejected"), "sess-2");

		verify(emitter).emit(TraceRecordBuilder.CeremonyType.ASSERTION, TraceRecordBuilder.Outcome.FAILURE, "sess-2");
	}

	@Test
	void emit_ifEntryHasSession_usesItsSessionState() {
		TraceEmitter emitter = mock(TraceEmitter.class);
		Logger log = mock(Logger.class);
		Fido2AuthenticationEntry entry = new Fido2AuthenticationEntry();
		entry.setSessionStateId("sess-3");

		TraceCeremonyHook.emit(emitter, log, TraceRecordBuilder.CeremonyType.ASSERTION, null, entry);

		verify(emitter).emit(eq(TraceRecordBuilder.CeremonyType.ASSERTION), eq(TraceRecordBuilder.Outcome.SUCCESS), eq("sess-3"));
	}

	@Test
	void emit_ifEntryIsNull_recordsNoSession() {
		TraceEmitter emitter = mock(TraceEmitter.class);
		Logger log = mock(Logger.class);

		TraceCeremonyHook.emit(emitter, log, TraceRecordBuilder.CeremonyType.ASSERTION, null, (Fido2AuthenticationEntry) null);

		verify(emitter).emit(any(), any(), eq(null));
	}

	@Test
	void emit_ifEmitterThrows_isolatedFromTheCeremony() {
		TraceEmitter emitter = mock(TraceEmitter.class);
		Logger log = mock(Logger.class);
		doThrow(new IllegalStateException("boom")).when(emitter).emit(any(), any(), any());

		assertDoesNotThrow(() -> TraceCeremonyHook.emit(emitter, log, TraceRecordBuilder.CeremonyType.REGISTRATION, null, "sess-4"));

		verify(log).error(eq("TRACE emission failed; the ceremony outcome is unchanged"), any(Throwable.class));
	}
}
