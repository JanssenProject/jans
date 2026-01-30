package io.jans.ca.plugin.adminui.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommonUtils {
    @Inject
    Logger log;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final DateTimeFormatter LS_DATE_FORMAT = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);


    public static String joinAndUrlEncode(Collection<String> list) throws UnsupportedEncodingException {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return encode(Joiner.on(" ").join(list));
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    public static String getFormattedDate() {
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        ZonedDateTime gmtTime = currentDateTime.withZoneSameInstant(ZoneId.of("GMT"));
        DateTimeFormatter currentTimeFormatter = LS_DATE_FORMAT;
        return gmtTime.format(currentTimeFormatter);
    }

    public static RSAPrivateKey loadPrivateKey(String privateKeyPEM) throws Exception {

        byte[] encoded = Base64.decodeBase64(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public static String decode(String toDecode) throws UnsupportedEncodingException {

        return java.net.URLDecoder.decode(toDecode, StandardCharsets.UTF_8.name());
    }

    public static GenericResponse createGenericResponse(boolean result, int responseCode, String responseMessage) {
        return createGenericResponse(result, responseCode, responseMessage, null);
    }

    public static GenericResponse createGenericResponse(boolean result, int responseCode, String responseMessage, JsonNode node) {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setResponseCode(responseCode);
        genericResponse.setResponseMessage(responseMessage);
        genericResponse.setSuccess(result);
        genericResponse.setResponseObject(node);
        return genericResponse;
    }

    public static boolean hasShortCode(Map<String, ?> map) {
        // Use regular expression to match placeholders in keys like "${placeholder}"
        String pattern = "\\$\\{(\\w+)}";
        Pattern placeholderPattern = Pattern.compile(pattern);

        // Iterate through map keys and check for placeholders
        for (Object value : map.values()) {
            Matcher matcher = placeholderPattern.matcher(value.toString());
            if (matcher.find()) {
                // Placeholder found in key
                return true;
            }
        }

        // No placeholders found in any key
        return false;
    }

    public static boolean hasShortCode(String input) {
        // Use regular expression to match placeholders like "${placeholder}"
        String pattern = "\\$\\{(\\w+)}";
        Pattern placeholderPattern = Pattern.compile(pattern);

        // Create a Matcher and check for placeholders
        Matcher matcher = placeholderPattern.matcher(input);
        return matcher.find();
    }

    public static Map<String, Object> replacePlaceholders(Map<String, Object> map, Map<String, Object> placeholderValues) {
        final Pattern placeholderPattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Map<String, Object> replacedMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = entry.getValue().toString();
            String replacedValue = value;

            Matcher matcher = placeholderPattern.matcher(value);
            while (matcher.find()) {
                String placeholderKey = matcher.group(1);
                Object replacement = placeholderValues.get(placeholderKey); // Get replacement from placeholder values map
                if (replacement != null) {
                    replacedValue = replacedValue.replace(matcher.group(0), replacement.toString());
                }
            }

            replacedMap.put(entry.getKey(), replacedValue);
        }

        return replacedMap;
    }

    public static String replacePlaceholders(String url, Map<String, Object> placeholderValues) {
        final Pattern placeholderPattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = placeholderPattern.matcher(url);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            Object replacement = placeholderValues.get(placeholderKey);
            if (replacement != null) {
                matcher.appendReplacement(sb, replacement.toString()); // Efficient replacement using appendReplacement
            }
        }

        matcher.appendTail(sb); // Append remaining string
        return sb.toString();
    }

    public static Map<String, Object> jsonStringToMap(String jsonString) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, Map.class);
    }

    public static String mapToJsonString(Map<String, Object> mapObj) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(mapObj);
    }

    public static boolean isValidJson(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    // Method to convert ISO datetime to formatted string (yyyy-MM-dd)
    public static String convertIsoToDateString(String isoDateTime) {
        Instant instant = Instant.parse(isoDateTime);
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // Method to convert formatted string (yyyy-MM-dd) back to LocalDate
    public static LocalDate convertStringToLocalDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(dateString, formatter);
    }

    public static String convertLocalDateToString(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(formatter);
    }

    /**
     * Converts any Java object into a JsonNode representation.
     *
     * @param object the Java object to convert
     * @return JSON representation of the object
     */
    public static JsonNode toJsonNode(Object object) throws JsonProcessingException {
        return objectMapper.valueToTree(object);
    }

    /**
     * Convert a Boolean reference to its primitive form, using a default value when the input is null.
     *
     * @return `true` if the input is `Boolean.TRUE`, `false` if the input is `Boolean.FALSE` or `null`.
     */
    public static boolean toPrimitiveOrDefaultFalse(Boolean booleanObject) {
        return booleanObject != null ? booleanObject : false;
    }

    /**
     * Return a new Date representing the given date plus the specified number of minutes.
     *
     * @param date         the base date to adjust
     * @param minutesToAdd the number of minutes to add; may be negative to subtract minutes
     * @return the adjusted Date shifted by {@code minutesToAdd} minutes from {@code date}
     */
    public static Date addMinutes(Date date, int minutesToAdd) {
        if (date == null) {
            return null;
        }
        // Convert the Date to an Instant
        Instant instant = date.toInstant();

        // Add the minutes using ChronoUnit
        Instant updatedInstant = instant.plus(minutesToAdd, ChronoUnit.MINUTES);

        // Convert the Instant back to a Date
        return Date.from(updatedInstant);
    }

    /**
     * Extracts claims from a Jwt into a Map keyed by claim name.
     *
     * <p>The returned map contains claim values using appropriate Java types:
     * Strings, Integer, Long, Boolean, List<String> for JSON arrays, or JSONObject for JSON objects.</p>
     *
     * @param jwtObj the Jwt to extract claims from; may be null
     * @return a map of claim names to claim values, empty if {@code jwtObj} is null
     */
    public static Map<String, Object> getClaims(Jwt jwtObj) {
        Map<String, Object> claims = Maps.newHashMap();
        if (jwtObj == null) {
            return claims;
        }
        JwtClaims jwtClaims = jwtObj.getClaims();
        Set<String> keys = jwtClaims.keys();
        keys.forEach(key -> {

            Object claimValue = jwtClaims.getClaim(key);

            if (claimValue instanceof String) {
                claims.put(key, claimValue);
            } else if (claimValue instanceof Integer) {
                claims.put(key, claimValue);
            } else if (claimValue instanceof Long) {
                claims.put(key, claimValue);
            } else if (claimValue instanceof Boolean) {
                claims.put(key, claimValue);
            } else if (claimValue instanceof JSONArray) {
                List<String> sourceArr = jwtClaims.getClaimAsStringList(key);
                claims.put(key, sourceArr);
            } else if (claimValue instanceof JSONObject) {
                claims.put(key, claimValue);
            }
        });
        return claims;
    }

    /**
     * Builds a UTF-8 URL-encoded query string from the provided parameters.
     *
     * @param params a map of parameter names to values; entries with a null value are omitted
     * @return a URL-encoded string of `key=value` pairs joined with `&`, encoded using UTF-8
     */
    public static String toUrlEncodedString(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(e -> e.getValue() !=null)
                .map(e ->
                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                                URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                )
                .collect(Collectors.joining("&"));
    }
}