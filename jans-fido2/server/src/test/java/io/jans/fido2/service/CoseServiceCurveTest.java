/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
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
 * Covers EC2 key decoding across the curves in the FIDO Server Requirements v2.3 table. P-256 is Required,
 * P-384 Recommended and P-521 Optional, but the decoder mapped only P-256, so a credential on any other
 * curve failed registration regardless of the algorithm it negotiated.
 */
@ExtendWith(MockitoExtension.class)
class CoseServiceCurveTest {

    private static final int COSE_KTY_EC2 = 2;
    private static final int COSE_ALG_ES256 = -7;
    private static final int COSE_CURVE_P256 = 1;
    private static final int COSE_CURVE_P384 = 2;
    private static final int COSE_CURVE_P521 = 3;

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

    private static KeyPair generateEcKeyPair(String curveName) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC",
                SecurityProviderUtility.getBCProvider());
        keyPairGenerator.initialize(new ECGenParameterSpec(curveName));

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * COSE carries the affine coordinates zero-padded to the curve's field size, which is what
     * {@code convertUncompressedPointToECKey} expects to find on either side of the point indicator.
     */
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

    private ObjectNode coseKey(int curve, ECPublicKey publicKey) {
        int keySizeBytes = (publicKey.getParams().getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

        ObjectNode coseKeyNode = mapper.createObjectNode();
        coseKeyNode.put("1", COSE_KTY_EC2);
        coseKeyNode.put("3", COSE_ALG_ES256);
        coseKeyNode.put("-1", curve);
        coseKeyNode.put("-2", toFixedLength(publicKey.getW().getAffineX(), keySizeBytes));
        coseKeyNode.put("-3", toFixedLength(publicKey.getW().getAffineY(), keySizeBytes));

        return coseKeyNode;
    }

    private void assertRoundTrips(String curveName, int coseCurve) throws Exception {
        KeyPair keyPair = generateEcKeyPair(curveName);
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        PublicKey decoded = coseService.createUncompressedPointFromCOSEPublicKey(coseKey(coseCurve, publicKey));

        assertArrayEquals(publicKey.getEncoded(), decoded.getEncoded());
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withP256Key_rebuildsTheSameKey() throws Exception {
        assertRoundTrips("secp256r1", COSE_CURVE_P256);
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withP384Key_rebuildsTheSameKey() throws Exception {
        assertRoundTrips("secp384r1", COSE_CURVE_P384);
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withP521Key_rebuildsTheSameKey() throws Exception {
        assertRoundTrips("secp521r1", COSE_CURVE_P521);
    }

    @Test
    void createUncompressedPointFromCOSEPublicKey_withUnknownCurve_reportsTheReceivedCurve() throws Exception {
        KeyPair keyPair = generateEcKeyPair("secp256r1");
        ObjectNode coseKeyNode = coseKey(99, (ECPublicKey) keyPair.getPublic());

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                () -> coseService.createUncompressedPointFromCOSEPublicKey(coseKeyNode));

        assertTrue(ex.getMessage().contains("99"));
    }
}
