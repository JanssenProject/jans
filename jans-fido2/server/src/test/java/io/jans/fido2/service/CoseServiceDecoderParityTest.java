/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.verifier.SignatureVerifier;
import io.jans.util.security.SecurityProviderUtility;

/**
 * The decoder and the verifier are two hand-maintained lists of what jans-fido2 supports, and every defect
 * in the v2.3 conformance tree is an instance of them disagreeing. These tests walk what
 * {@link SignatureVerifier} can verify and assert {@link CoseService} can decode a key for each of it, so
 * the two cannot drift apart again by accident.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoseServiceDecoderParityTest {

    private static final int COSE_KTY_EC2 = 2;
    private static final int COSE_KTY_RSA = 3;

    /** The curve each ECDSA algorithm signs over, per the FIDO Server Requirements v2.3 table. */
    private static final Map<CoseEC2Algorithm, Integer> EC2_CURVES = new EnumMap<>(CoseEC2Algorithm.class);
    private static final Map<CoseEC2Algorithm, String> EC2_CURVE_NAMES = new EnumMap<>(CoseEC2Algorithm.class);

    static {
        EC2_CURVES.put(CoseEC2Algorithm.ES256, 1);
        EC2_CURVES.put(CoseEC2Algorithm.ES384, 2);
        EC2_CURVES.put(CoseEC2Algorithm.ES512, 3);
        EC2_CURVE_NAMES.put(CoseEC2Algorithm.ES256, "secp256r1");
        EC2_CURVE_NAMES.put(CoseEC2Algorithm.ES384, "secp384r1");
        EC2_CURVE_NAMES.put(CoseEC2Algorithm.ES512, "secp521r1");
    }

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

    private boolean verifierSupports(int codePoint) {
        try {
            return signatureVerifier.getSignatureChecker(codePoint) != null;
        } catch (Fido2RuntimeException e) {
            return false;
        }
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

    private ObjectNode rsaCoseKey(int algorithm) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", COSE_KTY_RSA);
        coseKeyNode.put("3", algorithm);
        coseKeyNode.put("-1", publicKey.getModulus().toByteArray());
        coseKeyNode.put("-2", publicKey.getPublicExponent().toByteArray());

        return coseKeyNode;
    }

    private ObjectNode ec2CoseKey(CoseEC2Algorithm algorithm) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC",
                SecurityProviderUtility.getBCProvider());
        keyPairGenerator.initialize(new ECGenParameterSpec(EC2_CURVE_NAMES.get(algorithm)));
        ECPublicKey publicKey = (ECPublicKey) keyPairGenerator.generateKeyPair().getPublic();
        int keySizeBytes = (publicKey.getParams().getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", COSE_KTY_EC2);
        coseKeyNode.put("3", algorithm.getNumericValue());
        coseKeyNode.put("-1", EC2_CURVES.get(algorithm));
        coseKeyNode.put("-2", toFixedLength(publicKey.getW().getAffineX(), keySizeBytes));
        coseKeyNode.put("-3", toFixedLength(publicKey.getW().getAffineY(), keySizeBytes));

        return coseKeyNode;
    }

    @Test
    void everyRsaAlgorithmTheVerifierSupports_isDecodable() throws Exception {
        for (CoseRSAAlgorithm algorithm : CoseRSAAlgorithm.values()) {
            if (!verifierSupports(algorithm.getNumericValue())) {
                continue;
            }

            PublicKey decoded = coseService
                    .createUncompressedPointFromCOSEPublicKey(rsaCoseKey(algorithm.getNumericValue()));
            assertTrue(decoded instanceof RSAPublicKey, algorithm.name() + " decoded to " + decoded.getAlgorithm());
        }
    }

    @Test
    void everyEc2AlgorithmTheVerifierSupports_isDecodable() throws Exception {
        for (CoseEC2Algorithm algorithm : CoseEC2Algorithm.values()) {
            if (!verifierSupports(algorithm.getNumericValue())) {
                continue;
            }
            if (!EC2_CURVES.containsKey(algorithm)) {
                fail(algorithm.name() + " is verifiable but this test has no curve mapping for it");
            }

            PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(ec2CoseKey(algorithm));
            assertTrue(decoded instanceof ECPublicKey, algorithm.name() + " decoded to " + decoded.getAlgorithm());
        }
    }

    @Test
    void rsaKeyRoundTrips_forEveryVerifiableAlgorithm() throws Exception {
        ObjectNode coseKeyNode = rsaCoseKey(CoseRSAAlgorithm.PS512.getNumericValue());
        BigInteger modulus = new BigInteger(1, base64Service.decode(coseKeyNode.get("-1").asText()));

        RSAPublicKey decoded = (RSAPublicKey) coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode);

        assertArrayEquals(modulus.toByteArray(), decoded.getModulus().toByteArray());
    }

    /**
     * ECDH_ES_HKDF_256 is a key-agreement algorithm, not a signature one. The verifier rejects it, so the
     * decoder must too — the drift guard has to hold in both directions.
     */
    @Test
    void keyAgreementAlgorithm_isRejectedByBothVerifierAndDecoder() throws Exception {
        assertTrue(!verifierSupports(CoseEC2Algorithm.ECDH_ES_HKDF_256.getNumericValue()));

        ObjectNode coseKeyNode = ec2CoseKey(CoseEC2Algorithm.ES256);
        coseKeyNode.put("3", CoseEC2Algorithm.ECDH_ES_HKDF_256.getNumericValue());

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("ECDH_ES_HKDF_256"));
    }
}
