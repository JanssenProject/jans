/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.sg.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.as.model.fido.u2f.message.RawRegisterResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.as.model.fido.u2f.protocol.RegisterResponse;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.model.attestation.*;
import io.jans.fido2.model.common.PublicKeyCredentialType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.*;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.sg.RawRegistrationService;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.sg.SuperGluuMode;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;

/**
 * Converters Super Gluu registration request to U2F V2 request
 * @author Yuriy Movchan
 * @version Jan 26, 2023
 */
@ApplicationScoped
public class AttestationSuperGluuController {

    @Inject
    private Logger log;

    @Inject
    private AttestationService attestationService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private RawRegistrationService rawRegistrationService;

	@Inject
	private CoseService coseService;

	@Inject
	private DigestService digestService;

	@Inject
	private UserSessionIdService userSessionIdService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /* Example for one_step:
     *  - request:
     *             username: null
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: a5183b05-dbe0-4173-a794-2334bb864708
     *             enrollment_code: null
     *  - response:
     *             {"authenticateRequests":[],"registerRequests":[{"challenge":"GU4usvpYfvQ_RCMSqm819gTZMa0qCeLr1Xg2KbvW2To"
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm","version":"U2F_V2"}]}
     *
     * Example for two_step:
     *  - request:
     *             username: test1
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: 46c30db8-0339-4459-8ee7-e4960ad75986
     *             enrollment_code: null
     *  - response:
     *             {"authenticateRequests":[],"registerRequests":[{"challenge":"raPfmqZOHlHF4gXbprd29uwX-bs3Ff5v03quxBD4FkM",
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm","version":"U2F_V2"}]}
     */
    public JsonNode startRegistration(String userName, String appId, String sessionId, String enrollmentCode) {
        AttestationOptions attestationOptions = buildFido2AttestationStartResponse(userName, appId, sessionId);

        PublicKeyCredentialCreationOptions fromPublicKeyCredentialCreationOptions = attestationService.options(attestationOptions);

        ObjectNode result = (ObjectNode)CommonUtilService.toJsonNode(fromPublicKeyCredentialCreationOptions);
        // Build start registration response
        ObjectNode superGluuResult = dataMapperService.createObjectNode();
        ArrayNode registerRequests = superGluuResult.putArray("registerRequests");

        result.put("appId", appId);
        registerRequests.add(result);

        result.put("version", "U2F_V2");

        return superGluuResult;
    }

    public AttestationOptions buildFido2AttestationStartResponse(String userName, String appId, String sessionId) {
		boolean oneStep = StringHelper.isEmpty(userName);

        boolean valid = userSessionIdService.isValidSessionId(sessionId, userName);
        if (!valid) {
            String reasonError = String.format("session_id '%s' is invalid", sessionId);
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.INVALID_SESSION_ID, reasonError);
        }
        AttestationOptions attestationOptions = new AttestationOptions();
        // Add all required parameters from request to allow process U2F request
        attestationOptions.setSuper_gluu_request(true);
        attestationOptions.setSuper_gluu_request_mode(oneStep ? SuperGluuMode.ONE_STEP.getMode() : SuperGluuMode.TWO_STEP.getMode());
        attestationOptions.setSuper_gluu_app_id(appId);

        String useUserName = userName;
        if (oneStep) {
        	useUserName = attestationService.generateUserId();
        }
        attestationOptions.setUsername(useUserName);
        attestationOptions.setDisplayName(useUserName);
        attestationOptions.setSession_id(sessionId);
        attestationOptions.setAttestation(AttestationConveyancePreference.direct);

