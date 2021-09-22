/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

import org.apache.commons.codec.digest.DigestUtils;

import io.jans.as.model.util.Base64Util;

/**
 * <pre>
 * struct {
 *    TokenBindingKeyParameters key_parameters;
 *    uint16 key_length;       Length (in bytes) of the following TokenBindingID.TokenBindingPublicKey
 *    select (key_parameters) {
 *       case rsa2048_pkcs1.5:
 *       case rsa2048_pss:
 *          RSAPublicKey rsapubkey;
 *       case ecdsap256:
 *          TB_ECPoint point;
 *    } TokenBindingPublicKey;
 * } TokenBindingID;
 * </pre>
 *
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingID {

    private final TokenBindingKeyParameters keyParameters;
    private final byte[] publicKey;
    private final byte[] raw;

    public TokenBindingID(TokenBindingKeyParameters keyParameters, byte[] publicKey, byte[] raw) {
        this.keyParameters = keyParameters;
        this.publicKey = publicKey;
        this.raw = raw;
    }

    public TokenBindingKeyParameters getKeyParameters() {
        return keyParameters;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getRaw() {
        return raw;
    }

    public byte[] sha256() {
        return DigestUtils.sha256(raw);
    }

    public String sha256base64url() {
        return Base64Util.base64urlencode(sha256());
    }
}
