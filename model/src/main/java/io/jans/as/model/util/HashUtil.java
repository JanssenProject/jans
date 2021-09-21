/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.log4j.Logger;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;

/**
 * @author Yuriy Zabrovarnyy
 */
public class HashUtil {

    private static final Logger log = Logger.getLogger(HashUtil.class);

    private HashUtil() {
    }

    public static String getHash(String input, SignatureAlgorithm signatureAlgorithm) {
        try {
            final byte[] digest;
            if (signatureAlgorithm == SignatureAlgorithm.HS256 ||
                    signatureAlgorithm == SignatureAlgorithm.RS256 ||
                    signatureAlgorithm == SignatureAlgorithm.PS256 ||
                    signatureAlgorithm == SignatureAlgorithm.ES256) {
                digest = JwtUtil.getMessageDigestSHA256(input);
            } else if (signatureAlgorithm == SignatureAlgorithm.HS384 ||
                    signatureAlgorithm == SignatureAlgorithm.RS384 ||
                    signatureAlgorithm == SignatureAlgorithm.PS384 ||
                    signatureAlgorithm == SignatureAlgorithm.ES384) {
                digest = JwtUtil.getMessageDigestSHA384(input);
            } else if (signatureAlgorithm == SignatureAlgorithm.HS512 ||
                    signatureAlgorithm == SignatureAlgorithm.RS512 ||
                    signatureAlgorithm == SignatureAlgorithm.PS512 ||
                    signatureAlgorithm == SignatureAlgorithm.ES512) {
                digest = JwtUtil.getMessageDigestSHA512(input);
            } else { // Default
                digest = JwtUtil.getMessageDigestSHA256(input);
            }

            if (digest != null) {
                byte[] lefMostHalf = new byte[digest.length / 2];
                System.arraycopy(digest, 0, lefMostHalf, 0, lefMostHalf.length);
                return Base64Util.base64urlencode(lefMostHalf);
            }
        } catch (Exception e) {
            log.error("Failed to calculate hash.", e);
        }

        return null;
    }
}
