/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.verifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.Certificate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class AuthenticatorDataVerifier {

    @Inject
    private Logger log;

    @Inject
    private Base64Service base64Service;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private SignatureVerifier signatureVerifier;

    public void verifyPackedAttestationSignature(AuthData authData, byte[] clientDataHash, String signature, Certificate certificate,
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

        signatureVerifier.verifySignature(signatureBytes, signatureBase, certificate, signatureAlgorithm);
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

        signatureVerifier.verifySignature(signatureBytes, signatureBase, key, signatureAlgorithm);
    }

    public void verifyPackedAttestationSignature(byte[] authData, byte[] clientDataHash, String signature, Certificate certificate,
            int signatureAlgorithm) {
        verifyPackedAttestationSignature(authData, clientDataHash, signature, certificate.getPublicKey(), signatureAlgorithm);
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
        signatureVerifier.verifySignature(signatureBytes, signatureBase, publicKey, signatureAlgorithm);
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

        signatureVerifier.verifySignature(signatureBytes, signatureBase, publicKey, signatureAlgorithm);
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
            throw new Fido2RuntimeException("Can't parse public key");
        }
    }

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

        signatureVerifier.verifySignature(signatureBytes, signatureBase, certificate, signatureAlgorithm);
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

        signatureVerifier.verifySignature(signatureBytes, signatureBase, certificate, signatureAlgorithm);
    }

}
