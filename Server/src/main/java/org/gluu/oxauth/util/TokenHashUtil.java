/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

public class TokenHashUtil {

    public static final String PREFIX = "{sha256Hex}";

    public static String getHashedToken(String token) {
        if (StringUtils.isNotBlank(token) && !token.startsWith(PREFIX)) {
            return PREFIX + DigestUtils.sha256Hex(token);
        } else {
            return token;
        }
    }
}