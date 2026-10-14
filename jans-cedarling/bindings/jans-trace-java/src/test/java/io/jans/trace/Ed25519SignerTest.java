// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;
import io.jans.trace.uniffi.ErrorCode;

public class Ed25519SignerTest {

    @Test
    public void fromPkcs8PemLoadsTheKey() throws PrivateKeyException {
        Ed25519Signer signer = Ed25519Signer.fromPkcs8Pem("key-2026-01", Rfc8032.PEM);
        assertEquals(signer.kid(), "key-2026-01", "the kid should be kept");
        assertEquals(signer.publicKeyBytes(), Rfc8032.PUBLIC, "PEM should give the RFC 8032 key pair");
    }

    @Test
    public void fromSecretBytesLoadsTheKey() throws PrivateKeyException {
        assertEquals(Ed25519Signer.fromSecretBytes("k", Rfc8032.SECRET).publicKeyBytes(), Rfc8032.PUBLIC,
                "the secret should give the RFC 8032 public key");
    }

    @Test
    public void garbagePemIsRejected() {
        PrivateKeyException e = expectThrows(PrivateKeyException.class,
                () -> Ed25519Signer.fromPkcs8Pem("k", "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n"));
        assertEquals(e.code(), ErrorCode.INVALID_PRIVATE_KEY, "a PEM without a PKCS#8 Ed25519 key is invalid");
    }

    @Test
    public void wrongLengthSecretIsRejected() {
        PrivateKeyException e = expectThrows(PrivateKeyException.class,
                () -> Ed25519Signer.fromSecretBytes("k", new byte[31]));
        assertEquals(e.code(), ErrorCode.INVALID_PRIVATE_KEY, "an Ed25519 secret has 32 bytes");
    }

    @Test
    public void toStringDoesNotLeakTheKey() throws PrivateKeyException {
        String text = Ed25519Signer.fromPkcs8Pem("key-2026-01", Rfc8032.PEM).toString();
        assertFalse(text.contains("MC4CAQAw") || text.toLowerCase().contains("9d61b19d"),
                "toString must not contain key material: " + text);
    }

    @Test
    public void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> Ed25519Signer.fromPkcs8Pem(null, Rfc8032.PEM));
        assertThrows(NullPointerException.class, () -> Ed25519Signer.fromPkcs8Pem("k", null));
        assertThrows(NullPointerException.class, () -> Ed25519Signer.fromSecretBytes("k", null));
    }
}
