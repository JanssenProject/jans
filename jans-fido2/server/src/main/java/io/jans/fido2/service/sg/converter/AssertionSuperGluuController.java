/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.sg.converter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.jans.fido2.model.assertion.*;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.util.CommonUtilService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.DigestService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.sg.RawAuthenticationService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.sg.SuperGluuMode;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Converters Super Gluu authentication request to U2F V2 request
 *
 * @author Yuriy Movchan
 * @version Jan 26, 2023
 */
@ApplicationScoped
public class AssertionSuperGluuController {

    @Inject
    private Logger log;

    @Inject
    private AssertionService assertionService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private RawAuthenticationService rawAuthenticationService;

    @Inject
    private DigestService digestService;

    @Inject
    private UserSessionIdService userSessionIdService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /* Example for one_step:
     *  - request:
     *             username: null
     *             keyhandle: r4AIBCT_CEi8SWThJ-T5gsxjfZMqzqMdqCeDuK_xTvz_kr5FNNs2j6Tb2dvoXgculthxTzXF5-FI1KWsA_dRLA
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: 2994e597-3dc9-4d96-ae7e-84cfcf049db6
     *  - response:
     *             {"authenticateRequests":[{"challenge":"EELAH05XTUfPHrvpqVYhXB8pEmOMaRWY9mBurdhicBU",
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm",
     *             "keyHandle":"r4AIBCT_CEi8SWThJ-T5gsxjfZMqzqMdqCeDuK_xTvz_kr5FNNs2j6Tb2dvoXgculthxTzXF5-FI1KWsA_dRLA","version":"U2F_V2"}]}
     *
     * Example for two_step:
     *  - request:
     *             username: test1
     *             keyhandle: null
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: 850ff665-02b6-435b-baf8-b018b13043c3
     *  - response:
     *             {"authenticateRequests":[{"challenge":"5QoRtudmej5trcrMRgFBoI5rZ6pzIZiYP3u3bXCvvAE",
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm",
     *             "keyHandle":"YJvWD9n40eIurInJvPKUoxpKzrleUMWgu9w3v_NUBu7BiGAclgkH_Zg88_T5y6Rh78imTxTh0djWFYG4jxOixw","version":"U2F_V2"}]}
     */
    public JsonNode startAuthentication(String userName, String keyHandle, String appId, String sessionId) {
        AssertionOptions assertionOptions = buildFido2AssertionStartResponse(userName, keyHandle, appId, sessionId);
        AssertionOptionsResponse result = assertionService.options(assertionOptions);

        // Build start authentication response  
        ObjectNode superGluuResult = dataMapperService.createObjectNode();
        ArrayNode authenticateRequests = superGluuResult.putArray("authenticateRequests");

        String challenge = result.getChallenge();
        String userVerification = result.getUserVerification();

        if (result.getAllowCredentials() != null) {
            result.getAllowCredentials().forEach((f) -> {
                ObjectNode item = dataMapperService.createObjectNode();
                item.put("appId", appId);
                item.put("userVerification", userVerification);
                item.put("challenge", challenge);
                item.put("keyHandle", f.getId());
                item.put("version", "U2F_V2");

                authenticateRequests.add(item);
            });
        }

        return superGluuResult;
    }

    public AssertionOptions buildFido2AssertionStartResponse(String userName, String keyHandle, String appId,
                                                       String sessionId) {
        boolean oneStep = StringHelper.isEmpty(userName);

        boolean valid = userSessionIdService.isValidSessionId(sessionId, userName);
        if (!valid) {
            String reasonError = String.format("session_id '%s' is invalid", sessionId);
            throw errorResponseFactory.badRequestException(AssertionErrorResponseType.INVALID_SESSION_ID, reasonError);
        }

        if (StringHelper.isEmpty(userName) && StringHelper.isEmpty(keyHandle)) {
            String reasonError = "invalid username or keyHandle";
            throw errorResponseFactory.badRequestException(AssertionErrorResponseType.INVALID_USERNAME_OR_KEY_HANDLE, reasonError);
        }
        AssertionOptions assertionOptions = new AssertionOptions();
        // Add all required parameters from request to allow process U2F request
        assertionOptions.setSuperGluuRequest(true);
        assertionOptions.setSuperGluuAppId(appId);
        assertionOptions.setDocumentDomain(appId);
        assertionOptions.setSuperGluuKeyHandle(keyHandle);
        assertionOptions.setSuperGluuRequestMode(oneStep ? SuperGluuMode.ONE_STEP.getMode() : SuperGluuMode.TWO_STEP.getMode());

        assertionOptions.setUsername(userName);
        assertionOptions.setSessionId(sessionId);

        log.debug("Prepared U2F_V2 assertions options request: {}", assertionOptions.toString());
        return assertionOptions;
    }