        log.debug("Prepared U2F_V2 attestation options request: {}", CommonUtilService.toJsonNode(attestationOptions).toString());
		return attestationOptions;
	}

    /* Example for one_step:
     *  - request:
     *             username: null
     *             tokenResponse: {"registrationData":"BQQTkZFzsbTmuUoS_DS_jqpWRbZHp_J0YV8q4Xb4XTPYbIuvu-TRNubp8U-CKZuB
     *             5tDT-l6R3sQvNc6wXjGCmL-OQK-ACAQk_whIvElk4Sfk-YLMY32TKs6jHagng7iv8U78_5K-RTTbNo-k29nb6F4HLpbYcU81xefh
     *             SNSlrAP3USwwggImMIIBzKADAgECAoGBAPMsD5b5G58AphKuKWl4Yz27sbE_rXFy7nPRqtJ_r4E5DSZbFvfyuos-Db0095ubB0Jo
     *             yM8ccmSO_eZQ6IekOLPKCR7yC5kes-f7MaxyaphmmD4dEvmuKjF-fRsQP5tQG7zerToto8eIz0XjPaupiZxQXtSHGHHTuPhri2nf
     *             oZlrMAoGCCqGSM49BAMCMFwxIDAeBgNVBAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQH
     *             EwZBdXN0aW4xCzAJBgNVBAgTAlRYMQswCQYDVQQGEwJVUzAeFw0xNjAzMDExODU5NDZaFw0xOTAzMDExODU5NDZaMFwxIDAeBgNV
     *             BAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQHEwZBdXN0aW4xCzAJBgNVBAgTAlRYMQsw
     *             CQYDVQQGEwJVUzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABICUKnzCE5PJ7tihiKkYu6E5Uy_sZ-RSqs_MnUJt0tB8G8GSg9nK
     *             o6P2424iV9lXX9Pil8qw4ofZ-fAXXepbp4MwCgYIKoZIzj0EAwIDSAAwRQIgUWwawAB2udURWQziDXVjSOi_QcuXiRxylqj5thFw
     *             FhYCIQCGY-CTZFi7JdkhZ05nDpbSYJBTOo1Etckh7k0qcvnO0TBFAiEA1v1jKTwGn5LRRGSab1kNdgEqD6qL08bougoJUNY1A5MC
     *             IGvtBFSNzhGvhQmdYYj5-XOd5P4ucVk6TmkV1Xu73Dvj","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZmluaXNoRW5y
     *             b2xsbWVudCIsImNoYWxsZW5nZSI6IkdVNHVzdnBZZnZRX1JDTVNxbTgxOWdUWk1hMHFDZUxyMVhnMktidlcyVG8iLCJvcmlnaW4i
     *             OiJodHRwczpcL1wveXVyZW0tZW1lcmdpbmctcGlnLmdsdXUuaW5mbyJ9","deviceData":"eyJuYW1lIjoiU00tRzk5MUIiLCJv
     *             c19uYW1lIjoidGlyYW1pc3UiLCJvc192ZXJzaW9uIjoiMTMiLCJwbGF0Zm9ybSI6ImFuZHJvaWQiLCJwdXNoX3Rva2VuIjoicHVz
     *             aF90b2tlbiIsInR5cGUiOiJub3JtYWwiLCJ1dWlkIjoidXVpZCJ9"}
     *  - response:
     *             {"status":"success","challenge":"GU4usvpYfvQ_RCMSqm819gTZMa0qCeLr1Xg2KbvW2To"}
     *
    * Example for two_step:
     *  - request:
     *             username: test1
     *             tokenResponse: {"registrationData":"BQToXkGAjgXxC4g1NiA-IuRAu40NFBlXXNSu4TEZqGK5TBqwU07ANn4LJ9Hp3aV5
     *             PIvCDVsQ2tZJf1xD6LosZNDuQGCb1g_Z-NHiLqyJybzylKMaSs65XlDFoLvcN7_zVAbuwYhgHJYJB_2YPPP0-cukYe_Ipk8U4dHY
     *             1hWBuI8ToscwggImMIIBzKADAgECAoGBAPMsD5b5G58AphKuKWl4Yz27sbE_rXFy7nPRqtJ_r4E5DSZbFvfyuos-Db0095ubB0Jo
     *             yM8ccmSO_eZQ6IekOLPKCR7yC5kes-f7MaxyaphmmD4dEvmuKjF-fRsQP5tQG7zerToto8eIz0XjPaupiZxQXtSHGHHTuPhri2nf
     *             oZlrMAoGCCqGSM49BAMCMFwxIDAeBgNVBAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQH
     *             EwZBdXN0aW4xCzAJBgNVBAgTAlRYMQswCQYDVQQGEwJVUzAeFw0xNjAzMDExODU5NDZaFw0xOTAzMDExODU5NDZaMFwxIDAeBgNV
     *             BAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQHEwZBdXN0aW4xCzAJBgNVBAgTAlRYMQsw
     *             CQYDVQQGEwJVUzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABICUKnzCE5PJ7tihiKkYu6E5Uy_sZ-RSqs_MnUJt0tB8G8GSg9nK
     *             o6P2424iV9lXX9Pil8qw4ofZ-fAXXepbp4MwCgYIKoZIzj0EAwIDSAAwRQIgUWwawAB2udURWQziDXVjSOi_QcuXiRxylqj5thFw
     *             FhYCIQCGY-CTZFi7JdkhZ05nDpbSYJBTOo1Etckh7k0qcvnO0TBFAiArOYmHd22USw7flCmGXLOXVOrhDi-pkX7Qx_c8oz5hJQIh
     *             AOslo3LfymoFWT6mxUZjBlxKgxioozd0KmzUwobRcKdW","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZmluaXNoRW5y
     *             b2xsbWVudCIsImNoYWxsZW5nZSI6InJhUGZtcVpPSGxIRjRnWGJwcmQyOXV3WC1iczNGZjV2MDNxdXhCRDRGa00iLCJvcmlnaW4i
     *             OiJodHRwczpcL1wveXVyZW0tZW1lcmdpbmctcGlnLmdsdXUuaW5mbyJ9","deviceData":"eyJuYW1lIjoiU00tRzk5MUIiLCJv
     *             c19uYW1lIjoidGlyYW1pc3UiLCJvc192ZXJzaW9uIjoiMTMiLCJwbGF0Zm9ybSI6ImFuZHJvaWQiLCJwdXNoX3Rva2VuIjoicHVz
     *             aF90b2tlbiIsInR5cGUiOiJub3JtYWwiLCJ1dWlkIjoidXVpZCJ9"}
     *  - response:
     *             {"status":"success","challenge":"raPfmqZOHlHF4gXbprd29uwX-bs3Ff5v03quxBD4FkM"}
     *
     */
    public JsonNode finishRegistration(String userName, String registerResponseString) {
        RegisterResponse registerResponse = parseRegisterResponse(registerResponseString);

        //ObjectNode params = buildFido2AttestationVerifyResponse(userName, registerResponse);
        AttestationResult attestationResult = buildFido2AttestationVerifyResponse(userName, registerResponse);
        AttestationResultResponse attestationResultResponse = attestationService.verify(attestationResult);
        ObjectNode result = (ObjectNode)CommonUtilService.toJsonNode(attestationResultResponse);

        result.put("status", "success");
        result.put("challenge", registerResponse.getClientData().getChallenge());

        return result;
    }

    public RegisterResponse parseRegisterResponse(String registerResponseString) {
		RegisterResponse registerResponse;
        try {
            registerResponse = dataMapperService.readValue(registerResponseString, RegisterResponse.class);
        } catch (IOException ex) {
            throw errorResponseFactory.invalidRequest("Failed to parse options attestation request", ex);
        }

        return registerResponse;
	}

	public AttestationResult buildFido2AttestationVerifyResponse(String userName, RegisterResponse registerResponse) {
		if (!ArrayUtils.contains(RawRegistrationService.SUPPORTED_REGISTER_TYPES, registerResponse.getClientData().getTyp())) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.UNSUPPORTED_REGISTER_TYPE, "Invalid options attestation request type");
        }
        AttestationResult attestationResult = new AttestationResult();
        attestationResult.setSuper_gluu_request(true);

        // Add all required parameters from request to allow process U2F request
        boolean oneStep = StringHelper.isEmpty(userName);
        attestationResult.setSuper_gluu_request_mode(oneStep ? SuperGluuMode.ONE_STEP.getMode() : SuperGluuMode.TWO_STEP.getMode());

        // Manadatory parameter
        attestationResult.setType(PublicKeyCredentialType.PUBLIC_KEY.getKeyName());

        // Add response node
        Response response = new Response();
        attestationResult.setResponse(response);
        attestationResult.getResponse().setDeviceData(registerResponse.getDeviceData());

        // Convert clientData node to new format
        ObjectNode clientData = dataMapperService.createObjectNode();
        clientData.put("challenge", registerResponse.getClientData().getChallenge());
        clientData.put("origin", registerResponse.getClientData().getOrigin());
        clientData.put("type", registerResponse.getClientData().getTyp());
        attestationResult.getResponse().setClientDataJSON(base64Service.urlEncodeToString(clientData.toString().getBytes(StandardCharsets.UTF_8)));
		// Store cancel type
        attestationResult.setSuper_gluu_request_cancel(StringHelper.equals(RawRegistrationService.REGISTER_CANCEL_TYPE, registerResponse.getClientData().getTyp()));
		// Prepare attestationObject
        RawRegisterResponse rawRegisterResponse = rawRegistrationService.parseRawRegisterResponse(registerResponse.getRegistrationData());

        attestationResult.setId(base64Service.urlEncodeToString(rawRegisterResponse.getKeyHandle()));
        ObjectNode attestationObject = dataMapperService.createObjectNode();
        ObjectNode attStmt = dataMapperService.createObjectNode();

        try {
			ArrayNode x5certs = attStmt.putArray("x5c");
			x5certs.add(base64Service.encodeToString(rawRegisterResponse.getAttestationCertificate().getEncoded()));
	        attStmt.put("sig", rawRegisterResponse.getSignature());

	        attestationObject.put("fmt", AttestationFormat.fido_u2f_super_gluu.getFmt());
	        attestationObject.set("attStmt", attStmt);

	        byte[] authData = generateAuthData(registerResponse.getClientData(), rawRegisterResponse);
	        attestationObject.put("authData", authData);

            attestationResult.getResponse().setAttestationObject(base64Service.urlEncodeToString(dataMapperService.cborWriteAsBytes(attestationObject)));
		} catch (CertificateEncodingException e) {
            throw errorResponseFactory.invalidRequest("Failed during encoding attestationCertificate", e);
		} catch (IOException e) {
            throw errorResponseFactory.invalidRequest("Failed to prepare attestationObject", e);
		}

        log.debug("Prepared U2F_V2 attestation verify request: {}", attestationResult.toString());
		return attestationResult;
	}

    private byte[] generateAuthData(ClientData clientData, RawRegisterResponse rawRegisterResponse) throws IOException {
    	byte[] rpIdHash = digestService.hashSha256(clientData.getOrigin());
    	byte[] flags = new byte[] { AuthenticatorDataParser.FLAG_USER_PRESENT | AuthenticatorDataParser.FLAG_ATTESTED_CREDENTIAL_DATA_INCLUDED };
    	byte[] counter = ByteBuffer.allocate(4).putInt(0).array();

        byte[] aaguid = ByteBuffer.allocate(16).array();

        byte[] credIDBuffer = rawRegisterResponse.getKeyHandle();

        byte[] credIDLenBuffer = ByteBuffer.allocate(2).putShort((short) credIDBuffer.length).array();


		JsonNode uncompressedECPoint = coseService.convertECKeyToUncompressedPoint(
				rawRegisterResponse.getUserPublicKey());

		byte[] cosePublicKeyBuffer = dataMapperService.cborWriteAsBytes(uncompressedECPoint);

		byte[] authData = ByteBuffer
				.allocate(rpIdHash.length + flags.length + counter.length + aaguid.length + credIDLenBuffer.length
						+ credIDBuffer.length + cosePublicKeyBuffer.length)
				.put(rpIdHash).put(flags).put(counter).put(aaguid).put(credIDLenBuffer).put(credIDBuffer)
				.put(cosePublicKeyBuffer).array();

		return authData;
    }

}
