/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import io.jans.as.model.common.HasParamName;
import io.jans.orm.annotation.AttributeEnum;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */

public class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static final String UTF8_STRING_ENCODING = "UTF-8";

    public static final String PAR_ID_REFIX = "urn:ietf:params:oauth:request_uri:";

    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = AnnotationIntrospector.pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);
        return mapper;
    }

    public static String asJsonSilently(Object p_object) {
        try {
            return asJson(p_object);
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
            return "";
        }
    }

    public static String asPrettyJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p_object);
    }

    public static String asJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        return mapper.writeValueAsString(p_object);
    }

    public static byte[] getBytes(String p_str) throws UnsupportedEncodingException {
        return p_str.getBytes(UTF8_STRING_ENCODING);
    }

    public static List<String> asList(JSONArray p_array) throws JSONException {
        final List<String> result = new ArrayList<String>();
        if (p_array != null) {
            final int length = p_array.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    result.add(p_array.getString(i));
                }
            }
        }
        return result;
    }

    public static <T extends AttributeEnum> List<T> asEnumList(JSONArray p_array, Class<T> clazz)
            throws JSONException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final List<T> result = new ArrayList<T>();
        if (p_array != null) {
            final int length = p_array.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    Method method = clazz.getMethod("getByValue", String.class);
                    result.add((T) method.invoke(null, new Object[]{p_array.getString(i)}));
                }
            }
        }
        return result;
    }

    public static void addToListIfHas(List<String> p_list, JSONObject jsonObj, String p_key) throws JSONException {
        if (jsonObj != null && org.apache.commons.lang.StringUtils.isNotBlank(p_key) && jsonObj.has(p_key)) {
            JSONArray array = jsonObj.getJSONArray(p_key);
            if (p_list != null && array != null) {
                p_list.addAll(asList(array));
            }
        }
    }

    public static void addToJSONObjectIfNotNull(JSONObject p_jsonObject, String key, Object value) throws JSONException {
        if (p_jsonObject != null && value != null && StringUtils.isNotBlank(key)) {
            p_jsonObject.put(key, value);
        }
    }

    public static void addToJSONObjectIfNotNull(JSONObject p_jsonObject, String key, AttributeEnum value) throws JSONException {
        if (p_jsonObject != null && value != null && StringUtils.isNotBlank(key)) {
            p_jsonObject.put(key, value.getValue());
        }
    }

    public static void addToJSONObjectIfNotNull(JSONObject p_jsonObject, String key, String[] value) throws JSONException {
        if (p_jsonObject != null && value != null && StringUtils.isNotBlank(key)) {
            p_jsonObject.put(key, new JSONArray(Arrays.asList(value)));
        }
    }

    public static String asString(List<? extends HasParamName> p_list) {
        final StringBuilder sb = new StringBuilder();
        if (p_list != null && !p_list.isEmpty()) {
            for (HasParamName p : p_list) {
                sb.append(" ").append(p.getParamName());
            }
        }
        return sb.toString().trim();
    }

    public static String listAsString(List<String> p_list) {
        StringBuilder param = new StringBuilder();
        if (p_list != null && !p_list.isEmpty()) {
            for (String item : p_list) {
                param.append(" ").append(item);
            }
        }
        return param.toString().trim();
    }

    public static String mapAsString(Map<String, String> p_map) throws JSONException {
        if (p_map == null || p_map.size() == 0) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();
        for (String key : p_map.keySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(key, p_map.get(key));

            jsonArray.put(jsonObject);
        }

        return jsonArray.toString();
    }

    public static boolean allNotBlank(String... p_strings) {
        if (p_strings != null && p_strings.length > 0) {
            for (String s : p_strings) {
                if (org.apache.commons.lang.StringUtils.isBlank(s)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static List<String> splittedStringAsList(String p_string, String p_delimiter) {
        final List<String> result = new ArrayList<String>();
        if (StringUtils.isNotBlank(p_string)) {
            final String[] array = p_string.split(p_delimiter);
            if (array.length > 0) {
                result.addAll(Arrays.asList(array));
            }
        }
        return result;
    }

    public static List<String> jsonArrayStringAsList(String jsonString) throws JSONException {
        final List<String> result = new ArrayList<String>();
        if (StringUtils.isNotBlank(jsonString)) {
            JSONArray jsonArray = new JSONArray(jsonString);

            return asList(jsonArray);
        }

        return result;
    }

    public static JSONArray listToJsonArray(Collection<String> list) {
        if (list == null) {
        	return new JSONArray();
        }

        return new JSONArray(list);
    }

    /**
     * @param jsonString [{"CustomHeader1":"custom_header_value_1"},.....,{"CustomHeaderN":"custom_header_value_N"}]
     * @return
     */
    public static Map<String, String> jsonObjectArrayStringAsMap(String jsonString) throws JSONException {
        Map<String, String> result = new HashMap<String, String>();

        if (!isNullOrEmpty(jsonString)) {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Iterator<String> keysIter = jsonObject.keys();
                while (keysIter.hasNext()) {
                    String key = keysIter.next();
                    String value = jsonObject.getString(key);
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    public static <T> T firstItem(List<T> items) {
        if (items == null) {
            return null;
        }

        Iterator<T> iterator = items.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static int parseIntSilently(String intString) {
        try {
            return Integer.parseInt(intString);
        } catch (Exception e) {
            return -1;
        }
    }

    // SHA-1 (160 bits)
    public static String toSHA1HexString(String input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            return byteArrayToHexString(md.digest(input.getBytes()));
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    public static Integer getNumberOfSecondFromNow(Date date) {
        if (date == null) {
            return 0;
        }

        long now = new Date().getTime();
        final long time = date.getTime();
        if (time > now) {
            return (int) (time - now) / 1000;
        }

        return null;
    }

    public static Date createExpirationDate(Integer lifetimeInSeconds) {
        if (lifetimeInSeconds == null || lifetimeInSeconds == 0)
            throw new IllegalArgumentException("lifetime can't be null or zero");

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, lifetimeInSeconds);
        return calendar.getTime();
    }

    public static boolean isPar(String requestUri) {
        if (StringUtils.isBlank(requestUri)) {
            return false;
        }
        return requestUri.startsWith(PAR_ID_REFIX);
    }
}
