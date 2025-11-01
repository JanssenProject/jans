/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;

/**
 * Service class to work with JSON strings
 *
 * @author Yuriy Movchan Date: 05/14/2013
 */
@ApplicationScoped
@Named
public class JsonService implements Serializable {

    private static final long serialVersionUID = -1595376054267897007L;

    @Inject
    private Logger log;

    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        this.mapper = new ObjectMapper();
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    /**
     * Deserialize the given JSON string into an instance of the specified class.
     *
     * @param json  the JSON content to deserialize
     * @param clazz the target class to deserialize into
     * @param <T>   the target type
     * @return an instance of {@code clazz} populated from the JSON
     * @throws JsonParseException   if the input is not valid JSON
     * @throws JsonMappingException if the JSON cannot be mapped to the target type
     * @throws IOException          if an I/O error occurs during deserialization
     */
    public <T> T jsonToObject(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(json, clazz);
    }

    /**
     * Deserialize a JSON string into an instance described by a Jackson {@link JavaType}.
     *
     * @param json the JSON string to deserialize
     * @param valueType the Jackson {@link JavaType} that describes the target type
     * @param <T> the target type
     * @return the deserialized object matching the specified {@code valueType}
     * @throws JsonParseException if the input is not valid JSON
     * @throws JsonMappingException if the JSON cannot be mapped to the specified type
     * @throws IOException if an I/O error occurs during reading
     */
    public <T> T jsonToObject(String json, JavaType valueType) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(json, valueType);
    }

    /**
     * Serialize the given object to a compact JSON string.
     *
     * @param obj the object to serialize to JSON
     * @return the JSON string representation of the object
     * @throws JsonGenerationException if there is a problem writing JSON content
     * @throws JsonMappingException if there is a problem mapping the object to JSON
     * @throws IOException for other I/O-related errors during serialization
     */
    public <T> String objectToJson(T obj) throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writeValueAsString(obj);
    }

    /**
     * Serialize an object to a pretty-printed JSON string.
     *
     * @param obj the object to serialize
     * @return the pretty-printed JSON representation of {@code obj}
     * @throws com.fasterxml.jackson.core.JsonGenerationException if there is a problem generating JSON content
     * @throws com.fasterxml.jackson.databind.JsonMappingException   if mapping between the Java object and JSON fails
     * @throws java.io.IOException                                 if an I/O problem occurs during writing
     */
    public <T> String objectToPrettyJson(T obj) throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
    
    /**
     * Provide access to the Jackson TypeFactory used by this service.
     *
     * @return the TypeFactory instance from the internal ObjectMapper
     */
    public TypeFactory getTypeFactory() {
    	return mapper.getTypeFactory();
    }

}