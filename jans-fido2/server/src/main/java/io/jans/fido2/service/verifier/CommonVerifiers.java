/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.verifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import io.jans.fido2.model.assertion.AssertionErrorResponseType;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.ctap.TokenBindingSupport;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.sg.SuperGluuMode;
import io.jans.orm.model.fido2.UserVerification;
import io.jans.service.net.NetworkService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class CommonVerifiers {

    public static final String SUPER_GLUU_REQUEST = "super_gluu_request";
    public static final String SUPER_GLUU_MODE = "super_gluu_request_mode";
    public static final String SUPER_GLUU_REQUEST_CANCEL = "super_gluu_request_cancel";
    public static final String SUPER_GLUU_APP_ID = "super_gluu_app_id";
    public static final String SUPER_GLUU_KEY_HANDLE = "super_gluu_key_handle";

    @Inject
    private Logger log;

    @Inject
    private NetworkService networkService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Base64Service base64Service;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Instance<AttestationFormatProcessor> supportedAttestationFormats;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    public void verifyRpIdHash(AuthData authData, String domain) {
        byte[] retrievedRpIdHash = authData.getRpIdHash();
        byte[] calculatedRpIdHash = DigestUtils.getSha256Digest().digest(domain.getBytes(StandardCharsets.UTF_8));
        log.debug("rpIDHash from Domain    HEX {}", Hex.encodeHexString(calculatedRpIdHash));
        log.debug("rpIDHash from Assertion HEX {}", Hex.encodeHexString(retrievedRpIdHash));
        if (!Arrays.equals(retrievedRpIdHash, calculatedRpIdHash)) {
            log.warn("hash from domain doesn't match hash from assertion HEX");
            throw new Fido2RuntimeException("Hashes don't match");
        }
    }

    public String verifyRpDomain(JsonNode params) {
        String documentDomain;
        if (params.hasNonNull("documentDomain")) {
            documentDomain = params.get("documentDomain").asText();
        } else {
            documentDomain = appConfiguration.getIssuer();
        }
        documentDomain = networkService.getHost(documentDomain);

        return documentDomain;
    }

    public void verifyCounter(int oldCounter, int newCounter) {
        log.debug("old counter {} new counter {} ", oldCounter, newCounter);
        if (newCounter == 0 && oldCounter == 0)
            return;
        if (newCounter <= oldCounter) {
            throw new Fido2CompromisedDevice("Counter did not increase");
        }
    }

    public void verifyCounter(int counter) {
        if (counter < 0) {
            throw new Fido2RuntimeException("Invalid field : counter");
        }
    }

    public void verifyAttestationOptions(JsonNode params) {
        long count = Arrays.asList(params.hasNonNull("username"),
                        params.hasNonNull("displayName"),
                        params.hasNonNull("attestation"))
                .parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw new Fido2RuntimeException("Invalid parameters");
        }
    }

    public void verifyAssertionOptions(JsonNode params) {
        long count = Collections.singletonList(params.hasNonNull("username"))
                .parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw errorResponseFactory.invalidRequest("Invalid parameters");
        }
    }

    public void verifyBasicPayload(JsonNode params) {
        long count = Arrays.asList(params.hasNonNull("response"),
                params.hasNonNull("type"),
                params.hasNonNull("id")
        ).parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw errorResponseFactory.invalidRequest("Invalid parameters");
        }
    }

    public String verifyBase64UrlString(JsonNode node, String fieldName) {
        String value = verifyThatFieldString(node, fieldName);
        try {
            base64Service.urlDecode(value);
        } catch (IllegalArgumentException e) {
            throw errorResponseFactory.invalidRequest("Invalid \"" + fieldName + "\"");
        }

        return value;
    }

    public String verifyBase64String(JsonNode node) {
        validateNodeNotNull(node);
        String value = verifyThatBinary(node);
        if (value.isEmpty()) {
            throw new Fido2RuntimeException("Invalid data");
        }
        try {
            base64Service.decode(value.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new Fido2RuntimeException("Invalid data");
        }

        return value;
    }

    protected String verifyThatString(JsonNode node, String fieldName) {
        if (!node.isTextual()) {
            if (node.fieldNames().hasNext()) {
                throw errorResponseFactory.invalidRequest("Invalid field " + node.fieldNames().next() + ". There is no filed " + fieldName);
            } else {
                throw errorResponseFactory.invalidRequest("Field hasn't sub field " + fieldName);
            }
        }

        return node.asText();
    }

    public String verifyThatFieldString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        validateNodeNotNull(fieldNode);

        return verifyThatString(fieldNode, fieldName);
    }

    public String verifyThatNonEmptyString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        validateNodeNotNull(fieldNode);

        String value = verifyThatString(fieldNode, fieldName);
        if (StringUtils.isEmpty(value)) {
            throw errorResponseFactory.invalidRequest("Invalid field " + node);
        } else {
            return value;
        }
    }

    public String verifyThatBinary(JsonNode node) {
        if (!node.isBinary()) {
            throw errorResponseFactory.invalidRequest("Invalid field " + node);
        }
        return node.asText();
    }

    public String verifyAuthData(JsonNode node) {
        validateNodeNotNull(node);

        String data = verifyThatBinary(node);
        if (data.isEmpty()) {
            throw errorResponseFactory.invalidRequest("Invalid field " + node);
        }
        return data;
    }

    public JsonNode verifyAuthStatement(JsonNode node) {
        validateNodeNotNull(node);

        return node;
    }

    public int verifyAlgorithm(JsonNode alg, int registeredAlgorithmType) {
        validateNodeNotNull(alg);
        int algorithmType = Integer.parseInt(alg.asText());
        if (algorithmType != registeredAlgorithmType) {
            throw errorResponseFactory.invalidRequest("Wrong algorithm");
        }
        return algorithmType;
    }

    public String verifyFmt(JsonNode fmtNode, String fieldName) {
        String fmt = verifyThatFieldString(fmtNode, fieldName);
        supportedAttestationFormats.stream().filter(f -> f.getAttestationFormat().getFmt().equals(fmt)).findAny()
                .orElseThrow(() -> errorResponseFactory.badRequestException(AttestationErrorResponseType.UNSUPPORTED_ATTESTATION_FORMAT, "Unsupported attestation format " + fmt));
        return fmt;
    }

    public void verifyAAGUIDZeroed(AuthData authData) {
        byte[] buf = authData.getAaguid();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != 0) {
                throw errorResponseFactory.invalidRequest("Invalid AAGUID");
            }
        }
    }

    public void verifyClientJSONTypeIsGet(JsonNode clientJsonNode) {
            verifyClientJSONType(clientJsonNode, "webauthn.get");
    }

    void verifyClientJSONType(JsonNode clientJsonNode, String type) {
        if (clientJsonNode.has("type")) {
            if (!type.equals(clientJsonNode.get("type").asText())) {
                throw errorResponseFactory.invalidRequest("Invalid client json parameters");
            }
        }
    }

    public void verifyClientJSONTypeIsCreate(JsonNode clientJsonNode) {
        verifyClientJSONType(clientJsonNode, "webauthn.create");
    }

    public JsonNode verifyClientJSON(JsonNode responseNode) {
        JsonNode clientJsonNode = null;
        try {
            if (!responseNode.hasNonNull("clientDataJSON")) {
                throw errorResponseFactory.invalidRequest("Client data JSON is missing");
            }
            clientJsonNode = dataMapperService
                    .readTree(new String(base64Service.urlDecode(responseNode.get("clientDataJSON").asText()), StandardCharsets.UTF_8));
            if (clientJsonNode == null) {
                throw errorResponseFactory.invalidRequest("Client data JSON is empty");
            }
        } catch (IOException e) {
            throw errorResponseFactory.invalidRequest("Can't parse message");
        }

        long count = Arrays.asList(clientJsonNode.hasNonNull("challenge"), clientJsonNode.hasNonNull("origin"), clientJsonNode.hasNonNull("type")
        ).parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw errorResponseFactory.invalidRequest("Invalid client json parameters");
        }
        verifyBase64UrlString(clientJsonNode, "challenge");

        if (clientJsonNode.hasNonNull("tokenBinding")) {
            JsonNode tokenBindingNode = clientJsonNode.get("tokenBinding");

            if (tokenBindingNode.hasNonNull("status")) {
                String status = verifyThatFieldString(tokenBindingNode, "status");
                verifyTokenBindingSupport(status);
            } else {
                throw errorResponseFactory.invalidRequest("Invalid tokenBinding entry. it should contains status");
            }
            if (tokenBindingNode.hasNonNull("id")) {
                verifyThatFieldString(tokenBindingNode, "id");
            }
        }

        String origin = verifyThatFieldString(clientJsonNode, "origin");
        if (origin.isEmpty()) {
            throw errorResponseFactory.invalidRequest("Client data origin parameter should be string");
        }
        
        return clientJsonNode;
    }

    public JsonNode verifyClientRaw(JsonNode responseNode) {
        if (!responseNode.hasNonNull("clientDataRaw")) {
            throw new Fido2RuntimeException("Client data RAW is missing");
        }

        return responseNode.get("clientDataRaw");
    }

    public void verifyTPMVersion(JsonNode ver) {
        if (!"2.0".equals(ver.asText())) {
            throw new Fido2RuntimeException("Invalid TPM Attestation version");
        }
    }

    public AttestationConveyancePreference verifyAttestationConveyanceType(JsonNode params) {
        AttestationConveyancePreference attestationConveyancePreference = null;
        if (params.has("attestation")) {
            String type = verifyThatFieldString(params, "attestation");
            attestationConveyancePreference = AttestationConveyancePreference.valueOf(type);
        }

        if (attestationConveyancePreference == null) {
            attestationConveyancePreference = AttestationConveyancePreference.direct;
        }
        
        return attestationConveyancePreference;
    }

    public TokenBindingSupport verifyTokenBindingSupport(String status) {
    	if (status == null) {
    		return null;
    	}

        TokenBindingSupport tokenBindingSupportEnum = TokenBindingSupport.fromStatusValue(status);
        if (tokenBindingSupportEnum == null) {
            throw errorResponseFactory.invalidRequest("Wrong token binding status parameter " + status);
        } else {
            return tokenBindingSupportEnum;
        }
    }

    public AuthenticatorAttachment verifyAuthenticatorAttachment(JsonNode authenticatorAttachment) {
        if (authenticatorAttachment == null) {
            return null;
        }

        AuthenticatorAttachment authenticatorAttachmentEnum = AuthenticatorAttachment.fromAttachmentValue(authenticatorAttachment.asText());
        if (authenticatorAttachmentEnum == null) {
            throw new Fido2RuntimeException("Wrong authenticator attachment parameter " + authenticatorAttachment);
        } else {
            return authenticatorAttachmentEnum;
        }
    }

    public UserVerification verifyUserVerification(JsonNode userVerification) {
        if (userVerification == null) {
            return null;
        }

        try {
            return UserVerification.valueOf(userVerification.asText());
        } catch (Exception e) {
            throw new Fido2RuntimeException("Wrong user verification parameter " + userVerification);
        }
    }

    public UserVerification prepareUserVerification(JsonNode params) {
        UserVerification userVerification = UserVerification.preferred;

        if (params.hasNonNull("userVerification")) {
            userVerification = verifyUserVerification(params.get("userVerification"));
        }

        return userVerification;
    }

    public Boolean verifyRequireResidentKey(JsonNode requireResidentKey) {
        if (requireResidentKey == null) {
            return null;
        }

        try {
            return requireResidentKey.asBoolean();
        } catch (Exception e) {
            throw new Fido2RuntimeException("Wrong authenticator attachment parameter " + e.getMessage(), e);
        }
    }

    public String verifyAssertionType(JsonNode typeNode, String fieldName) {
        String type = verifyThatFieldString(typeNode, fieldName);
        if (!"public-key".equals(type)) {
            throw errorResponseFactory.invalidRequest("Invalid type");
        }
        return type;
    }

    public String verifyCredentialId(CredAndCounterData attestationData, JsonNode params) {
        String paramsKeyId = verifyBase64UrlString(params, "id");
        
        if (StringHelper.isEmpty(paramsKeyId)) {
            throw errorResponseFactory.invalidRequest("Credential id attestationObject and response id mismatch");
        }

        String attestationDataCredId = attestationData.getCredId();
        if (!StringHelper.compare(attestationDataCredId, paramsKeyId)) {
            throw errorResponseFactory.invalidRequest("Credential id attestationObject and response id mismatch");
        }
        
        return paramsKeyId;
    }

    public String getChallenge(JsonNode clientDataJSONNode) {
        try {
            String clientDataChallenge = base64Service
                    .urlEncodeToStringWithoutPadding(base64Service.urlDecode(clientDataJSONNode.get("challenge").asText()));

            return clientDataChallenge;
        } catch (Exception ex) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.INVALID_CHALLENGE, "Can't get challenge from clientData");
        }
    }

    public int verifyTimeout(JsonNode params) {
        int timeout = 90;
        if (params.hasNonNull("timeout")) {
            timeout = params.get("timeout").asInt(timeout);
        }

        return timeout;
    }

    // fix: fetching metadataStatement from the individual metadataNode (causing NPE) - also, removing metadata.hasNonNull("assertionScheme") as per MDS3 upgrade -  https://medium.com/webauthnworks/webauthn-fido2-whats-new-in-mds3-migrating-from-mds2-to-mds3-a271d82cb774
    public void verifyThatMetadataIsValid(JsonNode metadata)  {

        JsonNode metaDataStatement= null;
        try {
            metaDataStatement = dataMapperService
                    .readTree(metadata.get("metadataStatement").toPrettyString());
        } catch (IOException e) {
            throw new Fido2RuntimeException("Unable to process metadataStatement:",e);
        }
        long count = Arrays.asList(metaDataStatement.hasNonNull("aaguid"), metaDataStatement.hasNonNull("attestationTypes"),
                metaDataStatement.hasNonNull("description")).parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw new Fido2RuntimeException("Invalid parameters in metadata");
        }
    }

    public boolean hasSuperGluu(JsonNode params) {
        if (params.hasNonNull(SUPER_GLUU_REQUEST)) {
            JsonNode node = params.get(SUPER_GLUU_REQUEST);
            return node.isBoolean() && node.asBoolean();
        }

        return false;
    }

    public void verifyNotUseGluuParameters(JsonNode params) {
        // Protect generic U2F/Fido2 from sending requests with Super Gluu parameters
        if (params.hasNonNull(SUPER_GLUU_REQUEST) || params.hasNonNull(SUPER_GLUU_MODE) ||
            params.hasNonNull(SUPER_GLUU_APP_ID) || params.hasNonNull(SUPER_GLUU_KEY_HANDLE) ||
                params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)) {
            throw errorResponseFactory.badRequestException(AssertionErrorResponseType.CONFLICT_WITH_SUPER_GLUU, "Input request conflicts with Super Gluu parameters");
        }
    }

    public boolean isSuperGluuOneStepMode(JsonNode params) {
        if (!hasSuperGluu(params)) {
            return false;
        }
        if (!params.hasNonNull(SUPER_GLUU_MODE)) {
            return false;
        }
        JsonNode node = params.get(SUPER_GLUU_MODE);
        return SuperGluuMode.ONE_STEP == SuperGluuMode.fromModeValue(node.asText());
    }

    public boolean isSuperGluuCancelRequest(JsonNode params) {
        if (!hasSuperGluu(params)) {
            return false;
        }
        if (!params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)) {
            return false;
        }
        JsonNode node = params.get(SUPER_GLUU_REQUEST_CANCEL);
        return node.isBoolean() && node.asBoolean();
    }

    private void validateNodeNotNull(JsonNode node) throws Fido2RuntimeException {
        if ((node == null) || node.isNull()) {
            throw errorResponseFactory.invalidRequest("Invalid data, value is null");
        }
    }
}
