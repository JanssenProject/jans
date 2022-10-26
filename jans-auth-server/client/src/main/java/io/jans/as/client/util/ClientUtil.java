/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 26/12/2012
 */

public class ClientUtil {

    private static final Logger log = LoggerFactory.getLogger(ClientUtil.class);

    private ClientUtil() {
    }

    public static String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

    public static List<String> extractListByKey(JSONObject jsonObject, String key) {
        final List<String> result = new ArrayList<>();
        if (!jsonObject.has(key)) {
            return result;
        }

        JSONArray arrayOfValues = jsonObject.optJSONArray(key);
        if (arrayOfValues != null) {
            for (int i = 0; i < arrayOfValues.length(); i++) {
                final String v = arrayOfValues.optString(i);
                if (StringUtils.isNotBlank(v)) {
                    result.add(v);
                }
            }
            return result;
        }
        String listString = jsonObject.optString(key);
        if (StringUtils.isNotBlank(listString)) {
            String[] arrayOfStringValues = listString.split(" ");
            for (String c : arrayOfStringValues) {
                if (StringUtils.isNotBlank(c)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public static List<String> extractListByKeyOptString(JSONObject jsonObject, String key) {
        List<String> values = new ArrayList<>();
        if (jsonObject == null || StringUtils.isBlank(key) || !jsonObject.has(key)) {
            return values;
        }

        JSONArray jsonArray = jsonObject.optJSONArray(key);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                String value = jsonArray.optString(i);
                if (value != null) {
                    values.add(value);
                }
            }
        } else {
            String value = jsonObject.optString(key);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    public static Integer integerOrNull(JSONObject jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.optInt(key) : null;
    }

    public static Boolean booleanOrNull(JSONObject jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.optBoolean(key) : null;
    }

    public static Long longOrNull(JSONObject jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.optLong(key) : null;
    }

    public static String stringOrNull(JSONObject jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.optString(key) : null;
    }

    /**
     * Creates a special SSLContext using a custom TLS version and a set of ciphers enabled to process SSL connections.
     *
     * @param tlsVersion TLS version, for example TLSv1.2
     * @param ciphers    Set of ciphers used to create connections.
     */
    public static CloseableHttpClient createHttpClient(String tlsVersion, String[] ciphers) {
        try {
            SSLContext sslContext = SSLContexts.createDefault();
            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
                    new String[]{tlsVersion}, ciphers, NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();

            try (PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry)) {
                return HttpClients.custom()
                        .setSSLContext(sslContext)
                        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                        .setConnectionManager(cm)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error creating HttpClient with a custom TLS version and custom ciphers", e);
            return null;
        }
    }

}
