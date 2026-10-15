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
import io.jans.trace.TraceException.JwkException;
import io.jans.trace.TraceException.PrivateKeyException;
import io.jans.trace.TraceException.PublicKeyException;

public class PublicKeyTest {

    private static JsonBody jwk(String json) {
        return new JsonBody(json);
    }

    @Test
    public void fromJwkReadsRfc8037Key() throws JwkException {
        assertEquals(PublicKey.fromJwk(jwk(Rfc8032.JWK)).toBytes(), Rfc8032.PUBLIC,
                "x should decode to the RFC 8032 public key");
    }

    @Test
    public void fromJwkIgnoresOtherMembers() throws JwkException {
        String withExtras = "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + Rfc8032.X
                + "\",\"kid\":\"key-2026-01\",\"use\":\"sig\",\"alg\":\"EdDSA\"}";
        assertEquals(PublicKey.fromJwk(jwk(withExtras)), PublicKey.fromJwk(jwk(Rfc8032.JWK)),
                "kid, use and alg do not change the key");
    }

    @Test
    public void thumbprintMatchesRfc8037() throws JwkException {
        assertEquals(PublicKey.fromJwk(jwk(Rfc8032.JWK)).thumbprint().toString(), Rfc8032.THUMBPRINT,
                "RFC 7638 thumbprint in sha256:<hex> form");
    }

    @Test
    public void fromBytesRoundTrips() throws PublicKeyException, JwkException {
        PublicKey key = PublicKey.fromBytes(Rfc8032.PUBLIC);
        assertEquals(key, PublicKey.fromJwk(jwk(Rfc8032.JWK)), "bytes and JWK should give the same key");
        assertEquals(key.hashCode(), PublicKey.fromJwk(jwk(Rfc8032.JWK)).hashCode(),
                "equal keys should have equal hash codes");
    }

    @Test
    public void differentKeysAreNotEqual() throws PublicKeyException, PrivateKeyException {
        byte[] other = Ed25519Signer.fromSecretBytes("k", new byte[32]).publicKeyBytes();
        assertNotEquals(PublicKey.fromBytes(other), PublicKey.fromBytes(Rfc8032.PUBLIC),
                "different bytes should be different keys");
    }

    private static void assertJwkRejected(String json, ErrorCode code) {
        JwkException e = expectThrows(JwkException.class, () -> PublicKey.fromJwk(jwk(json)));
        assertEquals(e.code(), code, json + " should be rejected as " + code);
    }

    @Test
    public void otherKeyTypeIsRejected() {
        assertJwkRejected("{\"kty\":\"RSA\",\"crv\":\"Ed25519\",\"x\":\"" + Rfc8032.X + "\"}",
                ErrorCode.INVALID_PUBLIC_KEY);
    }

    @Test
    public void otherCurveIsRejected() {
        assertJwkRejected("{\"kty\":\"OKP\",\"crv\":\"X25519\",\"x\":\"" + Rfc8032.X + "\"}",
                ErrorCode.INVALID_PUBLIC_KEY);
    }

    @Test
    public void privateJwkIsRejected() {
        assertJwkRejected("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + Rfc8032.X
                + "\",\"d\":\"nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A\"}", ErrorCode.INVALID_PUBLIC_KEY);
    }

    @Test
    public void paddedXIsRejected() {
        assertJwkRejected("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + Rfc8032.X + "=\"}",
                ErrorCode.INVALID_PUBLIC_KEY);
    }

    @Test
    public void smallOrderKeyIsRejected() {
        // The identity point: every signature check under it would fail.
        assertJwkRejected("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}",
                ErrorCode.INVALID_PUBLIC_KEY);
    }

    @Test
    public void brokenJsonIsInvalidJson() {
        assertJwkRejected("{\"kty\":", ErrorCode.INVALID_JSON);
    }

    @Test
    public void duplicateMemberIsRejected() {
        assertJwkRejected("{\"kty\":\"OKP\",\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + Rfc8032.X + "\"}",
                ErrorCode.DUPLICATE_MEMBER);
    }

    @Test
    public void wrongLengthBytesAreRejected() {
        PublicKeyException e = expectThrows(PublicKeyException.class, () -> PublicKey.fromBytes(new byte[31]));
        assertEquals(e.code(), ErrorCode.INVALID_PUBLIC_KEY, "an Ed25519 public key has 32 bytes");
    }

    @Test
    public void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> PublicKey.fromJwk(null));
        assertThrows(NullPointerException.class, () -> PublicKey.fromBytes(null));
    }
}
