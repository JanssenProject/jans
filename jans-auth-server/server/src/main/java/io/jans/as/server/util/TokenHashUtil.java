/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

public class TokenHashUtil {

    public static final String PREFIX = "{sha256Hex}";

    public static String getHashWithPrefix(String token) {
        if (StringUtils.isNotBlank(token) && !token.startsWith(PREFIX)) {
            return PREFIX + DigestUtils.sha256Hex(token);
        } else {
            return token;
        }
    }

    public static String hash(String hashedToken) {
        if (StringUtils.isNotBlank(hashedToken) && hashedToken.startsWith(PREFIX)) {
            return hashedToken;
        } else {
            return DigestUtils.sha256Hex(hashedToken);
        }
    }

}