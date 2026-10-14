// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;
import io.jans.trace.uniffi.ErrorCode;

public class DigestTest {

    private static final String TEXT =
            "sha256:2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae";

    @Test
    public void parseKeepsTextForm() throws ParseDigestException {
        assertEquals(Digest.parse(TEXT).toString(), TEXT, "toString should give the TRACE text form back");
    }

    @Test
    public void parseRejectsUppercaseHex() {
        String upper = "sha256:" + TEXT.substring("sha256:".length()).toUpperCase();
        ParseDigestException e = expectThrows(ParseDigestException.class, () -> Digest.parse(upper));
        assertEquals(e.code(), ErrorCode.INVALID_DIGEST, "uppercase hex is not the strict text form");
    }

    @Test
    public void parseRejectsMissingPrefix() {
        ParseDigestException e = expectThrows(ParseDigestException.class,
                () -> Digest.parse(TEXT.substring("sha256:".length())));
        assertEquals(e.code(), ErrorCode.INVALID_DIGEST, "a digest without the sha256: prefix is invalid");
    }

    @Test
    public void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Digest.parse(null));
    }

    @Test
    public void genesisIsAllZeroSentinel() {
        assertEquals(Digest.genesis().toBytes(), new byte[32], "genesis should be 32 zero bytes");
        assertEquals(Digest.genesis().toString(), "sha256:" + "0".repeat(64),
                "genesis text should be the all-zero sentinel");
    }

    @Test
    public void toBytesReturnsRawHash() throws ParseDigestException {
        byte[] bytes = Digest.parse(TEXT).toBytes();
        assertEquals(bytes.length, 32, "SHA-256 has 32 bytes");
        assertEquals(bytes[0], (byte) 0x2c, "bytes should follow the hex text");
    }

    @Test
    public void equalDigestsAreEqualAndHashAlike() throws ParseDigestException {
        Digest a = Digest.parse(TEXT);
        Digest b = Digest.parse(TEXT);
        assertEquals(a, b, "the same text should give equal digests");
        assertEquals(a.hashCode(), b.hashCode(), "equal digests should have equal hash codes");
        assertNotEquals(a, Digest.genesis(), "different hashes should not be equal");
    }
}
