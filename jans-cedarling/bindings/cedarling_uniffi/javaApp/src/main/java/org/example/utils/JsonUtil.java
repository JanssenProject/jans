package org.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Converts any object to a pretty-printed JSON string.
     *
     * @param obj the object to serialize
     * @return the JSON string, or an error message if serialization fails
     */
    public static String toPrettyJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "Error converting to JSON: " + e.getMessage();
        }
    }
}
