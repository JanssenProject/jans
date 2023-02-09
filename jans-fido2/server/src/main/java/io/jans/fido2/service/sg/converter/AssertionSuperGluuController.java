/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.sg.converter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.fido2.exception.Fido2RpRuntimeException;
import io.jans.fido2.exception.Fido2RuntimeException;
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
        boolean oneStep = StringHelper.isEmpty(userName);

        boolean valid = userSessionIdService.isValidSessionId(sessionId, userName);
        if (!valid) {
            throw new Fido2RuntimeException(String.format("session_id '%s' is invalid", sessionId));
        }

        if (StringHelper.isEmpty(userName) && StringHelper.isEmpty(keyHandle)) {
            throw new Fido2RuntimeException("The request should contains either username or keyhandle");
        }

        ObjectNode params = dataMapperService.createObjectNode();
        // Add all required parameters from request to allow process U2F request 
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST, true);
        params.put(CommonVerifiers.SUPER_GLUU_APP_ID, appId);
        params.put(CommonVerifiers.SUPER_GLUU_KEY_HANDLE, keyHandle);
        params.put(CommonVerifiers.SUPER_GLUU_MODE, oneStep ? SuperGluuMode.ONE_STEP.getMode() : SuperGluuMode.TWO_STEP.getMode());

        params.put("username", userName);
        params.put("session_id", sessionId);

        log.debug("Prepared U2F_V2 assertions options request: {}", params.toString());

        ObjectNode result = assertionService.options(params);

        // Build start authentication response  
        ObjectNode superGluuResult = dataMapperService.createObjectNode();
        ArrayNode authenticateRequests = superGluuResult.putArray("authenticateRequests");

        String challenge = result.get("challenge").asText();
        String userVerification = result.get("userVerification").asText();

        if (result.has("allowCredentials")) {
	        	result.get("allowCredentials").forEach((f) -> {
	        		((ObjectNode) f).put("appId", appId);
	        		((ObjectNode) f).put("userVerification", userVerification);
	        		((ObjectNode) f).put("challenge", challenge);
	        		((ObjectNode) f).put("keyHandle", f.get("id").asText());
	        		((ObjectNode) f).remove("id");
	        		((ObjectNode) f).put("version", "U2F_V2");

	        		authenticateRequests.add(f);
	        	});
        }

        return superGluuResult;
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
     *             IurInJvPKUoxpKzrleUMWgu9w3v_NUBu7BiGAclgkH_Zg88_T5y6Rh78imTxTh0djWFYG4jxOixw"}
     *  - response:
     *             {"status":"success","challenge":"5QoRtudmej5trcrMRgFBoI5rZ6pzIZiYP3u3bXCvvAE"}
     *            
     */
    public JsonNode finishAuthentication(String userName, String authenticateResponseString) {
        AuthenticateResponse authenticateResponse;
        try {
        	authenticateResponse = dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse options assertion request", ex);
        }

        if (!ArrayUtils.contains(RawAuthenticationService.SUPPORTED_AUTHENTICATE_TYPES, authenticateResponse.getClientData().getTyp())) {
            throw new Fido2RuntimeException("Invalid options attestation request type");
        }

        boolean oneStep = StringHelper.isEmpty(userName);

        ObjectNode params = dataMapperService.createObjectNode();
        // Add all required parameters from request to allow process U2F request 
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST, true);
        params.put(CommonVerifiers.SUPER_GLUU_MODE, oneStep ? SuperGluuMode.ONE_STEP.getMode() : SuperGluuMode.TWO_STEP.getMode());

        // Manadatory parameter
        params.put("type", "public-key");
        
        params.put("id", authenticateResponse.getKeyHandle());

        params.put("rawId", authenticateResponseString);

        // Convert clientData node to new format
        ObjectNode clientData = dataMapperService.createObjectNode();
        clientData.put("type",  authenticateResponse.getClientData().getTyp());
        clientData.put("challenge", authenticateResponse.getClientData().getChallenge());
        clientData.put("origin", authenticateResponse.getClientData().getOrigin());

        // Store cancel type
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST_CANCEL, StringHelper.equals(RawAuthenticationService.AUTHENTICATE_CANCEL_TYPE, authenticateResponse.getClientData().getTyp()));

        // Add response node
        ObjectNode response = dataMapperService.createObjectNode();
		params.set("response", response);

		// We have to quote URL to conform bug in Super Gluu
		response.put("clientDataJSON", base64Service.urlEncodeToString(clientData.toString().replaceAll("/", "\\\\/").getBytes(StandardCharsets.UTF_8)));
		
		// Prepare attestationObject
		RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData());

		response.put("signature", base64Service.urlEncodeToString(rawAuthenticateResponse.getSignature()));

		ObjectNode attestationObject = dataMapperService.createObjectNode();
        
        try {
	        byte[] authData = generateAuthData(authenticateResponse.getClientData(), rawAuthenticateResponse);
	        response.put("authenticatorData", base64Service.urlEncodeToString(authData));
	        response.put("attestationObject", base64Service.urlEncodeToString(dataMapperService.cborWriteAsBytes(attestationObject)));
		} catch (IOException e) {
            throw new Fido2RuntimeException("Failed to prepare attestationObject");
		}

        log.debug("Prepared U2F_V2 assertion verify request: {}", params.toString());

        ObjectNode result = assertionService.verify(params);

        result.put("status", "success");
        result.put("challenge", authenticateResponse.getClientData().getChallenge());

        return result;
    }

    private byte[] generateAuthData(ClientData clientData, RawAuthenticateResponse rawAuthenticateResponse) throws IOException {
    	byte[] rpIdHash = digestService.hashSha256(clientData.getOrigin());
    	byte[] flags = new byte[] { AuthenticatorDataParser.FLAG_USER_PRESENT };
    	byte[] counter = ByteBuffer.allocate(4).putInt((int) rawAuthenticateResponse.getCounter()).array();
        
		byte[] authData = ByteBuffer
				.allocate(rpIdHash.length + flags.length + counter.length)
				.put(rpIdHash).put(flags).put(counter).array();
		
		return authData;
    }


}
