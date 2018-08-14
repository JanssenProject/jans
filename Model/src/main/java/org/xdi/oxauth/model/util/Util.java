/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.xdi.oxauth.model.common.HasParamName;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version July 18, 2017
 */

public class Util {

    private static final Logger LOG = Logger.getLogger(Util.class);

    public static final String UTF8_STRING_ENCODING = "UTF-8";

    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = new AnnotationIntrospector.Pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().withAnnotationIntrospector(pair);
        mapper.getSerializationConfig().withAnnotationIntrospector(pair);
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
        final ObjectMapper mapper = createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p_object);
    }

    public static String asJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
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

    public static <T extends LdapEnum> List<T> asEnumList(JSONArray p_array, Class<T> clazz)
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
        if (org.apache.commons.lang.StringUtils.isNotBlank(p_string) && org.apache.commons.lang.StringUtils.isNotEmpty(p_delimiter)) {
            final String[] array = p_string.split(p_delimiter);
            if (array != null && array.length > 0) {
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

}
