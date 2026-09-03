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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Covers OKP / Ed25519 key decoding. EdDSA is advertised in pubKeyCredParams by default and is Required in
 * FIDO Server Requirements v2.3, so a credential using it has to survive the whole decode-then-verify path.
 */
@ExtendWith(MockitoExtension.class)
class CoseServiceEdDSATest {

    private static final int COSE_KTY_OKP = 1;
    private static final int COSE_ALG_EDDSA = -8;
    private static final int COSE_CURVE_ED25519 = 6;
    private static final int COSE_CURVE_ED448 = 7;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private Logger log;

    @Spy
    private Base64Service base64Service = initializedBase64Service();

    @InjectMocks
    private CoseService coseService;

    @BeforeAll
    static void beforeAll() {
        SecurityProviderUtility.installBCProvider();
    }

    private static Base64Service initializedBase64Service() {
        Base64Service base64Service = new Base64Service();
        base64Service.init();

        return base64Service;
    }

    private static KeyPair generateEd25519KeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519",
                SecurityProviderUtility.getBCProvider());

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * The raw COSE {@code x} parameter is the trailing 32 bytes of the SubjectPublicKeyInfo encoding.
     */
    private static byte[] rawKeyOf(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();

        return Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);
    }

    private ObjectNode coseKey(int keyType, int algorithm, int curve, byte[] rawKey) {
        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", keyType);
        coseKeyNode.put("3", algorithm);
        coseKeyNode.put("-1", curve);
        coseKeyNode.put("-2", rawKey);

        return coseKeyNode;
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withEd25519Key_rebuildsTheSameKey() throws Exception {
        KeyPair keyPair = generateEd25519KeyPair();

        PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(
                coseKey(COSE_KTY_OKP, COSE_ALG_EDDSA, COSE_CURVE_ED25519, rawKeyOf(keyPair.getPublic())));

        assertEquals("Ed25519", decoded.getAlgorithm());
        assertArrayEquals(keyPair.getPublic().getEncoded(), decoded.getEncoded());
    }

    /**
     * The point of the fix: a signature produced by an Ed25519 authenticator has to verify against the key we
     * rebuild from what it sent us.
     */
    @Test
    void decodedEd25519Key_verifiesSignatureOverAuthenticatorData() throws Exception {
        KeyPair keyPair = generateEd25519KeyPair();
        byte[] signedBytes = "authenticatorData||clientDataHash".getBytes();

        Signature signer = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        signer.initSign(keyPair.getPrivate());
        signer.update(signedBytes);
        byte[] signature = signer.sign();

        PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(
                coseKey(COSE_KTY_OKP, COSE_ALG_EDDSA, COSE_CURVE_ED25519, rawKeyOf(keyPair.getPublic())));

        Signature verifier = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        verifier.initVerify(decoded);
        verifier.update(signedBytes);

        assertTrue(verifier.verify(signature));
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withUnknownAlgorithm_throwsHandledException() throws Exception {
        ObjectNode coseKeyNode = coseKey(COSE_KTY_OKP, -999, COSE_CURVE_ED25519,
                rawKeyOf(generateEd25519KeyPair().getPublic()));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("-999"));
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withEd448Curve_throwsUnsupportedCurve() throws Exception {
        ObjectNode coseKeyNode = coseKey(COSE_KTY_OKP, COSE_ALG_EDDSA, COSE_CURVE_ED448,
                rawKeyOf(generateEd25519KeyPair().getPublic()));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("Unsupported OKP curve"));
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withTruncatedKey_throwsInvalidLength() {
        ObjectNode coseKeyNode = coseKey(COSE_KTY_OKP, COSE_ALG_EDDSA, COSE_CURVE_ED25519, new byte[16]);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("Invalid Ed25519 public key length"));
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withoutCurveLabel_throwsHandledException() throws Exception {
        ObjectNode coseKeyNode = coseKey(COSE_KTY_OKP, COSE_ALG_EDDSA, COSE_CURVE_ED25519,
                rawKeyOf(generateEd25519KeyPair().getPublic()));
        coseKeyNode.remove("-1");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withoutPublicKeyLabel_throwsHandledException() throws Exception {
        ObjectNode coseKeyNode = coseKey(COSE_KTY_OKP, COSE_ALG_EDDSA, COSE_CURVE_ED25519,
                rawKeyOf(generateEd25519KeyPair().getPublic()));
        coseKeyNode.remove("-2");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("-2"));
    }
}
