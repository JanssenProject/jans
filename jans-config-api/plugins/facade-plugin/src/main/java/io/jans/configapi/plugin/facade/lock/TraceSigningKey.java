/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.lock;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ed25519 key pair that signs TRACE records. Uses the JDK EdDSA provider (JDK 15 and later). */
public final class TraceSigningKey {

    private static final String ALGORITHM = "Ed25519";
    private static final int SPKI_LENGTH = 44;
    private static final int RAW_KEY_LENGTH = 32;
    private static final Base64.Encoder BASE64URL_NO_PAD = Base64.getUrlEncoder().withoutPadding();

    private final KeyPair keyPair;

    private TraceSigningKey(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public static TraceSigningKey generate() {
        try {
            return new TraceSigningKey(KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate Ed25519 key pair", ex);
        }
    }

    /** @return the private key as unencrypted PKCS#8 PEM, the format {@code CEDARLING_TRACE_SIGNING_KEY} reads */
    public String privateKeyPem() {
        String body = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    }

    /** @return the RFC 8037 public JWK: kty, crv and the raw 32-byte key as base64url */
    public Map<String, String> publicJwk() {
        Map<String, String> jwk = new LinkedHashMap<>();
        jwk.put("kty", "OKP");
        jwk.put("crv", ALGORITHM);
        jwk.put("x", BASE64URL_NO_PAD.encodeToString(rawPublicKey(keyPair.getPublic())));
        return jwk;
    }

    private static byte[] rawPublicKey(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        if (encoded == null || encoded.length != SPKI_LENGTH) {
            throw new IllegalStateException("Unexpected Ed25519 SubjectPublicKeyInfo length: "
                    + (encoded == null ? "null" : encoded.length));
        }
        byte[] raw = new byte[RAW_KEY_LENGTH];
        System.arraycopy(encoded, SPKI_LENGTH - RAW_KEY_LENGTH, raw, 0, RAW_KEY_LENGTH);
        return raw;
    }
}
