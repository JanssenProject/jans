/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.log4j.Logger;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;

/**
 * Hash Tool, that calculates Hash Code, using Hashing Algorithm, defined by the Signature Algorithm.
 * I.e. Hashing Algorithm depends on the Signature Algorithm. 
 * 
 * @author Yuriy Zabrovarnyy
 * @author Sergey Manoylo
 * @version December 5, 2021
 */
public class HashUtil {

    private static final Logger log = Logger.getLogger(HashUtil.class);

    /**
     * Constructor.
     */
    private HashUtil() {
    }

    /**
     * Calculates Hash Code, using Hashing Algorithm, defined by used Signature Algorithm.
     * 
     * @param input Input string, whose hash code is being calculated.
     * @param signatureAlgorithm Signature Algorithm.
     * @return Hash Code, using algorithm, defined by used Signature Algorithm.
     */
    public static String getHash(String input, SignatureAlgorithm signatureAlgorithm) {
        try {
            byte[] digest = null;
            if (signatureAlgorithm != null) {
                switch (signatureAlgorithm) {
                    case HS256:
                    case RS256:
                    case PS256:
                    case ES256:
                    case ES256K: {
                        digest = JwtUtil.getMessageDigestSHA256(input);
                        break;
                    }
                    case HS384:
                    case RS384:
                    case PS384:
                    case ES384: {
                        digest = JwtUtil.getMessageDigestSHA384(input);
                        break;
                    }
                    case HS512:
                    case RS512:
                    case PS512:
                    case ES512: 
                    case EDDSA: {
                        digest = JwtUtil.getMessageDigestSHA512(input);
                        break;
                    }
                    default: {
                        digest = JwtUtil.getMessageDigestSHA256(input);
                        break;
                    }
                }
            } else {
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