    /* Example for one_step:
     *  - request:
     *             username: null
     *             tokenResponse: {"signatureData":"AQAAAAEwRQIhANrCm98JCTz6cqSZ_vwGHdF9uqe3b4z1nCrNIPCObwc-AiAblGdWyky
     *             LeaTJPzLtbWHMoN9MsKUlgmbfSRsINJEVeA","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwiY
     *             2hhbGxlbmdlIjoiRUVMQUgwNVhUVWZQSHJ2cHFWWWhYQjhwRW1PTWFSV1k5bUJ1cmRoaWNCVSIsIm9yaWdpbiI6Imh0dHBzOlwvX
     *             C95dXJlbS1lbWVyZ2luZy1waWcuZ2x1dS5pbmZvXC9pZGVudGl0eVwvYXV0aGNvZGUuaHRtIn0","keyHandle":"r4AIBCT_CEi
     *             8SWThJ-T5gsxjfZMqzqMdqCeDuK_xTvz_kr5FNNs2j6Tb2dvoXgculthxTzXF5-FI1KWsA_dRLA"}
     *  - response:
     *             {"status":"success","challenge":"EELAH05XTUfPHrvpqVYhXB8pEmOMaRWY9mBurdhicBU"}
     *
     * Example for two_step:
     *  - request:
     *             username: test1
     *             tokenResponse: {"signatureData":"AQAAAAEwRgIhAN4auE9-U2YDhi8ByxIIv3G2hvDeFjEGU_x5SvfcIQyUAiEA4I_xMin
     *             mYAmH5qk5KMaYATFAryIpoVwARGvEFQTWE2Q","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwi
     *             Y2hhbGxlbmdlIjoiNVFvUnR1ZG1lajV0cmNyTVJnRkJvSTVyWjZweklaaVlQM3UzYlhDdnZBRSIsIm9yaWdpbiI6Imh0dHBzOlwv
     *             XC95dXJlbS1lbWVyZ2luZy1waWcuZ2x1dS5pbmZvXC9pZGVudGl0eVwvYXV0aGNvZGUuaHRtIn0","keyHandle":"YJvWD9n40e
     *             IurInJvPKUoxpKzrleUMWgu9w3v_NUBu7BiGAclgkH_Zg88_T5y6Rh78imTxTh0djWFYG4jxOixw","deviceData":"eyJuYW1l
     *             IjoiU00tRzk5MUIiLCJvc19uYW1lIjoidGlyYW1pc3UiLCJvc192ZXJzaW9uIjoiMTMiLCJwbGF0Zm9ybSI6ImFuZHJvaWQiLCJw
     *             dXNoX3Rva2VuIjoicHVzaF90b2tlbiIsInR5cGUiOiJub3JtYWwiLCJ1dWlkIjoidXVpZCJ9"}
     *  - response:
     *             {"status":"success","challenge":"5QoRtudmej5trcrMRgFBoI5rZ6pzIZiYP3u3bXCvvAE"}
     *
     */
    public JsonNode finishAuthentication(String userName, String authenticateResponseString) {
        AuthenticateResponse authenticateResponse = parseAuthenticateResponse(authenticateResponseString);

        AssertionResult params = buildFido2AuthenticationVerifyResponse(userName, authenticateResponseString, authenticateResponse);

        AssertionResultResponse assertionResultResponse = assertionService.verify(params);
        ObjectNode result = (ObjectNode)CommonUtilService.toJsonNode(assertionResultResponse);

        result.put("status", "success");
        result.put("challenge", authenticateResponse.getClientData().getChallenge());

        return result;
    }

