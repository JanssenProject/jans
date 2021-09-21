/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.commons.codec.binary.Base64;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class Base64Util {

    private Base64Util() {
    }

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

    public static String base64urldecodeToString(String arg) throws IllegalArgumentException {
        byte[] decoded = base64urldecode(arg);
        return new String(decoded, StandardCharsets.UTF_8);
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

    public static String base64urlencodeUnsignedBigInt(BigInteger bigInteger) {
        byte[] array = bigInteger.toByteArray();
        if (array[0] == 0) {
            byte[] tmp = new byte[array.length - 1];
            System.arraycopy(array, 1, tmp, 0, tmp.length);
            array = tmp;
        }

        return Base64Util.base64urlencode(array);
    }

    public static byte[] unsignedToBytes(int[] plaintextUnsignedBytes) {
        byte[] bytes = new byte[plaintextUnsignedBytes.length];

        for (int i = 0; i < plaintextUnsignedBytes.length; i++) {
            bytes[i] = (byte) plaintextUnsignedBytes[i];
        }

        return bytes;
    }
}