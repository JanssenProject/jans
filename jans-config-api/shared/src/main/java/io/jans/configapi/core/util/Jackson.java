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
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
        LOG.debug("Patch details - patchAsString:{}, obj:{}", patchAsString, obj);
        JsonPatch jsonPatch = JsonPatch.fromJson(Jackson.asJsonNode(patchAsString));
        return applyPatch(jsonPatch, obj);
    }

    public static boolean isFieldPresent(String patchAsString, String fieldName) {
        LOG.debug("Check if FieldPresent patchAsString:{} contains fieldName:{}", patchAsString, fieldName);
        boolean isPresent = false;
        if (patchAsString == null || fieldName == null) {
            LOG.warn("isFieldPresent called with null parameter - patchAsString: {}, fieldName: {}", patchAsString,
                    fieldName);
            return isPresent;
        }
        try {
            JsonNode jsonNode = Jackson.asJsonNode(patchAsString);
            LOG.debug("patchAsString jsonNode:{}", jsonNode);
            List<String> keys = new ArrayList<>();
            if (jsonNode.isArray()) {
                for (JsonNode operationNode : jsonNode) {
                    JsonNode pathNode = operationNode.get("path");
                    if (pathNode != null && pathNode.isTextual()) {
                        keys.add(pathNode.asText());
                    }
                }

                LOG.debug(" FieldPresent keys:{}", keys);
                isPresent = keys.contains("/" + fieldName);
                LOG.debug(" FieldPresent contains fieldName:{}?:{}", fieldName, isPresent);
            }

        } catch (Exception e) {
            LOG.error("Error processing JSON Patch string for field '{}'", fieldName, e);
        }
        return isPresent;
    }

    public static JsonPatch getJsonPatch(String patchAsString) throws JsonPatchException, IOException {
        LOG.debug("Patch details - patchAsString:{}", patchAsString);
        return JsonPatch.fromJson(Jackson.asJsonNode(patchAsString));
    }

    public static <T> T applyJsonPatch(JsonPatch jsonPatch, T obj) throws JsonPatchException, IOException {
        LOG.debug("Patch details - jsonPatch:{}, obj:{}", jsonPatch, obj);
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

    public static <T> String getJsonString(T obj) throws IOException {
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

    public static <T> T readStringValue(String content, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, clazz);
    }

    public static <T> List<T> readListValue(String content, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, new TypeReference<List<T>>() {
        });
    }

    public static <T> List<T> readList(String str, Class<T> type) {
        return readList(str, ArrayList.class, type);
    }

    public static <T> List<T> readList(String str, Class<? extends Collection> type, Class<T> elementType) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(str, mapper.getTypeFactory().constructCollectionType(type, elementType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static boolean isValidJson(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public static boolean isValidJson(Object obj) {
        try {
            new JSONObject(obj);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public static JSONObject convertObjectToJsonObject(Object obj) throws JsonProcessingException {
        final ObjectMapper objectMapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        String jsonString = objectMapper.writeValueAsString(obj);
        // Create JSONObject from the JSON string
        return new JSONObject(jsonString);
    }

}
