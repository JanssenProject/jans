// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;

public class SignedAssertionTest {

    private static VerificationContext context() throws PublicKeyException {
        return new VerificationContext(Records.PRODUCER, Records.KID, Records.publicKey());
    }

    @Test
    public void parseKeepsTheReceivedText() throws TraceException {
        String pretty = Records.SIGNED.replace(",", ",\n  ");
        SignedAssertion signed = SignedAssertion.parse(new JsonBody(pretty), new Limits());
        assertEquals(signed.text(), pretty, "Lock stores the text exactly as received");
        assertEquals(signed.contentDigest().toString(), Records.CONTENT_DIGEST,
                "content_digest is over JCS, so formatting does not change it");
        assertEquals(signed.signingInput(), Records.SIGNING_INPUT, "signing input is JCS without signature");
        assertEquals(signed.signature().toString(), Records.SIGNATURE, "signature is read");
        assertEquals(signed.identity().producer(), Records.PRODUCER, "identity is read");
    }

    @Test
    public void verifyAcceptsTheRegisteredKey() throws TraceException {
        VerifiedAssertion verified = Records.signed().verify(context());
        assertEquals(verified.identity().recordId(), Records.RECORD_ID, "identity is carried");
        assertEquals(verified.contentDigest().toString(), Records.CONTENT_DIGEST, "content_digest is carried");
        assertEquals(verified.signatureInputSha256().toString(), Records.SIGNING_INPUT_SHA256,
                "SHA-256 of the signed bytes");
    }

    @Test
    public void producedRecordVerifies() throws TraceException {
        SignedAssertion signed = UnsignedAssertion.parse(new JsonBody(Records.UNSIGNED), new Limits())
                .sign(Records.signer());
        SignedAssertion received = SignedAssertion.parse(new JsonBody(signed.text()), new Limits());
        assertEquals(received.verify(context()).contentDigest(), signed.contentDigest(),
                "producer and Lock should agree on content_digest");
    }

    private static void assertVerifyRejected(SignedAssertion signed, VerificationContext context, ErrorCode code) {
        VerifyException e = expectThrows(VerifyException.class, () -> signed.verify(context));
        assertEquals(e.code(), code, "verify should reject with " + code);
    }

    @Test
    public void otherProducerIsRejected() throws TraceException {
        assertVerifyRejected(Records.signed(),
                new VerificationContext("other-producer", Records.KID, Records.publicKey()),
                ErrorCode.PRODUCER_MISMATCH);
    }

    @Test
    public void otherKidIsRejected() throws TraceException {
        assertVerifyRejected(Records.signed(),
                new VerificationContext(Records.PRODUCER, "key-other", Records.publicKey()),
                ErrorCode.KID_MISMATCH);
    }

    @Test
    public void otherKeyIsRejected() throws TraceException {
        PublicKey other = PublicKey.fromBytes(Ed25519Signer.fromSecretBytes("k", new byte[32]).publicKeyBytes());
        assertVerifyRejected(Records.signed(), new VerificationContext(Records.PRODUCER, Records.KID, other),
                ErrorCode.SIGNATURE_INVALID);
    }

    @Test
    public void tamperedRecordIsRejected() throws TraceException {
        SignedAssertion tampered = SignedAssertion.parse(
                Records.signedWith("AUTHORIZATION_DECISION", "CAPABILITY_INVOKED"), new Limits());
        assertVerifyRejected(tampered, context(), ErrorCode.SIGNATURE_INVALID);
    }

    private static void assertParseRejected(JsonBody json, ErrorCode code) {
        ParseSignedAssertionException e = expectThrows(ParseSignedAssertionException.class,
                () -> SignedAssertion.parse(json, new Limits()));
        assertEquals(e.code(), code, json.value() + " should be rejected as " + code);
    }

    @Test
    public void missingSignatureIsRejected() {
        assertParseRejected(new JsonBody(Records.SIGNING_INPUT), ErrorCode.MISSING_SIGNATURE);
    }

    @Test
    public void paddedSignatureIsRejected() {
        assertParseRejected(Records.signedWith(Records.SIGNATURE, Records.SIGNATURE + "=="),
                ErrorCode.INVALID_SIGNATURE_ENCODING);
    }

    @Test
    public void nonStringSignatureIsRejected() {
        assertParseRejected(Records.signedWith("\"" + Records.SIGNATURE + "\"", "42"),
                ErrorCode.INVALID_SIGNATURE_ENCODING);
    }

    @Test
    public void duplicateKidIsRejected() {
        assertParseRejected(Records.signedWith("{\"kid\":\"key-2026-01\",", "{\"kid\":\"key-2026-01\",\"kid\":\"x\","),
                ErrorCode.DUPLICATE_MEMBER);
    }

    @Test
    public void emptyRecordIdIsInvalidShape() {
        assertParseRejected(Records.signedWith(Records.RECORD_ID, ""), ErrorCode.INVALID_ASSERTION_SHAPE);
    }

    @Test
    public void unsafeIntegerIsNonCanonicalizable() {
        assertParseRejected(Records.signedWith("\"sequence_number\":1", "\"sequence_number\":9007199254740992"),
                ErrorCode.NON_CANONICALIZABLE_JSON);
    }

    @Test
    public void nullArgumentsAreRejected() throws TraceException {
        assertThrows(NullPointerException.class, () -> SignedAssertion.parse(null, new Limits()));
        assertThrows(NullPointerException.class, () -> Records.signed().verify(null));
        assertThrows(NullPointerException.class, () -> new VerificationContext(null, Records.KID, Records.publicKey()));
    }
}
