package io.jans.casa.plugins.authnmethod.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.casa.core.PersistenceService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.json.JSONObject;

class BaseService {

    @Inject
    PersistenceService persistenceService;

    ObjectMapper mapper;

    JSONObject props;

    @PostConstruct
    private void inited() {
        mapper = new ObjectMapper();
    }

    public String getPropertyValue(String key) {
        return props.optString(key, null);
    }

}
