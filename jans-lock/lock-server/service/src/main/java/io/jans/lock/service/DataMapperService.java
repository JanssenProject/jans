package io.jans.lock.service;

/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Conversions to/from JSON format
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class DataMapperService {

    @Inject
    private Logger log;

    private ObjectMapper objectMapper;
	private ObjectMapper jaxbObjectMapper;

    private ObjectMapper cborObjectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.jaxbObjectMapper = jsonMapperWithWrapRoot();
    }

    public JsonNode readTree(byte[] content) throws IOException {
        return objectMapper.readTree(content);
    }

    public JsonNode readTree(String content) throws IOException {
        return objectMapper.readTree(content);
    }

    public JsonNode readTree(InputStream in) throws IOException {
        return objectMapper.readTree(in);
    }

    public JsonNode readTree(BufferedReader reader) throws IOException {
        return objectMapper.readTree(reader);
    }

    public <T> T readValue(String content, Class<T> clazz) throws IOException {
        return jaxbObjectMapper.readValue(content, clazz);
    }

    public ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    public ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    public JsonNode cborReadTree(byte[] content) throws IOException {
        return cborObjectMapper.readTree(content);
    }

    public byte[] cborWriteAsBytes(JsonNode jsonNode) throws IOException {
        return cborObjectMapper.writeValueAsBytes(jsonNode);
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    public String writeValueAsString(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    public <T> T readValueString(String content, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(content, clazz);
    }

    private ObjectMapper createJsonMapperWithJaxb() {
        final ObjectMapper mapper = new ObjectMapper();

        return mapper;
    }

    private ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapperWithJaxb().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }
}
