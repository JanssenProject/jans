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

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.verifier.SignatureVerifier;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Ed25519 (-19) and Ed448 (-53) are the fully-specified EdDSA algorithms from the FIDO Server
 * Requirements v2.3 table, at code points distinct from EdDSA (-8). Being fully specified, each names its
 * curve in the code point, so a credential that pairs one with a different curve is malformed. The
 * verifier cannot catch that - it resolves both to a plain {@code Signature} instance that never sees the
 * COSE curve - so the decoder is the only place the pairing can be enforced.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoseServiceFullySpecifiedEdDSATest {

    private static final int COSE_KTY_OKP = 1;

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

    /** The raw COSE {@code x} parameter is the trailing raw-key bytes of the SubjectPublicKeyInfo. */
    private static byte[] rawKeyOf(PublicKey publicKey, int rawKeyLength) {
        byte[] encoded = publicKey.getEncoded();

        return Arrays.copyOfRange(encoded, encoded.length - rawKeyLength, encoded.length);
    }

    private ObjectNode coseKey(int algorithm, int curve, byte[] rawKey) {
        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", COSE_KTY_OKP);
        coseKeyNode.put("3", algorithm);
        coseKeyNode.put("-1", curve);
        coseKeyNode.put("-2", rawKey);

        return coseKeyNode;
    }

    @ParameterizedTest(name = "COSE {0} is {1} on curve {2}")
    @CsvSource({ "-19, Ed25519, 6, 32", "-53, Ed448, 7, 57" })
    void credential_decodesAndVerifiesItsOwnSignature(int codePoint, String algorithmName, int coseCurve,
            int rawKeyLength) throws Exception {
        KeyPair keyPair = generateKeyPair(algorithmName);
        ObjectNode coseKeyNode = coseKey(codePoint, coseCurve, rawKeyOf(keyPair.getPublic(), rawKeyLength));

        // Registration: the credential public key has to come back out of the COSE structure intact.
        PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode);
        assertEquals(algorithmName, decoded.getAlgorithm());
        assertArrayEquals(keyPair.getPublic().getEncoded(), decoded.getEncoded());

        // Assertion: a signature the authenticator would produce has to verify against that key.
        byte[] signatureBase = "authenticatorData||clientDataHash".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance(algorithmName, SecurityProviderUtility.getBCProvider());
        signer.initSign(keyPair.getPrivate());
        signer.update(signatureBase);

        signatureVerifier.verifySignature(signer.sign(), signatureBase, decoded, codePoint);
    }

    @ParameterizedTest(name = "COSE {0} rejects a key on curve {2}")
    @CsvSource({ "-19, Ed448, 7, 57", "-53, Ed25519, 6, 32" })
    void credential_onTheWrongCurve_isRejected(int codePoint, String algorithmName, int coseCurve, int rawKeyLength)
            throws Exception {
        PublicKey publicKey = generateKeyPair(algorithmName).getPublic();
        ObjectNode coseKeyNode = coseKey(codePoint, coseCurve, rawKeyOf(publicKey, rawKeyLength));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("requires COSE curve"), ex.getMessage());
    }

    /**
     * EdDSA (-8) is not fully specified, so it carries whichever curve the key uses and must keep working
     * on both. Constraining it alongside the new code points would break existing Ed25519 credentials.
     */
    @ParameterizedTest(name = "EdDSA still accepts curve {1}")
    @CsvSource({ "Ed25519, 6, 32", "Ed448, 7, 57" })
    void unspecifiedEdDSA_acceptsEitherCurve(String algorithmName, int coseCurve, int rawKeyLength) throws Exception {
        PublicKey publicKey = generateKeyPair(algorithmName).getPublic();

        PublicKey decoded = coseService
                .createUncompressedPointFromCOSEPublicKey(coseKey(-8, coseCurve, rawKeyOf(publicKey, rawKeyLength)));

        assertEquals(algorithmName, decoded.getAlgorithm());
    }

    @ParameterizedTest(name = "COSE {0} rejects a truncated key")
    @CsvSource({ "-19, 6, Ed25519", "-53, 7, Ed448" })
    void credential_withTruncatedKey_isRejected(int codePoint, int coseCurve, String algorithmName) {
        ObjectNode coseKeyNode = coseKey(codePoint, coseCurve, new byte[16]);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("Invalid " + algorithmName + " public key length"), ex.getMessage());
    }
}
