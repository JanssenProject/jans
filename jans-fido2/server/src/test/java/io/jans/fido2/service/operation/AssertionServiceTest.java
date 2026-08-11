package io.jans.fido2.service.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.error.Fido2ErrorResponse;
import io.jans.fido2.service.external.ExternalFido2Service;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssertionServiceTest {

    @InjectMocks
    private AssertionService assertionService;

    @Mock
    private Logger log;
    @Mock
    private CommonVerifiers commonVerifiers;
    @Mock
    private ExternalFido2Service externalFido2InterceptionService;
    @Mock
    private AuthenticationPersistenceService authenticationPersistenceService;
    @Mock
    private RegistrationPersistenceService registrationPersistenceService;
    @Mock
    private ErrorResponseFactory errorResponseFactory;
    @Mock
    private DomainVerifier domainVerifier;
    @Mock
    private MetricService metricService;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * CONF-09: the clientData challenge must be explicitly compared to the issued challenge.
     * When the authentication entry found by challenge carries a different stored challenge,
     * verify() must reject the assertion instead of proceeding.
     */
    @Test
    void verify_ifClientChallengeDoesNotMatchIssuedChallenge_throws() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);

        JsonNode clientJsonNode = mapper.createObjectNode();
        when(commonVerifiers.verifyNullOrEmptyString(any())).thenReturn("keyId");
        when(commonVerifiers.verifyClientJSON(any())).thenReturn(clientJsonNode);
        when(commonVerifiers.getChallenge(any())).thenReturn("clientChallenge");

        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setChallenge("issuedChallenge"); // differs from the clientData challenge
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(authenticationPersistenceService.findByChallenge("clientChallenge")).thenReturn(List.of(entry));

        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("challenge mismatch").build()));

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    /**
     * CONF-09: the other branch of the guard — a stored entry with a null challenge must also be
     * rejected rather than treated as a match.
     */
    @Test
    void verify_ifIssuedChallengeIsNull_throws() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);

        when(commonVerifiers.verifyNullOrEmptyString(any())).thenReturn("keyId");
        when(commonVerifiers.verifyClientJSON(any())).thenReturn(mapper.createObjectNode());
        when(commonVerifiers.getChallenge(any())).thenReturn("clientChallenge");

        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setChallenge(null); // no issued challenge stored
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(authenticationPersistenceService.findByChallenge("clientChallenge")).thenReturn(List.of(entry));

        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("challenge mismatch").build()));

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    /**
     * CONF-09 positive path: when the clientData challenge MATCHES the issued challenge, the guard must
     * NOT reject — verify() proceeds past it. We prove that by stubbing the very next step
     * (domainVerifier.verifyDomain) to throw a unique sentinel and asserting the sentinel surfaces
     * (status 499), i.e. control reached past the challenge check rather than failing on it (400).
     */
    @Test
    void verify_ifClientChallengeMatchesIssuedChallenge_passesChallengeCheck() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);

        when(commonVerifiers.verifyNullOrEmptyString(any())).thenReturn("keyId");
        when(commonVerifiers.verifyClientJSON(any())).thenReturn(mapper.createObjectNode());
        when(commonVerifiers.getChallenge(any())).thenReturn("clientChallenge");

        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setChallenge("clientChallenge"); // matches → guard must pass
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(authenticationPersistenceService.findByChallenge("clientChallenge")).thenReturn(List.of(entry));

        // Sentinel thrown by the step immediately after the challenge check.
        doThrow(new WebApplicationException(Response.status(499).entity("reached domain check").build()))
                .when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            // 499 (not 400) proves the matching challenge was accepted and control moved past the guard.
            assertEquals(499, ex.getResponse().getStatus());
        }
    }

    /**
     * CONF-10: rawId and id must reference the same credential; a mismatch is rejected.
     */
    @Test
    void verify_ifRawIdDoesNotMatchId_throws() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);
        when(assertionResult.getRawId()).thenReturn("rawIdValue");
        when(assertionResult.getId()).thenReturn("idValue");
        when(commonVerifiers.verifyNullOrEmptyString("rawIdValue")).thenReturn("rawIdValue");
        when(commonVerifiers.verifyNullOrEmptyString("idValue")).thenReturn("idValue");
        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("rawId mismatch").build()));

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    /**
     * CONF-10: in a username-scoped ceremony, the asserted credential must belong to that user
     * (be within the issued allowCredentials); a credential owned by a different user is rejected.
     */
    @Test
    void verify_ifAssertedCredentialOwnerDiffersFromCeremonyUser_throws() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);
        when(commonVerifiers.verifyNullOrEmptyString(any())).thenReturn("keyId");
        when(commonVerifiers.verifyClientJSON(any())).thenReturn(mapper.createObjectNode());
        when(commonVerifiers.getChallenge(any())).thenReturn("clientChallenge");

        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setChallenge("clientChallenge");
        authData.setUsername("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");
        when(authenticationPersistenceService.findByChallenge("clientChallenge")).thenReturn(List.of(entry));

        Fido2RegistrationData regData = new Fido2RegistrationData();
        regData.setUsername("bob"); // different owner than the ceremony user
        Fido2RegistrationEntry regEntry = mock(Fido2RegistrationEntry.class);
        when(regEntry.getRegistrationData()).thenReturn(regData);
        when(registrationPersistenceService.findByPublicKeyId(any(), any())).thenReturn(Optional.of(regEntry));

        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("not allowed").build()));

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    /**
     * CONF-10: when a userHandle is present, it must map to the credential owner; a mismatch is rejected.
     */
    @Test
    void verify_ifUserHandleDoesNotMatchCredentialOwner_throws() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);
        when(response.getUserHandle()).thenReturn("different-handle");
        when(commonVerifiers.verifyNullOrEmptyString(any())).thenReturn("keyId");
        when(commonVerifiers.verifyClientJSON(any())).thenReturn(mapper.createObjectNode());
        when(commonVerifiers.getChallenge(any())).thenReturn("clientChallenge");

        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setChallenge("clientChallenge");
        authData.setUsername("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");
        when(authenticationPersistenceService.findByChallenge("clientChallenge")).thenReturn(List.of(entry));

        Fido2RegistrationData regData = new Fido2RegistrationData();
        regData.setUsername("alice"); // same user → passes the allowCredentials check
        regData.setUserId("alice-handle"); // but the userHandle differs
        Fido2RegistrationEntry regEntry = mock(Fido2RegistrationEntry.class);
        when(regEntry.getRegistrationData()).thenReturn(regData);
        when(registrationPersistenceService.findByPublicKeyId(any(), any())).thenReturn(Optional.of(regEntry));

        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("userHandle mismatch").build()));

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    /**
     * A rejected assertion must give the ceremony a terminal status instead of leaving it at
     * pending, where it was indistinguishable from a ceremony still in flight and was then deleted
     * unlabelled. The reason and category must match what the metrics store records for the same
     * event, and the entry must be promoted off the short unfinished-request window.
     */
    @Test
    void verify_ifAssertionRejected_marksEntryFailedWithReasonAndHistoryRetention() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");

        stubCeremonyLookup(entry);
        stubHistoryExpiration(1296000);
        when(metricService.categorizeError("Challenge in clientData does not match")).thenReturn("INVALID_INPUT");

        // Rejected after the ceremony is known, which is the case that used to leave the row pending.
        doThrow(fido2Rejection("Challenge in clientData does not match"))
                .when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            assertThrows(WebApplicationException.class, () -> assertionService.verify(assertionResultWithChallenge()));
        }

        assertEquals(Fido2AuthenticationStatus.failed, authData.getStatus());
        assertEquals("Challenge in clientData does not match", authData.getErrorReason());
        assertEquals("INVALID_INPUT", authData.getErrorCategory());
        verify(entry).setExpiration(1296000);
        verify(authenticationPersistenceService).update(entry);
    }

    /**
     * A rejection raised through ErrorResponseFactory is a WebApplicationException whose getMessage()
     * is only the generic status line, identical for every such failure. The specific reason lives in
     * the {status, errorMessage} envelope, and both the entry and the metric must record that instead
     * — otherwise every rejection is stored as "HTTP 400 Bad Request" and categorized as OTHER.
     */
    @Test
    void verify_ifRejectedWithFido2Envelope_recordsSpecificReasonNotTheStatusLine() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");

        stubCeremonyLookup(entry);
        stubHistoryExpiration(1296000);

        WebApplicationException rejection = fido2Rejection("Couldn't find the key by PublicKeyId");
        // Guards the premise: the generic message is what naive extraction would have recorded.
        assertEquals("HTTP 400 Bad Request", rejection.getMessage());
        doThrow(rejection).when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            assertThrows(WebApplicationException.class, () -> assertionService.verify(assertionResultWithChallenge()));
        }

        assertEquals("Couldn't find the key by PublicKeyId", authData.getErrorReason());
        verify(metricService).recordPasskeyAuthenticationFailure(any(), any(), anyLong(),
                eq("Couldn't find the key by PublicKeyId"), any());
    }

    /**
     * The reason recorded on the entry must be the same string the failure metric carries, including
     * the fallback used when the exception has no message — otherwise the two sources still disagree.
     */
    @Test
    void verify_ifRejectionHasNoMessage_entryAndMetricShareTheSameReason() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");

        stubCeremonyLookup(entry);
        stubHistoryExpiration(1296000);
        when(metricService.categorizeError(any())).thenReturn("UNKNOWN_ERROR");

        doThrow(new RuntimeException((String) null)).when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            assertThrows(RuntimeException.class, () -> assertionService.verify(assertionResultWithChallenge()));
        }

        assertEquals("Unknown error", authData.getErrorReason());
        verify(metricService).recordPasskeyAuthenticationFailure(any(), any(), anyLong(), eq("Unknown error"), any());
    }

    /**
     * A failure raised before the ceremony is known must attribute the metric to the ceremony user
     * anyway. Previously username was only assigned after the registration lookup, so every earlier
     * failure was recorded against a null user.
     */
    @Test
    void verify_ifRejectedBeforeRegistrationLookup_attributesFailureToCeremonyUser() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");

        stubCeremonyLookup(entry);
        stubHistoryExpiration(1296000);

        doThrow(new WebApplicationException(Response.status(400).entity("boom").build()))
                .when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            assertThrows(WebApplicationException.class, () -> assertionService.verify(assertionResultWithChallenge()));
        }

        verify(metricService).recordPasskeyAuthenticationFailure(eq("alice"), any(), anyLong(), any(), any());
    }

    /**
     * Only a ceremony still in flight can be rejected. A late failure must not overwrite an outcome
     * an earlier request already recorded.
     */
    @Test
    void verify_ifCeremonyAlreadyTerminal_doesNotOverwriteOutcome() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        authData.setStatus(Fido2AuthenticationStatus.authenticated);
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");

        stubCeremonyLookup(entry);

        doThrow(new WebApplicationException(Response.status(400).entity("boom").build()))
                .when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            assertThrows(WebApplicationException.class, () -> assertionService.verify(assertionResultWithChallenge()));
        }

        assertEquals(Fido2AuthenticationStatus.authenticated, authData.getStatus());
        verify(authenticationPersistenceService, never()).update(any());
    }

    /**
     * When the challenge never resolves to a ceremony there is no row to mark, and the original
     * rejection must still surface unchanged.
     */
    @Test
    void verify_ifChallengeNeverResolved_surfacesOriginalRejectionAndWritesNothing() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);
        when(assertionResult.getRawId()).thenReturn("rawIdValue");
        when(assertionResult.getId()).thenReturn("idValue");
        when(commonVerifiers.verifyNullOrEmptyString("rawIdValue")).thenReturn("rawIdValue");
        when(commonVerifiers.verifyNullOrEmptyString("idValue")).thenReturn("idValue");
        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("rawId mismatch").build()));

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResult));
            assertEquals(400, ex.getResponse().getStatus());
        }

        verify(authenticationPersistenceService, never()).update(any());
    }

    /**
     * Bookkeeping must never mask the rejection being returned to the client: if persisting the
     * failed status itself fails, the original exception still surfaces.
     */
    @Test
    void verify_ifMarkingFailedCannotPersist_originalRejectionStillSurfaces() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = mock(Fido2AuthenticationEntry.class);
        when(entry.getAuthenticationData()).thenReturn(authData);
        when(entry.getRpId()).thenReturn("rp");

        stubCeremonyLookup(entry);
        stubHistoryExpiration(1296000);
        doThrow(new IllegalStateException("persistence down")).when(authenticationPersistenceService).update(any());

        doThrow(new WebApplicationException(Response.status(400).entity("boom").build()))
                .when(domainVerifier).verifyDomain(any(), any());

        try (MockedStatic<CommonUtilService> mockedStatic = mockStatic(CommonUtilService.class)) {
            mockedStatic.when(() -> CommonUtilService.toJsonNode(any())).thenReturn(mapper.createObjectNode());

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                    () -> assertionService.verify(assertionResultWithChallenge()));
            // The client still sees the rejection, not the persistence failure.
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    /**
     * A rejection shaped the way ErrorResponseFactory builds them: the reason is in the JSON entity,
     * never in the exception message.
     */
    private WebApplicationException fido2Rejection(String reason) {
        return new WebApplicationException(Response.status(400)
                .entity(Fido2ErrorResponse.failed(reason).toJson())
                .build());
    }

    private Fido2AuthenticationData pendingCeremony(String username) {
        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setChallenge("clientChallenge");
        authData.setUsername(username);
        authData.setStatus(Fido2AuthenticationStatus.pending);
        return authData;
    }

    private AssertionResult assertionResultWithChallenge() {
        AssertionResult assertionResult = mock(AssertionResult.class);
        io.jans.fido2.model.assertion.Response response = mock(io.jans.fido2.model.assertion.Response.class);
        when(assertionResult.getResponse()).thenReturn(response);
        return assertionResult;
    }

    private void stubCeremonyLookup(Fido2AuthenticationEntry entry) {
        when(commonVerifiers.verifyNullOrEmptyString(any())).thenReturn("keyId");
        when(commonVerifiers.verifyClientJSON(any())).thenReturn(mapper.createObjectNode());
        when(commonVerifiers.getChallenge(any())).thenReturn("clientChallenge");
        when(authenticationPersistenceService.findByChallenge("clientChallenge")).thenReturn(List.of(entry));
    }

    private void stubHistoryExpiration(int seconds) {
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setAuthenticationHistoryExpiration(seconds);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
    }

    /**
     * A missing payload used to reach an unguarded dereference and surface as a 500.
     * It must be rejected as a bad request instead.
     */
    @Test
    void options_ifAssertionOptionsIsNull_throwsInvalidRequest() {
        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("options mandatory").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> assertionService.options(null));
        assertEquals(400, ex.getResponse().getStatus());
    }
}
