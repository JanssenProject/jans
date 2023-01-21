/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.verifier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

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

    public void verifyRpIdHash(AuthData authData, String domain) {
        try {
            byte[] retrievedRpIdHash = authData.getRpIdHash();
            byte[] calculatedRpIdHash = DigestUtils.getSha256Digest().digest(domain.getBytes("UTF-8"));
            log.debug("rpIDHash from Domain    HEX {}", Hex.encodeHexString(calculatedRpIdHash));
            log.debug("rpIDHash from Assertion HEX {}", Hex.encodeHexString(retrievedRpIdHash));
            if (!Arrays.equals(retrievedRpIdHash, calculatedRpIdHash)) {
                log.warn("hash from domain doesn't match hash from assertion HEX");
                throw new Fido2RuntimeException("Hashes don't match");
            }
        } catch (UnsupportedEncodingException e) {
            throw new Fido2RuntimeException("This encoding is not supported");
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
				.parallelStream().filter(f -> f == false).count();
		if (count != 0) {
			throw new Fido2RuntimeException("Invalid parameters");
		}
    }

    public void verifyAssertionOptions(JsonNode params) {
		long count = Arrays.asList(params.hasNonNull("username"))
				.parallelStream().filter(f -> f == false).count();
		if (count != 0) {
			throw new Fido2RuntimeException("Invalid parameters");
		}
    }

    public void verifyBasicPayload(JsonNode params) {
        long count = Arrays.asList(params.hasNonNull("response"),
        		params.hasNonNull("type"),
        		params.hasNonNull("id")
        ).parallelStream().filter(f -> f == false).count();
        if (count != 0) {
            throw new Fido2RuntimeException("Invalid parameters");
        }
    }

    public String verifyBase64UrlString(JsonNode node, String fieldName) {
        String value = verifyThatFieldString(node, fieldName);
        try {
            base64Service.urlDecode(value);
        } catch (IllegalArgumentException e) {
            throw new Fido2RuntimeException("Invalid \"" + fieldName + "\"");
        }

        return value;
    }

    public String verifyBase64String(JsonNode node) {
        if ((node == null) || node.isNull()) {
            throw new Fido2RuntimeException("Invalid data");
        }
        String value = verifyThatBinary(node);
        if (value.isEmpty()) {
            throw new Fido2RuntimeException("Invalid data");
        }
        try {
            base64Service.decode(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new Fido2RuntimeException("Invalid data");
        } catch (IllegalArgumentException e) {
            throw new Fido2RuntimeException("Invalid data");
        }

        return value;
    }

    protected String verifyThatString(JsonNode node, String fieldName) {
        if (!node.isTextual()) {
            if (node.fieldNames().hasNext()) {
                throw new Fido2RuntimeException("Invalid field " + node.fieldNames().next() + ". There is no filed " + fieldName);
            } else {
                throw new Fido2RuntimeException("Field hasn't sub field " + fieldName);
            }
        }

        return node.asText();
    }

    public String verifyThatFieldString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if ((fieldNode == null || fieldNode.isNull())) {
            throw new Fido2RuntimeException("Invalid \"" + fieldName + "\"");
        }

        return verifyThatString(fieldNode, fieldName);
    }

    public String verifyThatNonEmptyString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if ((fieldNode == null || fieldNode.isNull())) {
            throw new Fido2RuntimeException("Invalid \"" + fieldName + "\"");
        }

        String value = verifyThatString(fieldNode, fieldName);
        if (StringUtils.isEmpty(value)) {
            throw new Fido2RuntimeException("Invalid field " + node);
        } else {
            return value;
        }
    }

    public String verifyThatBinary(JsonNode node) {
        if (!node.isBinary()) {
            throw new Fido2RuntimeException("Invalid field " + node);
        }
        return node.asText();
    }

    public String verifyAuthData(JsonNode node) {
        if ((node == null) || node.isNull()) {
            throw new Fido2RuntimeException("Empty auth data");
        }

        String data = verifyThatBinary(node);
        if (data.isEmpty()) {
            throw new Fido2RuntimeException("Invalid field " + node);
        }
        return data;
    }

    public JsonNode verifyAuthStatement(JsonNode node) {
        if ((node == null) || node.isNull()) {
            throw new Fido2RuntimeException("Empty auth statement");
        }
        return node;
    }

    public int verifyAlgorithm(JsonNode alg, int registeredAlgorithmType) {
        if ((alg == null) || alg.isNull()) {
            throw new Fido2RuntimeException("Wrong algorithm");
        }
        int algorithmType = Integer.parseInt(alg.asText());
        if (algorithmType != registeredAlgorithmType) {
            throw new Fido2RuntimeException("Wrong algorithm");
        }
        return algorithmType;
    }

    public String verifyFmt(JsonNode fmtNode, String fieldName) {
        String fmt = verifyThatFieldString(fmtNode, fieldName);
        supportedAttestationFormats.stream().filter(f -> f.getAttestationFormat().getFmt().equals(fmt)).findAny()
                .orElseThrow(() -> new Fido2RuntimeException("Unsupported attestation format " + fmt));
        return fmt;
    }

    public void verifyAAGUIDZeroed(AuthData authData) {
        byte[] buf = authData.getAaguid();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != 0) {
                throw new Fido2RuntimeException("Invalid AAGUID");
            }
        }
    }

    public void verifyClientJSONTypeIsGet(JsonNode clientJsonNode) {
        verifyClientJSONType(clientJsonNode, "webauthn.get");
    }

    void verifyClientJSONType(JsonNode clientJsonNode, String type) {
        if (clientJsonNode.has("type")) {
            if (!type.equals(clientJsonNode.get("type").asText())) {
                throw new Fido2RuntimeException("Invalid client json parameters");
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
                throw new Fido2RuntimeException("Client data JSON is missing");
            }
            clientJsonNode = dataMapperService
                    .readTree(new String(base64Service.urlDecode(responseNode.get("clientDataJSON").asText()), Charset.forName("UTF-8")));
            if (clientJsonNode == null) {
                throw new Fido2RuntimeException("Client data JSON is empty");
            }
        } catch (IOException e) {
            throw new Fido2RuntimeException("Can't parse message");
        }

        long count = Arrays.asList(clientJsonNode.hasNonNull("challenge"), clientJsonNode.hasNonNull("origin"), clientJsonNode.hasNonNull("type")
        ).parallelStream().filter(f -> f == false).count();
        if (count != 0) {
            throw new Fido2RuntimeException("Invalid client json parameters");
        }
        verifyBase64UrlString(clientJsonNode, "challenge");

        if (clientJsonNode.hasNonNull("tokenBinding")) {
        	JsonNode tokenBindingNode = clientJsonNode.get("tokenBinding");
        	
        	if (tokenBindingNode.hasNonNull("status")) {
        		String status = verifyThatFieldString(tokenBindingNode, "status");
        		verifyTokenBindingSupport(status);
        	} else {
                throw new Fido2RuntimeException("Invalid tokenBinding entry. it should contaiss status");
        	}
            if (tokenBindingNode.hasNonNull("id")) {
            	verifyThatFieldString(tokenBindingNode, "id");
            }
        }

        String origin = verifyThatFieldString(clientJsonNode, "origin");
        if (origin.isEmpty()) {
            throw new Fido2RuntimeException("Client data origin parameter should be string");
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

        try {
        	TokenBindingSupport tokenBindingSupportEnum = TokenBindingSupport.fromStatusValue(status);
        	if (tokenBindingSupportEnum == null) {
                throw new Fido2RuntimeException("Wrong token binding status parameter " + status);
        	} else {
        		return tokenBindingSupportEnum;
        	}
        } catch (Exception e) {
            throw new Fido2RuntimeException("Wrong token binding status parameter " + e.getMessage(), e);
        }
    }

    public AuthenticatorAttachment verifyAuthenticatorAttachment(JsonNode authenticatorAttachment) {
    	if (authenticatorAttachment == null) {
    		return null;
    	}

        try {
        	AuthenticatorAttachment authenticatorAttachmentEnum = AuthenticatorAttachment.fromAttachmentValue(authenticatorAttachment.asText());
        	if (authenticatorAttachmentEnum == null) {
                throw new Fido2RuntimeException("Wrong authenticator attachment parameter " + authenticatorAttachment);
        	} else {
        		return authenticatorAttachmentEnum;
        	}
        } catch (Exception e) {
            throw new Fido2RuntimeException("Wrong authenticator attachment parameter " + e.getMessage(), e);
        }
    }

    public UserVerification verifyUserVerification(JsonNode userVerification) {
    	if (userVerification == null) {
    		return null;
    	}

    	try {
    		UserVerification userVerificationEnum = UserVerification.valueOf(userVerification.asText());
        	if (userVerificationEnum == null) {
                throw new Fido2RuntimeException("Wrong user verification parameter " + userVerification);
        	} else {
        		return userVerificationEnum;
        	}
        } catch (Exception e) {
            throw new Fido2RuntimeException("Wrong user verification parameter " + e.getMessage(), e);
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
            throw new Fido2RuntimeException("Invalid type");
        }
        return type;
    }

	public String verifyCredentialId(CredAndCounterData attestationData, JsonNode params) {
        String paramsKeyId = verifyBase64UrlString(params, "id");
        
        if (StringHelper.isEmpty(paramsKeyId)) {
            throw new Fido2RuntimeException("Credential id attestationObject and response id mismatch");
        }
        
		String attestationDataCredId = attestationData.getCredId();
        if (!StringHelper.compare(attestationDataCredId, paramsKeyId)) {
            throw new Fido2RuntimeException("Credential id attestationObject and response id mismatch");
        }
        
        return paramsKeyId;
	}

	public String getChallenge(JsonNode clientDataJSONNode) {
		try {
			String clientDataChallenge = base64Service
					.urlEncodeToStringWithoutPadding(base64Service.urlDecode(clientDataJSONNode.get("challenge").asText()));

			return clientDataChallenge;
		} catch (Exception ex) {
			throw new Fido2RuntimeException("Can't get challenge from clientData");
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
        		metaDataStatement.hasNonNull("description")).parallelStream().filter(f -> f == false).count();
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

}
