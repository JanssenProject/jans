package org.xdi.oxd.license.admin.client;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/11/2014
 */

public class HashParser {

    public static String getIdTokenFromHash(String hash) {
        if (!isEmpty(hash) && hash.contains("id_token=")) {
            int indexOf = hash.indexOf("id_token=");
            indexOf = indexOf + "id_token=".length();

            final int end = hash.indexOf("&", indexOf);
            if (end != -1) {
                return hash.substring(indexOf, end);
            }
            return hash.substring(indexOf);
        }
        return "";
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().equals("");
    }

}
