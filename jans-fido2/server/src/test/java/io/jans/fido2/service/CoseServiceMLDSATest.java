/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.ctap.CoseMLDSAAlgorithm;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.verifier.SignatureVerifier;
import io.jans.util.security.SecurityProviderUtility;

/**
 * ML-DSA is the post-quantum family from the FIDO Server Requirements v2.3 table. Its COSE key type is AKP,
 * which carries the whole public key in one parameter rather than the coordinate pairs EC2 and OKP use, so
 * the decode path is genuinely new rather than a widened list.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoseServiceMLDSATest {

    private static final int COSE_KTY_AKP = 7;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private Logger log;

    @Spy
    private Base64Service base64Service = initializedBase64Service();

    @InjectMocks
    private CoseService coseService;

    @InjectMocks
    private SignatureVerifier signatureVerifier;

    @BeforeAll
    static void beforeAll() {
        SecurityProviderUtility.installBCProvider();
    }

    private static Base64Service initializedBase64Service() {
        Base64Service base64Service = new Base64Service();
        base64Service.init();

        return base64Service;
    }

    private static KeyPair generateKeyPair(String algorithmName) throws Exception {
        return KeyPairGenerator.getInstance(algorithmName, SecurityProviderUtility.getBCProvider())
                .generateKeyPair();
    }

    /** The COSE "pub" parameter is the raw key, which is the tail of the SubjectPublicKeyInfo encoding. */
    private static byte[] rawKeyOf(PublicKey publicKey, int rawKeyLength) {
        byte[] encoded = publicKey.getEncoded();

        return Arrays.copyOfRange(encoded, encoded.length - rawKeyLength, encoded.length);
    }

    private ObjectNode coseKey(int algorithm, byte[] rawKey) {
        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", COSE_KTY_AKP);
        coseKeyNode.put("3", algorithm);
        coseKeyNode.put("-1", rawKey);

        return coseKeyNode;
    }

    @ParameterizedTest(name = "COSE {0} is {1} with a {2}-byte key")
    @CsvSource({ "-48, ML-DSA-44, 1312", "-49, ML-DSA-65, 1952", "-50, ML-DSA-87, 2592" })
    void credential_decodesAndVerifiesItsOwnSignature(int codePoint, String algorithmName, int rawKeyLength)
            throws Exception {
        KeyPair keyPair = generateKeyPair(algorithmName);
        ObjectNode coseKeyNode = coseKey(codePoint, rawKeyOf(keyPair.getPublic(), rawKeyLength));

        // Registration: the credential public key has to come back out of the COSE structure intact.
        PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode);
        assertArrayEquals(keyPair.getPublic().getEncoded(), decoded.getEncoded());

        // Assertion: a signature the authenticator would produce has to verify against that key.
        byte[] signatureBase = "authenticatorData||clientDataHash".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance(algorithmName, SecurityProviderUtility.getBCProvider());
        signer.initSign(keyPair.getPrivate());
        signer.update(signatureBase);

        signatureVerifier.verifySignature(signer.sign(), signatureBase, decoded, codePoint);
    }

    @ParameterizedTest(name = "COSE {0} rejects a key of the wrong length")
    @CsvSource({ "-48, ML-DSA-44", "-49, ML-DSA-65", "-50, ML-DSA-87" })
    void credential_withWrongKeyLength_isRejected(int codePoint, String algorithmName) {
        ObjectNode coseKeyNode = coseKey(codePoint, new byte[64]);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("Invalid " + algorithmName + " public key length"), ex.getMessage());
    }

    /**
     * Each parameter set has its own key length, so a key valid for one must not be accepted under another.
     * The SubjectPublicKeyInfo prefixes carry distinct OIDs and length fields, and this is what catches a
     * prefix pasted against the wrong parameter set.
     */
    @Test
    void credential_withAKeyFromAnotherParameterSet_isRejected() throws Exception {
        byte[] ml65Key = rawKeyOf(generateKeyPair("ML-DSA-65").getPublic(), 1952);
        ObjectNode coseKeyNode = coseKey(CoseMLDSAAlgorithm.ML_DSA_44.getNumericValue(), ml65Key);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("Invalid ML-DSA-44 public key length"), ex.getMessage());
    }

    @Test
    void credential_withoutThePublicKeyLabel_isRejected() {
        ObjectNode coseKeyNode = coseKey(CoseMLDSAAlgorithm.ML_DSA_44.getNumericValue(), new byte[1312]);
        coseKeyNode.remove("-1");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("Missing AKP public key label -1"), ex.getMessage());
    }

    /**
     * This is the safety property that lets ML-DSA ship standard-only. The advertised set is derived from
     * decoder and verifier capability, and the verifier asks the running provider, so a deployment whose
     * provider lacks ML-DSA reports it unsupported and never offers it. Were this to report support without
     * the provider having it, a FIPS deployment would advertise ML-DSA and then fail the ceremony.
     */
    @ParameterizedTest(name = "COSE {0} capability matches the running provider")
    @CsvSource({ "-48, ML-DSA-44", "-49, ML-DSA-65", "-50, ML-DSA-87" })
    void advertisedCapability_followsTheRunningProvider(int codePoint, String algorithmName) {
        boolean providerHasIt;
        try {
            Signature.getInstance(algorithmName, SecurityProviderUtility.getBCProvider());
            providerHasIt = true;
        } catch (Exception e) {
            providerHasIt = false;
        }

        assertEquals(providerHasIt, signatureVerifier.isSupported(codePoint),
                algorithmName + ": advertised capability disagrees with the provider");
        assertTrue(coseService.isDecodable(codePoint), algorithmName + " should be decodable");
    }
}
