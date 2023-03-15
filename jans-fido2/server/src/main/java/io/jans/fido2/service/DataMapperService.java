/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import java.io.BufferedReader;
import java.io.IOException;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Conversions to/from JSON format and to/from CBOR format
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class DataMapperService {

    @Inject
    private Logger log;

    private ObjectMapper objectMapper;
	private ObjectMapper jaxbObjectMapper;

    private CBORFactory cborFactory;
    private ObjectMapper cborObjectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.cborFactory = new CBORFactory();
        this.cborObjectMapper = new ObjectMapper(cborFactory);
        this.jaxbObjectMapper = jsonMapperWithWrapRoot();
    }

    public JsonNode readTree(byte[] content) throws IOException {
        return objectMapper.readTree(content);
    }

    public JsonNode readTree(String content) throws IOException {
        return objectMapper.readTree(content);
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

    public CBORParser cborCreateParser(byte[] data) throws IOException {
        return cborFactory.createParser(data);
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    private ObjectMapper createJsonMapperWithJaxb() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = AnnotationIntrospector.pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);

        return mapper;
    }

    private ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapperWithJaxb().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

}
