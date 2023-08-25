package io.jans.ca.plugin.adminui.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;

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
}