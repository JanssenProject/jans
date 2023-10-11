package io.jans.casa.plugins.authnmethod.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.casa.core.PersistenceService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;

/**
 * @author jgomer
 */
class BaseService {

    @Inject
    PersistenceService persistenceService;

    ObjectMapper mapper;

    Map<String, String> props;

    @PostConstruct
    private void inited() {
        mapper = new ObjectMapper();
    }

    public String getScriptPropertyValue(String key) {
        return Optional.ofNullable(props).flatMap(m -> Optional.ofNullable(m.get(key))).orElse(null);
    }

}
