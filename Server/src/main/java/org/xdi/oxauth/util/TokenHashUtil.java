package org.xdi.oxauth.util;

import org.apache.commons.codec.digest.DigestUtils;

public class TokenHashUtil {

    public static String getHashedToken(String token) {
        return "{sha256Hex}"+ DigestUtils.sha256Hex(token);
    }
}
