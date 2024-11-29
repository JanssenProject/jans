/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.verifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;
import io.jans.entry.PublicKeyCredentialHints;
import io.jans.fido2.model.assertion.AssertionErrorResponseType;
import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.attestation.Response;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.error.ErrorResponseFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.TokenBindingSupport;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.orm.model.fido2.UserVerification;
import io.jans.service.net.NetworkService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tss.tpm.TPMS_ATTEST;
import tss.tpm.TPMT_PUBLIC;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class CommonVerifiers {

    @Inject
    private Logger log;

    @Inject
    private NetworkService networkService;

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

    public String verifyRpDomain(String origin, String rpId, List<RequestedParty> requestedParties) {

        origin = Strings.isNullOrEmpty(origin) ? rpId : origin;
        if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
            origin = "https://" + origin;
        }
        origin = networkService.getHost(origin);
        log.debug("Resolved origin to RP ID: " + origin);

        // Check if requestedParties is null or empty
        if (requestedParties == null || requestedParties.isEmpty()) {
            return origin;
        }
        // Check if the origin exists in any of the RequestedParties origins
        String finalOrigin = origin;
        boolean originExists = requestedParties.stream()
                .flatMap(requestedParty -> requestedParty.getOrigins().stream())
                .anyMatch(allowedOrigin -> allowedOrigin.equals(finalOrigin));

        if (!originExists) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.INVALID_ORIGIN, "The origin '" + origin + "' is not listed in the allowed origins.");
        }
        return origin;
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

    public void verifyAttestationOptions(AttestationOptions params) {
    	if(Strings.isNullOrEmpty(params.getUsername()))
    	{
    		throw new Fido2RuntimeException("Username is a mandatory parameter");
    	}
		/*
		 * long count = Arrays.asList(!Strings.isNullOrEmpty(params.getUsername()),
		 * !Strings.isNullOrEmpty(params.getDisplayName()), params.getAttestation() !=
		 * null) .parallelStream().filter(f -> !f).count(); if (count != 0) { throw new
		 * Fido2RuntimeException("Invalid parameters"); }
		 */
    }

    public void verifyAssertionOptions(AssertionOptions assertionOptions) {
        long count = Collections.singletonList(!Strings.isNullOrEmpty(assertionOptions.getUsername()))
                .parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw errorResponseFactory.invalidRequest("Invalid parameters : verifyAssertionOptions");
        }
    }

    public void verifyBasicPayload(AssertionResult assertionResult) {
        long count = Arrays.asList(assertionResult.getResponse() != null,
                !Strings.isNullOrEmpty(assertionResult.getType()),
                !Strings.isNullOrEmpty(assertionResult.getId())
        ).parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw errorResponseFactory.invalidRequest("Invalid parameters : verifyBasicPayload");
        }
    }

    public void verifyBasicAttestationResultRequest(AttestationResult attestationResult) {
        long count = Arrays.asList(attestationResult.getResponse() != null,
                !Strings.isNullOrEmpty(attestationResult.getType()),
                !Strings.isNullOrEmpty(attestationResult.getId())
        ).parallelStream().filter(f -> !f).count();
        if (count != 0) {
            throw errorResponseFactory.invalidRequest("Invalid parameters : verifyBasicAttestationResultRequest");
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

    public JsonNode verifyClientDataJSON(String clientDataJson) {
        if(Strings.isNullOrEmpty(clientDataJson)) {
            throw errorResponseFactory.invalidRequest("Invalid clientDataJson Null or Empty");
        }
        try {
            JsonNode clientJsonNode = dataMapperService.readTree(clientDataJson);
            return clientJsonNode;
        } catch (IOException e) {
            throw errorResponseFactory.invalidRequest("Can't parse message");
        }
    }

    public JsonNode verifyClientJSONTypeIsGet(JsonNode clientJsonNode) {
        verifyClientJSONType(clientJsonNode, "webauthn.get");
        return clientJsonNode;
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

    public JsonNode verifyClientJSON(String clientDataJSON) {
    	log.debug("clientDataJSON : "+ clientDataJSON);
        JsonNode clientJsonNode = null;
        try {
            if (Strings.isNullOrEmpty(clientDataJSON)) {
            	log.error("Client data JSON is missing");
                throw errorResponseFactory.invalidRequest("Client data JSON is missing");
            }
            clientJsonNode = dataMapperService
                    .readTree(new String(base64Service.urlDecode(clientDataJSON), StandardCharsets.UTF_8));
            if (clientJsonNode == null) {
            	log.error("Client data JSON is empty");
                throw errorResponseFactory.invalidRequest("Client data JSON is empty");
            }
        } catch (IOException e) {
        	log.error(e.getMessage());
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
            	log.error("Invalid tokenBinding entry. it should contains status");
                throw errorResponseFactory.invalidRequest("Invalid tokenBinding entry. it should contains status");
            }
            if (tokenBindingNode.hasNonNull("id")) {
                verifyThatFieldString(tokenBindingNode, "id");
            }
        }

        String origin = verifyThatFieldString(clientJsonNode, "origin");
        if (origin.isEmpty()) {
        	log.error("Client data origin parameter should be string");
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

    public UserVerification prepareUserVerification(UserVerification userVerification) {

        if (userVerification == null) {
            userVerification = UserVerification.preferred;
        }
        return userVerification;
    }

    public String verifyAssertionType(String type) {
        verifyNullOrEmptyString(type);
        if (!"public-key".equals(type)) {
            throw errorResponseFactory.invalidRequest("Invalid type");
        }
        return type;
    }

    public String verifyNullOrEmptyString(String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw errorResponseFactory.invalidRequest("Invalid data, value is null");
        }
        return input;
    }

    public String verifyCredentialId(CredAndCounterData attestationData, AttestationResult attestationResult) {
        String paramsKeyId = attestationResult.getId();
        
        if (StringHelper.isEmpty(paramsKeyId)) {
            throw errorResponseFactory.invalidRequest("Credential id attestationObject and response id mismatch");
        }

        String attestationDataCredId = attestationData.getCredId();
        if (!StringHelper.compare(attestationDataCredId, paramsKeyId)) {
            throw errorResponseFactory.invalidRequest("Credential id attestationObject and response id mismatch");
        }
        
        return paramsKeyId;
    }

    public String getChallenge(JsonNode clientJsonNode) {
        try {
            String clientDataChallenge = base64Service
                    .urlEncodeToStringWithoutPadding(base64Service.urlDecode(clientJsonNode.get("challenge").asText()));

            return clientDataChallenge;
        } catch (Exception ex) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.INVALID_CHALLENGE, "Can't get challenge from clientData");
        }
    }

    public long verifyTimeout(Long timeout) {
        if (timeout == null) {
            timeout = 90L;
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

    

    private void validateNodeNotNull(JsonNode node) throws Fido2RuntimeException {
        if ((node == null) || node.isNull()) {
            throw errorResponseFactory.invalidRequest("Invalid data, value is null");
        }
    }

    public TPMT_PUBLIC tpmParseToPublic(byte[] value) {
        return TPMT_PUBLIC.fromTpm(value);
    }

    public TPMS_ATTEST tpmParseToAttest(byte[] value) {
        return TPMS_ATTEST.fromTpm(value);
    }
}
