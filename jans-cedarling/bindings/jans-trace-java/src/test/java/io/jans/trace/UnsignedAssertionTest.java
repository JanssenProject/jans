// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;

public class UnsignedAssertionTest {

    private static UnsignedAssertion parse(String json) throws ParseUnsignedAssertionException {
        return UnsignedAssertion.parse(new JsonBody(json), new Limits());
    }

    @Test
    public void parseReadsIdentity() throws ParseUnsignedAssertionException {
        AssertionIdentity identity = parse(Records.UNSIGNED).identity();
        assertEquals(identity.producer(), Records.PRODUCER, "producer");
        assertEquals(identity.kid(), Records.KID, "kid");
        assertEquals(identity.recordId(), Records.RECORD_ID, "record_id");
    }

    @Test
    public void signingInputIsJcsOfTheRecord() throws ParseUnsignedAssertionException {
        assertEquals(parse(Records.UNSIGNED).signingInput(), Records.SIGNING_INPUT,
                "the signature covers JCS of the record");
    }

    @Test
    public void signMatchesTheIndependentVector() throws TraceException {
        SignedAssertion signed = parse(Records.UNSIGNED).sign(Records.signer());
        assertEquals(signed.text(), Records.SIGNED, "signed text is JCS of the whole record");
        assertEquals(signed.signature().toString(), Records.SIGNATURE, "Ed25519 over the signing input");
        assertEquals(signed.contentDigest().toString(), Records.CONTENT_DIGEST, "SHA-256 of the signed text");
        assertEquals(signed.signingInput(), Records.SIGNING_INPUT, "signing input is kept");
        assertEquals(signed.identity(), parse(Records.UNSIGNED).identity(), "identity is kept");
    }

    @Test
    public void signerWithAnotherKidIsRejected() throws TraceException {
        UnsignedAssertion unsigned = parse(Records.UNSIGNED);
        Ed25519Signer other = Ed25519Signer.fromSecretBytes("key-other", Rfc8032.SECRET);
        SignException e = expectThrows(SignException.class, () -> unsigned.sign(other));
        assertEquals(e.code(), ErrorCode.KID_MISMATCH, "Lock selects the key by the signed kid");
    }

    private static void assertRejected(String json, Limits limits, ErrorCode code) {
        ParseUnsignedAssertionException e = expectThrows(ParseUnsignedAssertionException.class,
                () -> UnsignedAssertion.parse(new JsonBody(json), limits));
        assertEquals(e.code(), code, json + " should be rejected as " + code);
    }

    @Test
    public void signedRecordIsRejected() {
        assertRejected(Records.SIGNED, new Limits(), ErrorCode.SIGNATURE_ALREADY_PRESENT);
    }

    @Test
    public void missingProducerIsInvalidShape() {
        assertRejected("{\"kid\":\"k\",\"record_id\":\"r\"}", new Limits(), ErrorCode.INVALID_ASSERTION_SHAPE);
    }

    @Test
    public void emptyKidIsInvalidShape() {
        assertRejected("{\"producer\":\"p\",\"kid\":\"\",\"record_id\":\"r\"}", new Limits(),
                ErrorCode.INVALID_ASSERTION_SHAPE);
    }

    @Test
    public void arrayIsInvalidShape() {
        assertRejected("[]", new Limits(), ErrorCode.INVALID_ASSERTION_SHAPE);
    }

    @Test
    public void duplicateMemberIsRejected() {
        assertRejected("{\"producer\":\"p\",\"producer\":\"q\",\"kid\":\"k\",\"record_id\":\"r\"}", new Limits(),
                ErrorCode.DUPLICATE_MEMBER);
    }

    @Test
    public void brokenJsonIsInvalidJson() {
        assertRejected("{\"producer\":", new Limits(), ErrorCode.INVALID_JSON);
    }

    @Test
    public void recordThatWouldNotFitOnceSignedIsRejected() {
        // The signature adds about 100 bytes, so a limit at the unsigned size is too small.
        Limits limits = new Limits().withMaxInputBytes(Records.SIGNING_INPUT.length() + 10);
        assertRejected(Records.SIGNING_INPUT, limits, ErrorCode.SIZE_LIMIT_EXCEEDED);
    }

    @Test
    public void nullArgumentsAreRejected() throws ParseUnsignedAssertionException {
        assertThrows(NullPointerException.class, () -> UnsignedAssertion.parse(null, new Limits()));
        assertThrows(NullPointerException.class, () -> UnsignedAssertion.parse(new JsonBody("{}"), null));
        UnsignedAssertion unsigned = parse(Records.UNSIGNED);
        assertThrows(NullPointerException.class, () -> unsigned.sign(null));
    }
}
