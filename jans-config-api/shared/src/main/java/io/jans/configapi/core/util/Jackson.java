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
import java.util.ArrayList;
import java.util.List;
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

    /**
     * Prevents instantiation of this utility class.
     */
    private Jackson() {
    }

    public static JsonNode asJsonNode(String objAsString) throws JsonProcessingException {
        return JacksonUtils.newMapper().readTree(objAsString);
    }

    /**
     * Retrieve the text value of a top-level field from a JSON string.
     *
     * @param jsonString the JSON content to parse
     * @param fieldName  the top-level field name whose text value to return
     * @return the field's text value, or `null` if the field is present but not a textual node
     * @throws JsonProcessingException if the input cannot be parsed as JSON
     */
    @SuppressWarnings("unchecked")
    public static String getElement(String jsonString, String fieldName) throws JsonProcessingException {
        JsonNode jsonNode = JacksonUtils.newMapper().readTree(jsonString);
        return jsonNode.get(fieldName).textValue();
    }

    /**
     * Apply a JSON Patch (RFC 6902) provided as a JSON string to the given object and return the patched instance.
     *
     * @param <T> the type of the target object
     * @param patchAsString the JSON Patch document as a JSON array string
     * @param obj the target object to which the patch will be applied; the returned object will be of the same type
     * @return the object resulting from applying the patch
     * @throws JsonPatchException if the patch cannot be applied to the object's JSON representation
     * @throws IOException if there is an error parsing the patch or mapping JSON to/from the object
     */
    public static <T> T applyPatch(String patchAsString, T obj) throws JsonPatchException, IOException {
        LOG.debug("Patch details - patchAsString:{}, obj:{}", patchAsString, obj);
        JsonPatch jsonPatch = JsonPatch.fromJson(Jackson.asJsonNode(patchAsString));
        return applyPatch(jsonPatch, obj);
    }

    /**
     * Checks whether a JSON Patch string contains an operation targeting the specified field.
     *
     * @param patchAsString the JSON Patch document as a string (expected to be a JSON array of operations)
     * @param fieldName     the target field name to look for (without a leading '/')
     * @return              `true` if any operation's "path" equals `"/" + fieldName`, `false` otherwise
     */
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

    /**
     * Creates a JsonPatch from a JSON Patch document represented as a string.
     *
     * @param patchAsString the JSON string containing the JSON Patch document
     * @return the parsed JsonPatch instance
     * @throws JsonPatchException if the JSON cannot be converted to a JsonPatch
     * @throws IOException if an I/O error occurs while parsing the string
     */
    public static JsonPatch getJsonPatch(String patchAsString) throws JsonPatchException, IOException {
        LOG.debug("Patch details - patchAsString:{}", patchAsString);
        return JsonPatch.fromJson(Jackson.asJsonNode(patchAsString));
    }

    /**
     * Applies a JSON Patch to the given object and returns the resulting object.
     *
     * @param jsonPatch the JSON Patch to apply
     * @param obj the target object to patch; the returned value will be of the same runtime type
     * @param <T> the type of the target object
     * @return the patched object of the same type as {@code obj}
     * @throws JsonPatchException if the patch cannot be applied to the target JSON
     * @throws IOException if an I/O error occurs while converting between object and JSON
     */
    public static <T> T applyJsonPatch(JsonPatch jsonPatch, T obj) throws JsonPatchException, IOException {
        LOG.debug("Patch details - jsonPatch:{}, obj:{}", jsonPatch, obj);
        return applyPatch(jsonPatch, obj);
    }

    /**
     * Applies a JSON Patch to the given object and returns a new instance representing the patched result.
     *
     * @param jsonPatch the JSON Patch to apply
     * @param obj       the target object whose JSON representation will be patched
     * @param <T>       the runtime type of the target object
     * @return the patched object of the same runtime class as {@code obj}
     * @throws JsonPatchException       if the patch cannot be applied to the object's JSON representation
     * @throws JsonProcessingException  if conversion between the object and JSON fails
     */
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

    /**
     * Constructs a JSONObject from the provided map of keys to values.
     *
     * The map's entries are added to the resulting JSONObject using each entry's key and value.
     *
     * @param map the mapping of keys to values to include in the JSONObject; may be null or empty
     * @return a JSONObject containing the map's entries, or {@code null} if {@code map} is null or empty
     * @throws JSONException if an error occurs while inserting a value into the JSONObject
     */
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

    /**
     * Determines whether a string contains valid JSON.
     *
     * @param json the string to validate as JSON
     * @return true if the string is valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether an object can be parsed as JSON by org.json.JSONObject.
     *
     * @param obj the object to validate as JSON (commonly a String or other JSON-compatible value)
     * @return `true` if the object can be parsed into a JSONObject, `false` otherwise
     */
    public static boolean isValidJson(Object obj) {
        try {
            new JSONObject(obj);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    /**
     * Convert an object into an org.json.JSONObject by serializing it to JSON and parsing that string.
     *
     * @param obj the object to convert into a JSONObject
     * @return the JSONObject representation of the provided object
     * @throws JsonProcessingException if the object cannot be serialized to JSON
     */
    public static JSONObject convertObjectToJsonObject(Object obj) throws JsonProcessingException {
        final ObjectMapper objectMapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        String jsonString = objectMapper.writeValueAsString(obj);
        // Create JSONObject from the JSON string
        return new JSONObject(jsonString);
    }

}