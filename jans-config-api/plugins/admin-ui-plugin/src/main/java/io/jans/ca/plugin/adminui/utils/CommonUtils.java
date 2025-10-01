package io.jans.ca.plugin.adminui.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {
    @Inject
    Logger log;
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
}