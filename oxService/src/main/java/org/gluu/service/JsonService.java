/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;

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
