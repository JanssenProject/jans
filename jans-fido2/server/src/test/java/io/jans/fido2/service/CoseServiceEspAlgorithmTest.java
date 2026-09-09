/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
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

import io.jans.fido2.service.verifier.SignatureVerifier;
import io.jans.util.security.SecurityProviderUtility;

/**
 * ESP256 and ESP384 are the fully-specified ECDSA algorithms from the FIDO Server Requirements v2.3
 * table: distinct COSE code points that fix the curve, over the same crypto as ES256 and ES384. These
 * tests take a credential from its COSE key through decoding to a verified signature, which is the pair
 * of ceremonies an authenticator offering them would drive.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoseServiceEspAlgorithmTest {

    private static final int COSE_KTY_EC2 = 2;

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

    private static byte[] toFixedLength(BigInteger coordinate, int keySizeBytes) {
        byte[] unpadded = coordinate.toByteArray();
        if (unpadded.length == keySizeBytes) {
            return unpadded;
        }
        if (unpadded.length > keySizeBytes) {
            return Arrays.copyOfRange(unpadded, unpadded.length - keySizeBytes, unpadded.length);
        }

        byte[] padded = new byte[keySizeBytes];
        System.arraycopy(unpadded, 0, padded, keySizeBytes - unpadded.length, unpadded.length);

        return padded;
    }

    @ParameterizedTest(name = "COSE {0} over {1}")
    @CsvSource({ "-9, secp256r1, 1, SHA256withECDSA", "-51, secp384r1, 2, SHA384withECDSA" })
    void espCredential_decodesAndVerifiesItsOwnSignature(int codePoint, String curveName, int coseCurve,
            String signatureAlgorithm) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC",
                SecurityProviderUtility.getBCProvider());
        keyPairGenerator.initialize(new ECGenParameterSpec(curveName));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        int keySizeBytes = (publicKey.getParams().getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", COSE_KTY_EC2);
        coseKeyNode.put("3", codePoint);
        coseKeyNode.put("-1", coseCurve);
        coseKeyNode.put("-2", toFixedLength(publicKey.getW().getAffineX(), keySizeBytes));
        coseKeyNode.put("-3", toFixedLength(publicKey.getW().getAffineY(), keySizeBytes));

        // Registration: the credential public key has to come back out of the COSE structure.
        PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode);
        assertEquals(publicKey.getW(), ((ECPublicKey) decoded).getW());

        // Assertion: a signature the authenticator would produce has to verify against that key.
        byte[] signatureBase = "authenticator data + client data hash".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance(signatureAlgorithm, SecurityProviderUtility.getBCProvider());
        signer.initSign(keyPair.getPrivate());
        signer.update(signatureBase);

        signatureVerifier.verifySignature(signer.sign(), signatureBase, decoded, codePoint);
    }
}
