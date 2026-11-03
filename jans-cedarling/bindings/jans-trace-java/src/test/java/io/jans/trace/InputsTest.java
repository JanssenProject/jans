// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.testng.annotations.Test;

public class InputsTest {

    /** SHA-256 of {@code abc} (FIPS 180-2, appendix B.1). */
    private static final String SHA256_ABC =
            "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    public void bundleHashIsSha256OfArchiveBytes() {
        assertEquals(Inputs.computeBundleHash("abc".getBytes(StandardCharsets.US_ASCII)).toString(),
                SHA256_ABC, "bundle_hash should be SHA-256 of the raw archive");
    }

    @Test
    public void tokenFingerprintIsSha256OfTheCompactToken() {
        assertEquals(Inputs.tokenFingerprint("eyJhbGciOiJub25lIn0.e30.").toString(),
                "sha256:4badc4f0c891b654107a33eb52b94a3d31ef3bd9dbba322958019ad1a9cc07bb",
                "the same value as the core test vector");
    }

    @Test
    public void tokenFingerprintHashesUtf8OfNonAsciiText() throws NoSuchAlgorithmException {
        String token = "tök€n";
        byte[] expected = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        assertEquals(Inputs.tokenFingerprint(token).toBytes(), expected,
                "a Java String should be hashed as its UTF-8 bytes");
    }
}
