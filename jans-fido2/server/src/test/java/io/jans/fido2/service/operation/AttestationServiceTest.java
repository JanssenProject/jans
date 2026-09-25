/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.attestation.Response;
import io.jans.fido2.exception.Fido2NativeFailureException;
import io.jans.fido2.model.audit.LockAuditEvent;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.trust.NativeFailureDiagnostic;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.audit.LockAuditEventCollector;
import io.jans.fido2.service.external.ExternalFido2Service;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.AttestationVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the persistence-vs-audit-outcome contract CodeRabbit flagged on {@code verify()}:
 * {@code registrationData.setStatus(registered)} happens well before
 * {@code registrationPersistenceService.update()} actually persists it, so the audit event's
 * ALLOW/DENY decision must track whether {@code update()} itself returned, never the in-memory
 * status alone — otherwise a failure raised between the two moments would falsely report ALLOW for
 * a registration that was never actually persisted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttestationServiceTest {

	@InjectMocks
	private AttestationService attestationService;

	@Mock
	private Logger log;
	@Mock
	private AppConfiguration appConfiguration;
	@Mock
	private RegistrationPersistenceService registrationPersistenceService;
	@Mock
	private AttestationVerifier attestationVerifier;
	@Mock
	private UserSessionIdService userSessionIdService;
	@Mock
	private DomainVerifier domainVerifier;
	@Mock
	private ChallengeGenerator challengeGenerator;
	@Mock
	private CommonVerifiers commonVerifiers;
	@Mock
	private ExternalFido2Service externalFido2InterceptionService;
	@Mock
	private ErrorResponseFactory errorResponseFactory;
	@Mock
	private MetricService metricService;
	@Mock
	private LockAuditEventCollector lockAuditEventCollector;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Stubs everything {@code verify()} needs to reach {@code registrationPersistenceService.update()}:
	 * a resolvable challenge, a registration entry, and a cryptographically-inert attestation
	 * verification result ({@code attestationVerifier} itself is mocked, so no real signature or
	 * certificate checks run). Returns the {@code registrationEntry} mock so a test can layer
	 * additional, scenario-specific stubbing (e.g. making a step between status-set and persistence
	 * throw) on top of it.
	 */
	private Fido2RegistrationEntry stubHappyPathThroughStatusSet() {
		when(commonVerifiers.verifyClientJSON(any())).thenReturn(mapper.createObjectNode());
		when(commonVerifiers.getChallenge(any())).thenReturn("challenge123");
		when(commonVerifiers.verifyCredentialId(any(), any())).thenReturn("keyId");
		when(attestationVerifier.verifyAuthenticatorAttestationResponse(any(), any()))
				.thenReturn(new CredAndCounterData());

		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		registrationData.setUsername("alice");
		Fido2RegistrationEntry registrationEntry = mock(Fido2RegistrationEntry.class);
		when(registrationEntry.getRegistrationData()).thenReturn(registrationData);
		when(registrationPersistenceService.findByChallenge("challenge123")).thenReturn(List.of(registrationEntry));

		return registrationEntry;
	}

	private static AttestationResult attestationResult() {
		Response response = new Response();
		response.setTransports(new String[0]);
		AttestationResult attestationResult = new AttestationResult();
		attestationResult.setResponse(response);
		return attestationResult;
	}

	/**
	 * The regression case: a failure raised after the in-memory status is set to {@code registered}
	 * but before {@code registrationPersistenceService.update()} runs (simulated here via
	 * {@code getPublicKeyIdHash()}, which the real code calls in exactly that window) must still
	 * report DENY. Before this fix, the catch block trusted {@code registrationData.getStatus()}
	 * alone and would have reported ALLOW here, contradicting the fact that nothing was ever
	 * persisted.
	 */
	@Test
	void verify_ifFailureOccursAfterStatusSetButBeforePersistence_collectsADenyLockAuditEvent() {
		Fido2RegistrationEntry registrationEntry = stubHappyPathThroughStatusSet();
		when(registrationPersistenceService.getPublicKeyIdHash(any())).thenThrow(new RuntimeException("boom"));

		try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
			mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

			assertThrows(RuntimeException.class, () -> attestationService.verify(attestationResult()));
		}

		verify(registrationPersistenceService, never()).update(any());

		ArgumentCaptor<LockAuditEvent> captor = ArgumentCaptor.forClass(LockAuditEvent.class);
		verify(lockAuditEventCollector).collect(captor.capture());
		assertEquals("DENY", captor.getValue().getDecisionResult());
	}

	/**
	 * The counterpart case: once {@code registrationPersistenceService.update()} has actually
	 * returned, a later failure (here, the external interception script, which runs after
	 * persistence in the real method) must still report ALLOW, since the registration really was
	 * persisted.
	 */
	@Test
	void verify_ifExternalScriptThrowsAfterPersistenceSucceeds_collectsAnAllowLockAuditEventNotDeny() {
		Fido2RegistrationEntry registrationEntry = stubHappyPathThroughStatusSet();
		doThrow(new RuntimeException("interception script failed")).when(externalFido2InterceptionService)
				.verifyAttestationFinish(any(), any());

		try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
			mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

			assertThrows(RuntimeException.class, () -> attestationService.verify(attestationResult()));
		}

		verify(registrationPersistenceService).update(registrationEntry);

		ArgumentCaptor<LockAuditEvent> captor = ArgumentCaptor.forClass(LockAuditEvent.class);
		verify(lockAuditEventCollector).collect(captor.capture());
		assertEquals("ALLOW", captor.getValue().getDecisionResult());
	}

	/**
	 * An RP ID hash mismatch (#14608) — a common symptom of a misconfigured native asset-link/AASA
	 * association — must be recorded under its diagnostic code, not the raw "Hashes don't match"
	 * message, so registration failures can be counted by cause.
	 */
	@Test
	void verify_ifRpIdHashMismatch_recordsTheNativeFailureDiagnosticCode() {
		stubHappyPathThroughStatusSet();
		when(attestationVerifier.verifyAuthenticatorAttestationResponse(any(), any()))
				.thenThrow(new Fido2NativeFailureException(NativeFailureDiagnostic.JFS_RPID_HASH_MISMATCH,
						"Hashes don't match"));

		try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
			mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

			assertThrows(Fido2NativeFailureException.class, () -> attestationService.verify(attestationResult()));
		}

		verify(metricService).recordPasskeyRegistrationFailure(eq("alice"), any(), anyLong(),
				eq("JFS_RPID_HASH_MISMATCH"), any(), any());
	}
}
