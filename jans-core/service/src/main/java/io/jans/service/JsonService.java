/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public <T> T jsonToObject(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(json, clazz);
    }

    public <T> String objectToJson(T obj) throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writeValueAsString(obj);
    }

    public <T> String objectToPerttyJson(T obj) throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

}
