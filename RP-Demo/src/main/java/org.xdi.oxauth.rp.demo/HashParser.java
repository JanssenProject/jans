package org.xdi.oxauth.rp.demo;

/**
 * @author yuriyz on 07/19/2016.
 */
public class HashParser {

    public static String getIdTokenFromHash(String hash) {
        return getParameterFromHash(hash, "id_token");
    }

    public static String getCodeFromHash(String hash) {
        return getParameterFromHash(hash, "code");
    }

    public static String getParameterFromHash(String hash, String parameterName) {
        parameterName = parameterName + "=";
        if (!isEmpty(hash) && hash.contains(parameterName)) {
            int indexOf = hash.indexOf(parameterName);
            indexOf = indexOf + parameterName.length();

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