    public AssertionResult buildFido2AuthenticationVerifyResponse(String userName, String authenticateResponseString, AuthenticateResponse authenticateResponse) {
        if (!ArrayUtils.contains(RawAuthenticationService.SUPPORTED_AUTHENTICATE_TYPES, authenticateResponse.getClientData().getTyp())) {
            throw errorResponseFactory.badRequestException(AssertionErrorResponseType.UNSUPPORTED_AUTHENTICATION_TYPE, "Invalid options attestation request type");
        }

        boolean oneStep = StringHelper.isEmpty(userName);

        AssertionResult assertionResult = new AssertionResult();
        // Add all required parameters from request to allow process U2F request 
        assertionResult.setSuperGluuRequest(true);
        assertionResult.setSuperGluuRequestMode(oneStep ? SuperGluuMode.ONE_STEP.getMode() : SuperGluuMode.TWO_STEP.getMode());
        assertionResult.setId(authenticateResponse.getKeyHandle());
        assertionResult.setRawId(authenticateResponseString);

        // Convert clientData node to new format
        ObjectNode clientData = dataMapperService.createObjectNode();
        clientData.put("type", authenticateResponse.getClientData().getTyp());
        clientData.put("challenge", authenticateResponse.getClientData().getChallenge());
        clientData.put("origin", authenticateResponse.getClientData().getOrigin());

        // Store cancel type
        assertionResult.setSuperGluuRequestCancel(StringHelper.equals(RawAuthenticationService.AUTHENTICATE_CANCEL_TYPE, authenticateResponse.getClientData().getTyp()));
        // Add response node
        Response response = new Response();
        response.setDeviceData(authenticateResponse.getDeviceData());
        // We have to quote URL to conform bug in Super Gluu
        response.setClientDataJSON(base64Service.urlEncodeToString(clientData.toString().replaceAll("/", "\\\\/").getBytes(StandardCharsets.UTF_8)));
        // Prepare attestationObject
        RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData());
        response.setSignature(base64Service.urlEncodeToString(rawAuthenticateResponse.getSignature()));

        ObjectNode attestationObject = dataMapperService.createObjectNode();

        try {
            byte[] authData = generateAuthData(authenticateResponse.getClientData(), rawAuthenticateResponse);
            response.setAuthenticatorData(base64Service.urlEncodeToString(authData));
            response.setAttestationObject(base64Service.urlEncodeToString(dataMapperService.cborWriteAsBytes(attestationObject)));
        } catch (IOException e) {
            throw errorResponseFactory.invalidRequest("Failed to prepare attestationObject: " + e.getMessage(), e);
        }
        assertionResult.setResponse(response);
        log.debug("Prepared U2F_V2 assertion verify request: {}", assertionResult.toString());
        return assertionResult;
    }

    public AuthenticateResponse parseAuthenticateResponse(String authenticateResponseString) {
        AuthenticateResponse authenticateResponse;
        try {
            authenticateResponse = dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class);
        } catch (Exception ex) {
            throw errorResponseFactory.invalidRequest(ex.getMessage());
        }
        return authenticateResponse;
    }

    private byte[] generateAuthData(ClientData clientData, RawAuthenticateResponse rawAuthenticateResponse) {
        byte[] rpIdHash = digestService.hashSha256(clientData.getOrigin());
        byte[] flags = new byte[]{AuthenticatorDataParser.FLAG_USER_PRESENT};
        byte[] counter = ByteBuffer.allocate(4).putInt((int) rawAuthenticateResponse.getCounter()).array();

        byte[] authData = ByteBuffer
                .allocate(rpIdHash.length + flags.length + counter.length)
                .put(rpIdHash).put(flags).put(counter).array();

        return authData;
    }
}
