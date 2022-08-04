/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Jackson {

    private static final Logger LOG = LoggerFactory.getLogger(Jackson.class);
    private Jackson() {
    }

    public static JsonNode asJsonNode(String objAsString) throws JsonProcessingException {
        return JacksonUtils.newMapper().readTree(objAsString);
    }

    @SuppressWarnings("unchecked")
    public static String getElement(String jsonString, String fieldName) throws JsonProcessingException {
        JsonNode jsonNode = JacksonUtils.newMapper().readTree(jsonString);
        return jsonNode.get(fieldName).textValue();
    }

    public static <T> T applyPatch(String patchAsString, T obj) throws JsonPatchException, IOException {
        LOG.debug("Patch details - patchAsString:{}, obj:{}", patchAsString, obj );
        JsonPatch jsonPatch = JsonPatch.fromJson(Jackson.asJsonNode(patchAsString));
        return applyPatch(jsonPatch, obj);
    }

    public static <T> T applyJsonPatch(JsonPatch jsonPatch, T obj) throws JsonPatchException, IOException {
        LOG.debug("Patch details - jsonPatch:{}, obj:{}", jsonPatch, obj );
        return applyPatch(jsonPatch, obj);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T applyPatch(JsonPatch jsonPatch, T obj) throws JsonPatchException, JsonProcessingException {
        Preconditions.checkNotNull(jsonPatch);
        Preconditions.checkNotNull(obj);
        ObjectMapper objectMapper = JacksonUtils.newMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode patched = jsonPatch.apply(objectMapper.convertValue(obj, JsonNode.class));
        return (T) objectMapper.treeToValue(patched, obj.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T> T read(InputStream inputStream, T obj) throws IOException {
        Preconditions.checkNotNull(inputStream);
        return (T) JacksonUtils.newMapper().readValue(inputStream, obj.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(String jsonString, T obj) throws IOException {
        ObjectMapper objectMapper = JacksonUtils.newMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return (T) objectMapper.readValue(jsonString, obj.getClass());
    }

    public static ObjectMapper createJsonMapper() {
        return JacksonMapperHolder.MAPPER;
    }

    private static class JacksonMapperHolder {
        private static final ObjectMapper MAPPER = jsonMapper();

        public static ObjectMapper jsonMapper() {
            final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

            final ObjectMapper mapper = new ObjectMapper();
            final DeserializationConfig deserializationConfig = mapper.getDeserializationConfig().with(jackson);
            final SerializationConfig serializationConfig = mapper.getSerializationConfig().with(jackson);
            if (deserializationConfig != null && serializationConfig != null) {
                // do nothing for now
            }
            return mapper;
        }
    }

    public <T> String getJsonString(T obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }

    public static String asJson(Object obj) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(obj);
    }

    public static String asPrettyJson(Object obj) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    public static JSONObject createJSONObject(Map<String, Object> map) throws JSONException {
        if (map == null || map.size() == 0) {
            return null;
        }

        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }

        return jsonObject;
    }

}
