package org.gluu.oxauth.fido2.service.verifier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.fido2.ctap.AttestationConveyancePreference;
import org.gluu.oxauth.fido2.ctap.AuthenticatorAttachment;
import org.gluu.oxauth.fido2.ctap.UserVerification;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.SignatureValidator;
import org.gluu.oxauth.fido2.service.processors.AttestationFormatProcessor;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class CommonVerifiers {

    private static final int FLAG_USER_PRESENT = 0x01;
    private static final int FLAG_ATTESTED_CREDENTIAL_DATA_INCLUDED = 0x40;
    private static final int FLAG_USER_VERIFIED = 0x04;
    private static final int FLAG_EXTENSION_DATA_INCLUDED = 0x80;

    @Inject
    private Logger log;

    @Inject
    private Base64Service base64Service;

    @Inject
    private DataMapperService dataMapperService;
    
    @Inject
    private SignatureValidator signatureValidator;

    @Inject
    private Instance<AttestationFormatProcessor> supportedAttestationFormats;

    public void verifyU2FAttestationSignature(AuthData authData, byte[] clientDataHash, String signature, Certificate certificate,
            int signatureAlgorithm) {
        int bufferSize = 0;
        byte[] reserved = new byte[] { 0x00 };
        bufferSize += reserved.length;
        byte[] rpIdHash = authData.getRpIdHash();
        bufferSize += rpIdHash.length;

        bufferSize += clientDataHash.length;
        byte[] credId = authData.getCredId();
        bufferSize += credId.length;
        byte[] publicKey = convertCOSEtoPublicKey(authData.getCosePublicKey());
        bufferSize += publicKey.length;

        byte[] signatureBase = ByteBuffer.allocate(bufferSize).put(reserved).put(rpIdHash).put(clientDataHash).put(credId).put(publicKey).array();
        byte[] signatureBytes = base64Service.decode(signature.getBytes());
        log.debug("Signature {}", Hex.encodeHexString(signatureBytes));
        log.debug("Signature Base {}", Hex.encodeHexString(signatureBase));
        verifySignature(signatureBytes, signatureBase, certificate, signatureAlgorithm);
    }

    void verifyPackedAttestationSignature(AuthData authData, byte[] clientDataHash, String signature, Certificate certificate,
            int signatureAlgorithm) {
        int bufferSize = 0;
        byte[] rpIdHashBuffer = authData.getRpIdHash();
        bufferSize += rpIdHashBuffer.length;

        byte[] flagsBuffer = authData.getFlags();
        bufferSize += flagsBuffer.length;

        byte[] countersBuffer = authData.getCounters();
        bufferSize += countersBuffer.length;

        bufferSize += clientDataHash.length;
        byte[] signatureBase = ByteBuffer.allocate(bufferSize).put(rpIdHashBuffer).put(flagsBuffer).put(countersBuffer).put(clientDataHash).array();
        byte[] signatureBytes = base64Service.decode(signature.getBytes());
        log.debug("Signature {}", Hex.encodeHexString(signatureBytes));
        log.debug("Signature Base {}", Hex.encodeHexString(signatureBase));
        log.debug("Signature BaseLen {}", signatureBase.length);
        verifySignature(signatureBytes, signatureBase, certificate, signatureAlgorithm);
    }

    public void verifyPackedAttestationSignature(byte[] authData, byte[] clientDataHash, String signature, PublicKey key, int signatureAlgorithm) {
        int bufferSize = 0;

        bufferSize += authData.length;
        bufferSize += clientDataHash.length;
        byte[] signatureBase = ByteBuffer.allocate(bufferSize).put(authData).put(clientDataHash).array();
        byte[] signatureBytes = base64Service.decode(signature.getBytes());
        log.debug("Signature {}", Hex.encodeHexString(signatureBytes));
        log.debug("Signature Base {}", Hex.encodeHexString(signatureBase));
        log.debug("Signature BaseLen {}", signatureBase.length);
        signatureValidator.verifySignature(signatureBytes, signatureBase, key, signatureAlgorithm);
    }

    public void verifyPackedAttestationSignature(byte[] authData, byte[] clientDataHash, String signature, Certificate certificate,
            int signatureAlgorithm) {
        verifyPackedAttestationSignature(authData, clientDataHash, signature, certificate.getPublicKey(), signatureAlgorithm);
    }

    public void verifyAssertionSignature(AuthData authData, byte[] clientDataHash, String signature, PublicKey publicKey, int signatureAlgorithm) {
        int bufferSize = 0;
        byte[] rpIdHash = authData.getRpIdHash();
        bufferSize += rpIdHash.length;

        byte[] flags = authData.getFlags();
        bufferSize += flags.length;

        byte[] counters = authData.getCounters();
        bufferSize += counters.length;

        byte[] extensionsBuffer = authData.getExtensions();
        if (extensionsBuffer == null) {
        	extensionsBuffer = new byte[0];
        }
        bufferSize += extensionsBuffer.length;

        bufferSize += clientDataHash.length;
        log.debug("Client data hash HEX {}", Hex.encodeHexString(clientDataHash));

        byte[] signatureBase = ByteBuffer.allocate(bufferSize).put(rpIdHash).put(flags).put(counters).put(extensionsBuffer).put(clientDataHash).array();
        byte[] signatureBytes = base64Service.urlDecode(signature.getBytes());
        log.debug("Signature {}", Hex.encodeHexString(signatureBytes));
        log.debug("Signature Base {}", Hex.encodeHexString(signatureBase));
        log.debug("Signature BaseLen {}", signatureBase.length);

        signatureValidator.verifySignature(signatureBytes, signatureBase, publicKey, signatureAlgorithm);
    }

    public boolean verifyUserPresent(AuthData authData) {
        if ((authData.getFlags()[0] & FLAG_USER_PRESENT) == 1) {
            return true;
        } else {
            throw new Fido2RPRuntimeException("User not present");
        }
    }

    public boolean verifyUserVerified(AuthData authData) {
        if ((authData.getFlags()[0] & FLAG_USER_VERIFIED) == 1) {
            return true;
        } else {
            return false;
        }
    }

    public void verifyRpIdHash(AuthData authData, String domain) {
        try {
            byte[] retrievedRpIdHash = authData.getRpIdHash();
            byte[] calculatedRpIdHash = DigestUtils.getSha256Digest().digest(domain.getBytes("UTF-8"));
            log.debug("rpIDHash from Domain    HEX {}", Hex.encodeHexString(calculatedRpIdHash));
            log.debug("rpIDHash from Assertion HEX {}", Hex.encodeHexString(retrievedRpIdHash));
            if (!Arrays.equals(retrievedRpIdHash, calculatedRpIdHash)) {
                log.warn("hash from domain doesn't match hash from assertion HEX ");
                throw new Fido2RPRuntimeException("Hashes don't match");
            }
        } catch (UnsupportedEncodingException e) {
            throw new Fido2RPRuntimeException("This encoding is not supported");
        }
    }

    public void verifyCounter(int oldCounter, int newCounter) {
        log.debug("old counter {} new counter {} ", oldCounter, newCounter);
        if (newCounter == 0 && oldCounter == 0)
            return;
        if (newCounter <= oldCounter) {
            throw new Fido2RPRuntimeException("Counter did not increase");
        }
    }

    private byte[] convertCOSEtoPublicKey(byte[] cosePublicKey) {
        try {
            JsonNode cborPublicKey = dataMapperService.cborReadTree(cosePublicKey);
            byte[] x = base64Service.decode(cborPublicKey.get("-2").asText());
            byte[] y = base64Service.decode(cborPublicKey.get("-3").asText());
            byte[] keyBytes = ByteBuffer.allocate(1 + x.length + y.length).put((byte) 0x04).put(x).put(y).array();
            log.debug("KeyBytes HEX {}", Hex.encodeHexString(keyBytes));
            return keyBytes;
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Can't parse public key");
        }
    }

    public void verifySignature(byte[] signature, byte[] signatureBase, Certificate certificate, int signatureAlgorithm) {
    	signatureValidator.verifySignature(signature, signatureBase, certificate.getPublicKey(), signatureAlgorithm);
    }

    public void verifyOptions(JsonNode params) {
		long count = Arrays.asList(params.hasNonNull("username"),
				params.hasNonNull("displayName"),
				params.hasNonNull("attestation"))
				.parallelStream().filter(f -> f == false).count();
		if (count != 0) {
			throw new Fido2RPRuntimeException("Invalid parameters");
		}
    }

    public void verifyBasicPayload(JsonNode params) {
        long count = Arrays.asList(params.hasNonNull("response"), params.hasNonNull("type")
        ).parallelStream().filter(f -> f == false).count();
        if (count != 0) {
            throw new Fido2RPRuntimeException("Invalid parameters");
        }
    }

    public String verifyBase64UrlString(JsonNode node, String fieldName) {
        String value = verifyThatString(node, fieldName);
        try {
            base64Service.urlDecode(value);
        } catch (IllegalArgumentException e) {
            throw new Fido2RPRuntimeException("Invalid \"" + fieldName + "\"");
        }

        return value;
    }

    protected String verifyThatString(JsonNode node) {
        if (!node.isTextual()) {
            if (node.fieldNames().hasNext()) {
                throw new Fido2RPRuntimeException("Invalid field " + node.fieldNames().next());
            } else {
                throw new Fido2RPRuntimeException("Field hasn't sub fields");
            }
        }

        return node.asText();
    }

    public String verifyThatString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if ((fieldNode == null || fieldNode.isNull())) {
            throw new Fido2RPRuntimeException("Invalid \"" + fieldName + "\"");
        }

        return verifyThatString(fieldNode);
    }

    public String verifyThatNonEmptyString(JsonNode node) {
        String value = verifyThatString(node);
        if (StringUtils.isEmpty(value)) {
            throw new Fido2RPRuntimeException("Invalid field " + node);
        } else {
            return value;
        }
    }

    public String verifyAuthData(JsonNode node) {
        if ((node == null) || node.isNull()) {
            throw new Fido2RPRuntimeException("Empty auth data");
        }

        String data = verifyThatBinary(node);
        if (data.isEmpty()) {
            throw new Fido2RPRuntimeException("Invalid field " + node);
        }
        return data;
    }

    public JsonNode verifyAuthStatement(JsonNode node) {
        if ((node == null) || node.isNull()) {
            throw new Fido2RPRuntimeException("Empty auth statement");
        }
        return node;
    }

    public String verifyThatBinary(JsonNode node) {
        if (!node.isBinary()) {
            throw new Fido2RPRuntimeException("Invalid field " + node);
        }
        return node.asText();
    }

    public void verifyCounter(int counter) {
        if (counter < 0) {
            throw new Fido2RPRuntimeException("Invalid field : counter");
        }
    }

    public boolean verifyAtFlag(byte[] flags) {
        return (flags[0] & FLAG_ATTESTED_CREDENTIAL_DATA_INCLUDED) == FLAG_ATTESTED_CREDENTIAL_DATA_INCLUDED;
    }

    public boolean verifyEdFlag(byte[] flags) {
        return (flags[0] & FLAG_EXTENSION_DATA_INCLUDED) == FLAG_EXTENSION_DATA_INCLUDED;
    }

    public void verifyAttestationBuffer(byte[] attestationBuffer) {
        if (attestationBuffer.length == 0) {
            throw new Fido2RPRuntimeException("Invalid attestation data buffer");
        }
    }

    public void verifyExtensionBuffer(byte[] extensionBuffer) {
        if (extensionBuffer.length == 0) {
            throw new Fido2RPRuntimeException("Invalid extension data buffer");
        }
    }

    public void verifyNoLeftovers(byte[] leftovers) {
        if (leftovers.length > 0) {
            throw new Fido2RPRuntimeException("Invalid attestation data buffer: leftovers");
        }
    }

    public int verifyAlgorithm(JsonNode alg, int registeredAlgorithmType) {
        if ((alg == null) || alg.isNull()) {
            throw new Fido2RPRuntimeException("Wrong algorithm");
        }
        int algorithmType = Integer.parseInt(alg.asText());
        if (algorithmType != registeredAlgorithmType) {
            throw new Fido2RPRuntimeException("Wrong algorithm");
        }
        return algorithmType;
    }

    public String verifyBase64String(JsonNode node) {
        if ((node == null) || node.isNull()) {
            throw new Fido2RPRuntimeException("Invalid data");
        }
        String value = verifyThatBinary(node);
        if (value.isEmpty()) {
            throw new Fido2RPRuntimeException("Invalid data");
        }
        try {
            base64Service.decode(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new Fido2RPRuntimeException("Invalid data");
        } catch (IllegalArgumentException e) {
            throw new Fido2RPRuntimeException("Invalid data");
        }

        return value;

    }

    public void verifyPackedSurrogateAttestationSignature(byte[] authData, byte[] clientDataHash, String signature, PublicKey publicKey,
            int signatureAlgorithm) {
        int bufferSize = 0;
        bufferSize += authData.length;
        bufferSize += clientDataHash.length;
        byte[] signatureBase = ByteBuffer.allocate(bufferSize).put(authData).put(clientDataHash).array();
        byte[] signatureBytes = base64Service.decode(signature.getBytes());
        log.debug("Signature {}", Hex.encodeHexString(signatureBytes));
        log.debug("Signature Base {}", Hex.encodeHexString(signatureBase));
        log.debug("Signature BaseLen {}", signatureBase.length);
        signatureValidator.verifySignature(signatureBytes, signatureBase, publicKey, signatureAlgorithm);
    }

    public String verifyFmt(JsonNode fmtNode, String fieldName) {
        String fmt = verifyThatString(fmtNode, fieldName);
        supportedAttestationFormats.stream().filter(f -> f.getAttestationFormat().getFmt().equals(fmt)).findAny()
                .orElseThrow(() -> new Fido2RPRuntimeException("Unsupported attestation format " + fmt));
        return fmt;
    }

    public void verifyAAGUIDZeroed(AuthData authData) {
        byte[] buf = authData.getAaguid();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != 0) {
                throw new Fido2RPRuntimeException("Invalid AAGUID");
            }
        }
    }

    public void verifyTPMVersion(JsonNode ver) {
        if (!"2.0".equals(ver.asText())) {
            throw new Fido2RPRuntimeException("Invalid TPM Attestation version");
        }
    }

    public AttestationConveyancePreference verifyAttestationConveyanceType(JsonNode params) {
        if (params.has("attestation")) {
            String type = verifyThatString(params.get("attestation"));
            return AttestationConveyancePreference.valueOf(type);
        } else {
            return AttestationConveyancePreference.direct;
        }
    }

    public void verifyClientJSONTypeIsGet(JsonNode clientJsonNode) {
        verifyClientJSONType(clientJsonNode, "webauthn.get");
    }

    void verifyClientJSONType(JsonNode clientJsonNode, String type) {
        if (clientJsonNode.has("type")) {
            if (!type.equals(clientJsonNode.get("type").asText())) {
                throw new Fido2RPRuntimeException("Invalid client json parameters");
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
                throw new Fido2RPRuntimeException("Client data JSON is missing");
            }
            clientJsonNode = dataMapperService
                    .readTree(new String(base64Service.urlDecode(responseNode.get("clientDataJSON").asText()), Charset.forName("UTF-8")));
            if (clientJsonNode == null) {
                throw new Fido2RPRuntimeException("Client data JSON is empty");
            }
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Can't parse message");
        }

        long count = Arrays.asList(clientJsonNode.hasNonNull("challenge"), clientJsonNode.hasNonNull("origin"), clientJsonNode.hasNonNull("type")
        ).parallelStream().filter(f -> f == false).count();
        if (count != 0) {
            throw new Fido2RPRuntimeException("Invalid client json parameters");
        }
        verifyBase64UrlString(clientJsonNode, "challenge");

        if (clientJsonNode.hasNonNull("tokenBinding")) {
        	JsonNode tokenBindingNode = clientJsonNode.get("tokenBinding");
        	
        	if (tokenBindingNode.hasNonNull("status")) {
        		verifyThatString(tokenBindingNode.get("status"), "status");
        	} else {
                throw new Fido2RPRuntimeException("Invalid tokenBinding entry. it should coaints status");
        	}
            if (tokenBindingNode.hasNonNull("id")) {
            	verifyThatString(tokenBindingNode.get("id"), "id");
            }
        }

        String origin = verifyThatString(clientJsonNode.get("origin"));
        if (origin.isEmpty()) {
            throw new Fido2RPRuntimeException("Invalid client json parameters");
        }

        verifyClientJSONTypeIsCreate(clientJsonNode);
        
        return clientJsonNode;
    }

    public AuthenticatorAttachment verifyAuthenticatorAttachment(JsonNode authenticatorAttachment) {
    	if (authenticatorAttachment == null) {
    		return null;
    	}

        try {
            return AuthenticatorAttachment.fromAttachmentValue(authenticatorAttachment.asText());
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("Wrong authenticator attachment parameter " + e.getMessage(), e);
        }
    }

    public UserVerification verifyUserVerification(JsonNode userVerification) {
    	if (userVerification == null) {
    		return null;
    	}

    	try {
            return UserVerification.valueOf(userVerification.asText());
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("Wrong user verification parameter " + e.getMessage(), e);
        }
    }

    public Boolean verifyRequireResidentKey(JsonNode requireResidentKey) {
    	if (requireResidentKey == null) {
    		return null;
    	}

        try {
            return requireResidentKey.asBoolean();
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("Wrong authenticator attachment parameter " + e.getMessage(), e);
        }
    }

    public void verifyAttestationSignature(AuthData authData, byte[] clientDataHash, String signature, Certificate certificate,
            int signatureAlgorithm) {

        int bufferSize = 0;
        byte[] authDataBuffer = authData.getAttestationBuffer();
        bufferSize += authDataBuffer.length;
        bufferSize += clientDataHash.length;

        byte[] signatureBase = ByteBuffer.allocate(bufferSize).put(authDataBuffer).put(clientDataHash).array();
        byte[] signatureBytes = base64Service.decode(signature.getBytes());
        log.debug("Signature {}", Hex.encodeHexString(signatureBytes));
        log.debug("Signature Base {}", Hex.encodeHexString(signatureBase));
        verifySignature(signatureBytes, signatureBase, certificate, signatureAlgorithm);
    }

    public String verifyAssertionType(JsonNode typeNode) {
        String type = verifyThatString(typeNode);
        if (!"public-key".equals(type)) {
            throw new Fido2RPRuntimeException("Invalid type");
        }
        return type;
    }

	public void verifyUserVerificationOption(UserVerification userVerification, AuthData authData) {
		if (userVerification == UserVerification.required) {
            verifyRequiredUserPresent(authData);
        }
        if (userVerification == UserVerification.preferred) {
            verifyPreferredUserPresent(authData);
        }
        if (userVerification == UserVerification.discouraged) {
            verifyDiscouragedUserPresent(authData);
        }
	}

    public void verifyRequiredUserPresent(AuthData authData) {
        log.debug("Required user present {}", Hex.encodeHexString(authData.getFlags()));
        byte flags = authData.getFlags()[0];

        if (!isUserVerified(flags)) {
            throw new Fido2RPRuntimeException("User required is not present");
        }
    }

    public void verifyPreferredUserPresent(AuthData authData) {
        log.debug("Preferred user present {}", Hex.encodeHexString(authData.getFlags()));
//        byte flags = authData.getFlags()[0];
//        if (!(isUserVerified(flags) || isUserPresent(flags) || true)) {
//            throw new Fido2RPRuntimeException("User preferred is not present");
//        }
    }

    public void verifyDiscouragedUserPresent(AuthData authData) {
        log.debug("Discouraged user present {}", Hex.encodeHexString(authData.getFlags()));
//        byte flags = authData.getFlags()[0];
//        if (isUserPresent(flags) && isUserVerified(flags)) {
//            throw new Fido2RPRuntimeException("User discouraged is not present");
//        }
    }

    private boolean isUserVerified(byte flags) {
        boolean uv = (flags & FLAG_USER_VERIFIED) != 0;
        log.debug("UV = {}", uv);
        return uv;

    }

    private boolean isUserPresent(byte flags) {
        boolean up = (flags & FLAG_USER_PRESENT) != 0;
        log.debug("UP = {}", up);
        return up;
    }

    public void verifyThatMetadataIsValid(JsonNode metadata) {
        long count = Arrays.asList(metadata.hasNonNull("aaguid"), metadata.hasNonNull("assertionScheme"), metadata.hasNonNull("attestationTypes"),
                metadata.hasNonNull("description")).parallelStream().filter(f -> f == false).count();
        if (count != 0) {
            throw new Fido2RPRuntimeException("Invalid parameters in metadata");
        }
    }
}
