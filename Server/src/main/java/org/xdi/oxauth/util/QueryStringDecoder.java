package org.xdi.oxauth.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides functionality to parse query strings.
 *
 * @author Javier Rojas Blum Date: 07.31.2012
 */
public class QueryStringDecoder {

    /**
     * Decodes a query string and returns a map with the parsed query string
     * parameters as keys and its values.
     *
     * @param queryString The query string.
     * @return A map with the parsed query string parameters and its values.
     */
    public static Map<String, String> decode(String queryString) {
        String[] params = queryString.split("&");
        Map<String, String> map = new HashMap<String, String>();

        for (String param : params) {
            String[] nameValue = param.split("=");
            String name = nameValue.length > 0 ? nameValue[0] : "";
            String value = nameValue.length > 1 ? nameValue[1] : "";
            map.put(name, value);
        }

        return map;
    }
}