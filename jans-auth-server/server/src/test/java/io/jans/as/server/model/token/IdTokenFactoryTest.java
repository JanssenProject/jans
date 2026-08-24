/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalAuthorizationChallengeService;
import org.json.JSONException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class IdTokenFactoryTest {

    @InjectMocks
    private IdTokenFactory idTokenFactory;

    @Mock
    private Logger log;

    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

    @Mock
    private ExternalAuthorizationChallengeService externalAuthorizationChallengeService;

    @Test
    public void parseAgamaAmr_whenAmrKeyMissing_shouldReturnEmptyList() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"userId\":\"admin\"}");

        assertTrue(amr.isEmpty());
    }

    @Test
    public void parseAgamaAmr_whenAmrIsBlankString_shouldReturnEmptyList() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"amr\":\"\"}");

        assertTrue(amr.isEmpty());
    }

    @Test
    public void parseAgamaAmr_whenAmrIsString_shouldReturnSingletonList() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"userId\":\"admin\",\"amr\":\"some-amr\"}");

        assertEquals(amr, List.of("some-amr"));
    }

    @Test
    public void parseAgamaAmr_whenAmrIsArray_shouldReturnAllNonBlankValues() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"amr\":[\"otp\", \"\", \"sms\"]}");

        assertEquals(amr, List.of("otp", "sms"));
    }

    @Test
    public void parseAgamaAmr_whenAmrIsNonStringScalar_shouldReturnEmptyList() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"amr\":123}");

        assertTrue(amr.isEmpty());
    }

    @Test
    public void parseAgamaAmr_whenAmrIsBoolean_shouldReturnEmptyList() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"amr\":true}");

        assertTrue(amr.isEmpty());
    }

    @Test
    public void parseAgamaAmr_whenAmrArrayHasNonStringElements_shouldSkipThem() throws JSONException {
        List<String> amr = IdTokenFactory.parseAgamaAmr("{\"amr\":[\"otp\", 123, true, \"sms\"]}");

        assertEquals(amr, List.of("otp", "sms"));
    }

    @Test(expectedExceptions = JSONException.class)
    public void parseAgamaAmr_whenJsonIsMalformed_shouldThrow() throws JSONException {
        IdTokenFactory.parseAgamaAmr("not-a-json-object");
    }

    @Test
    public void addAgamaAmr_whenSessionIsNull_shouldNotChangeAmrListAndLogTrace() {
        List<String> amrList = newAmrList("10");

        idTokenFactory.addAgamaAmr(amrList, null);

        assertEquals(amrList, List.of("10"));
        verify(log).trace("Unable to propagate amr from agama - session is not available");
    }

    @Test
    public void addAgamaAmr_whenAgamaDataAttributeIsAbsent_shouldNotChangeAmrListAndLogTrace() {
        List<String> amrList = newAmrList("10");
        SessionId session = sessionWithAttributes(new HashMap<>());

        idTokenFactory.addAgamaAmr(amrList, session);

        assertEquals(amrList, List.of("10"));
        verify(log).trace(
                eq("Unable to propagate amr from agama - '{}' session attribute is not set, session id: {}"),
                eq(IdTokenFactory.AGAMA_DATA_SESSION_ATTR_KEY), eq(session.getId()));
    }

    @Test
    public void addAgamaAmr_whenAgamaDataHasNoAmrKey_shouldNotChangeAmrListAndLogTrace() {
        List<String> amrList = newAmrList("10");
        SessionId session = sessionWithAgamaData("{\"userId\":\"admin\"}");

        idTokenFactory.addAgamaAmr(amrList, session);

        assertEquals(amrList, List.of("10"));
        verify(log).trace(
                eq("Unable to propagate amr from agama - no non-blank '{}' entry found in '{}' session attribute, session id: {}"),
                eq(IdTokenFactory.AGAMA_AMR_KEY), eq(IdTokenFactory.AGAMA_DATA_SESSION_ATTR_KEY), eq(session.getId()));
    }

    @Test
    public void addAgamaAmr_whenAgamaDataHasAmrString_shouldAppendItAndLogTrace() {
        List<String> amrList = newAmrList("10");
        SessionId session = sessionWithAgamaData("{\"userId\":\"admin\",\"amr\":\"some-amr\"}");

        idTokenFactory.addAgamaAmr(amrList, session);

        assertEquals(amrList, List.of("10", "some-amr"));
        verify(log).trace(
                eq("Propagated amr value '{}' from agama session attribute '{}' into id_token amr claim, session id: {}"),
                eq("some-amr"), eq(IdTokenFactory.AGAMA_DATA_SESSION_ATTR_KEY), eq(session.getId()));
    }

    @Test
    public void addAgamaAmr_whenAgamaDataHasAmrArray_shouldAppendAllValues() {
        List<String> amrList = newAmrList("10");
        SessionId session = sessionWithAgamaData("{\"amr\":[\"otp\",\"sms\"]}");

        idTokenFactory.addAgamaAmr(amrList, session);

        assertEquals(amrList, List.of("10", "otp", "sms"));
    }

    @Test
    public void addAgamaAmr_whenAmrValueAlreadyPresent_shouldNotDuplicateAndLogTrace() {
        List<String> amrList = newAmrList("some-amr");
        SessionId session = sessionWithAgamaData("{\"amr\":\"some-amr\"}");

        idTokenFactory.addAgamaAmr(amrList, session);

        assertEquals(amrList, List.of("some-amr"));
        verify(log).trace(
                eq("Amr value '{}' from agama is already present in id_token amr claim, session id: {}"),
                eq("some-amr"), eq(session.getId()));
    }

    @Test
    public void addAgamaAmr_whenAgamaDataIsMalformedJson_shouldLogErrorAndNotThrow() {
        List<String> amrList = newAmrList("10");
        SessionId session = sessionWithAgamaData("not-a-json-object");

        idTokenFactory.addAgamaAmr(amrList, session);

        assertEquals(amrList, List.of("10"));
    }

    @Test
    public void setAmrClaim_whenAgamaDataHasAmr_shouldMergeIntoAmrClaim() {
        when(externalAuthenticationService.getCustomScriptConfigurationByName("agama_co.test")).thenReturn(null);
        when(externalAuthorizationChallengeService.getAuthenticationMethodClaims(any(ExecutionContext.class)))
                .thenReturn(new HashMap<>());

        Jwt jwt = new Jwt();
        Client client = new Client();
        client.setClientId("client1");
        SessionId session = sessionWithAgamaData("{\"userId\":\"admin\",\"amr\":\"some-amr\"}");

        idTokenFactory.setAmrClaim(jwt, "agama_co.test", client, session);

        assertEquals(jwt.getClaims().getClaim(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES), List.of("some-amr"));
    }

    private static List<String> newAmrList(String... values) {
        List<String> amrList = new ArrayList<>();
        for (String value : values) {
            amrList.add(value);
        }
        return amrList;
    }

    private static SessionId sessionWithAgamaData(String agamaDataAsJson) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(IdTokenFactory.AGAMA_DATA_SESSION_ATTR_KEY, agamaDataAsJson);
        return sessionWithAttributes(attributes);
    }

    private static SessionId sessionWithAttributes(Map<String, String> attributes) {
        SessionId session = new SessionId();
        session.setId("session-1");
        session.setSessionAttributes(attributes);
        return session;
    }
}
