/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.service;

import java.io.BufferedReader;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

@ApplicationScoped
public class DataMapperService {

    @Inject
    private Logger log;

    private ObjectMapper objectMapper;

    private CBORFactory cborFactory;
    private ObjectMapper cborObjectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.cborFactory = new CBORFactory();
        this.cborObjectMapper = new ObjectMapper(cborFactory);
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

    public ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    public JsonNode cborReadTree(byte[] content) throws IOException {
        return cborObjectMapper.readTree(content);
    }

    public CBORParser cborCreateParser(byte[] data) throws IOException {
        return cborFactory.createParser(data);
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }
}
