/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class Base64Util {

    private static final Logger log = Logger.getLogger(Base64Util.class);

    public static String base64urlencode(byte[] arg) {
        String s = Base64.encodeBase64String(arg); // Standard base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    public static byte[] base64urldecode(String arg) throws IllegalArgumentException {
        String s = removePadding(arg);
        return Base64.decodeBase64(s); // Standard base64 decoder
    }

    public static String removePadding(String base64UrlEncoded) {
		String s = base64UrlEncoded;
        s = s.replace('-', '+'); // 62nd char of encoding
        s = s.replace('_', '/'); // 63rd char of encoding
        switch (s.length() % 4) // Pad with trailing '='s
        {
            case 0:
                break; // No pad chars in this case
            case 2:
                s += "==";
                break; // Two pad chars
            case 3:
                s += "=";
                break; // One pad char
            default:
                throw new IllegalArgumentException("Illegal base64url string.");
        }
		return s;
	}

}