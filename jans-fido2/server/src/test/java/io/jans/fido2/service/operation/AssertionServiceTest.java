package io.jans.fido2.service.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.external.ExternalFido2Service;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
    private ErrorResponseFactory errorResponseFactory;
    @Mock
    private DomainVerifier domainVerifier;
    @Mock
    private MetricService metricService;
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
}
