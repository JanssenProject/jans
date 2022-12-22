/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import io.jans.util.ArrayHelper;
import io.jans.util.Util;

/**
 * @author Madhumita S
 */

public class ServerUtil {

    private static final Logger log = LoggerFactory.getLogger(ServerUtil.class);
   
    private ServerUtil() {
    }

    public static GregorianCalendar now() {
        return new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    }

    public static int nowAsSeconds() {
        return (int) (new Date().getTime() / 1000L);
    }

    public static int calculateTtl(Integer expirationDateAsSeconds) {
        if (expirationDateAsSeconds == null) {
            return 0;
        }
        return expirationDateAsSeconds - nowAsSeconds();
    }

    public static int calculateTtl(Date creationDate, Date expirationDate) {
        if (creationDate != null && expirationDate != null) {
            return (int) ((expirationDate.getTime() - creationDate.getTime()) / 1000L);
        }
        return 0;
    }

    public static String asJsonSilently(Object obj) {
        try {
            return asJson(obj);
        } catch (IOException e) {
            log.trace(e.getMessage(), e);
            return "";
        }
    }

    public static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        };
    }

    public static String asPrettyJson(Object obj) throws IOException {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    public static String asJson(Object obj) throws IOException {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(obj);
    }

   
    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = AnnotationIntrospector.pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);
        return mapper;
    }

    public static ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

    public static ObjectMapper jsonMapperWithUnwrapRoot() {
        return createJsonMapper().configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    }

    public static String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

   

    public static String urlDecode(String str) {
        if (StringUtils.isNotBlank(str)) {
            try {
                return URLDecoder.decode(str, Util.UTF8);
            } catch (UnsupportedEncodingException e) {
                log.trace(e.getMessage(), e);
            }
        }
        return str;
    }

   


    public static String getFirstValue(Map<String, String[]> map, String key) {
        if (map.containsKey(key)) {
            String[] values = map.get(key);
            if (ArrayHelper.isNotEmpty(values)) {
                return values[0];
            }
        }

        return null;
    }



    public static Integer dateToSeconds(Date date) {
        return date != null ? (int) (date.getTime() / 1000) : null;
    }
}
