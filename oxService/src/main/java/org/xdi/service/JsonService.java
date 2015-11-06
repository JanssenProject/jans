/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * Service class to work with JSON strings
 *
 * @author Yuriy Movchan Date: 05/14/2013
 */
@Name("jsonService")
@Scope(ScopeType.APPLICATION)
@AutoCreate
public class JsonService {

    @Logger
    private Log log;

    private ObjectMapper mapper;

    @Create
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

    /**
     * Get jsonService instance
     *
     * @return JsonService instance
     */
    public static JsonService instance() {
        return (JsonService) Component.getInstance(JsonService.class);
    }

}
