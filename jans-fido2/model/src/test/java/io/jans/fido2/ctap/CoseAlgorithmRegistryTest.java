/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.ctap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pins every COSE constant to the IANA COSE Algorithms registry
 * (https://www.iana.org/assignments/cose/cose.xhtml#algorithms). These values go out on the wire in
 * pubKeyCredParams, so a wrong one silently advertises a different algorithm than the admin configured.
 */
class CoseAlgorithmRegistryTest {

    @Test
    void ec2Constants_matchTheIanaRegistry() {
        Map<CoseEC2Algorithm, Integer> expected = new HashMap<>();
        expected.put(CoseEC2Algorithm.ES256, -7);
        expected.put(CoseEC2Algorithm.ES384, -35);
        expected.put(CoseEC2Algorithm.ES512, -36);
        expected.put(CoseEC2Algorithm.ECDH_ES_HKDF_256, -25);

        assertEquals(expected.size(), CoseEC2Algorithm.values().length, "unexpected EC2 constant added or removed");
        expected.forEach((algorithm, value) -> assertEquals(value.intValue(), algorithm.getNumericValue(),
                algorithm.name() + " code point"));
    }

    @Test
    void rsaConstants_matchTheIanaRegistry() {
        Map<CoseRSAAlgorithm, Integer> expected = new HashMap<>();
        expected.put(CoseRSAAlgorithm.RS256, -257);
        expected.put(CoseRSAAlgorithm.RS384, -258);
        expected.put(CoseRSAAlgorithm.RS512, -259);
        expected.put(CoseRSAAlgorithm.RS65535, -65535);
        expected.put(CoseRSAAlgorithm.PS256, -37);
        expected.put(CoseRSAAlgorithm.PS384, -38);
        expected.put(CoseRSAAlgorithm.PS512, -39);

        assertEquals(expected.size(), CoseRSAAlgorithm.values().length, "unexpected RSA constant added or removed");
        expected.forEach((algorithm, value) -> assertEquals(value.intValue(), algorithm.getNumericValue(),
                algorithm.name() + " code point"));
    }

    @Test
    void edDsaConstants_matchTheIanaRegistry() {
        assertEquals(1, CoseEdDSAAlgorithm.values().length, "unexpected EdDSA constant added or removed");
        assertEquals(-8, CoseEdDSAAlgorithm.EdDSA.getNumericValue());
    }

    /**
     * -260, -261 and -262 are WalnutDSA, TurboSHAKE128 and TurboSHAKE256 in the registry. They were held by
     * ED256, ED512 and RS1 respectively; RS1's real value is -65535, already covered by RS65535.
     */
    @Test
    void codePointsOfUnrelatedRegistryEntries_areNotClaimed() {
        assertNull(CoseEC2Algorithm.fromNumericValue(-260));
        assertNull(CoseEC2Algorithm.fromNumericValue(-261));
        assertNull(CoseRSAAlgorithm.fromNumericValue(-262));
        assertEquals(CoseRSAAlgorithm.RS65535, CoseRSAAlgorithm.fromNumericValue(-65535));
    }

    @Test
    void noTwoConstants_shareACodePoint() {
        Map<Integer, String> seen = new HashMap<>();
        for (CoseEC2Algorithm algorithm : CoseEC2Algorithm.values()) {
            seen.put(algorithm.getNumericValue(), algorithm.name());
        }
        for (CoseRSAAlgorithm algorithm : CoseRSAAlgorithm.values()) {
            String clash = seen.put(algorithm.getNumericValue(), algorithm.name());
            assertNull(clash, algorithm.name() + " shares a code point with " + clash);
        }
        for (CoseEdDSAAlgorithm algorithm : CoseEdDSAAlgorithm.values()) {
            String clash = seen.put(algorithm.getNumericValue(), algorithm.name());
            assertNull(clash, algorithm.name() + " shares a code point with " + clash);
        }
    }
}
