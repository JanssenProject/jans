// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;
import io.jans.trace.uniffi.ErrorCode;
import io.jans.trace.TraceException.ParseSignatureException;

public class SignatureValueTest {

    @Test
    public void parseKeepsCanonicalText() throws ParseSignatureException {
        SignatureValue signature = SignatureValue.parse(Rfc8032.SIGNATURE);
        assertEquals(signature.toString(), Rfc8032.SIGNATURE, "toString should be the unpadded base64url form");
        assertEquals(signature.toBytes().length, 64, "an Ed25519 signature has 64 bytes");
        assertEquals(signature, SignatureValue.parse(Rfc8032.SIGNATURE), "same text, equal signatures");
    }

    private static void assertRejected(String text) {
        ParseSignatureException e = expectThrows(ParseSignatureException.class, () -> SignatureValue.parse(text));
        assertEquals(e.code(), ErrorCode.INVALID_SIGNATURE_ENCODING, text + " is not a canonical signature");
    }

    @Test
    public void paddedTextIsRejected() {
        assertRejected(Rfc8032.SIGNATURE + "==");
    }

    @Test
    public void standardBase64IsRejected() {
        assertRejected(Rfc8032.SIGNATURE.replace('-', '+'));
    }

    @Test
    public void wrongLengthIsRejected() {
        assertRejected(Rfc8032.SIGNATURE.substring(0, 40));
    }
}
