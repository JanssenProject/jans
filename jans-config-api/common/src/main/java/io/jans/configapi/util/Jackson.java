/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Jackson {

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
        JsonPatch jsonPatch = JsonPatch.fromJson(Jackson.asJsonNode(patchAsString));
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
        try {

            Preconditions.checkNotNull(inputStream);
            ObjectMapper objectMapper = JacksonUtils.newMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return (T) JacksonUtils.newMapper().readValue(inputStream, obj.getClass());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(String jsonString, T obj) throws JsonPatchException, IOException {
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

    public <T> String getJsonString(T obj) throws JsonPatchException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }

}
