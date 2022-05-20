package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.model.EngineConfig;
//import io.jans.as.model.configuration.AppConfiguration;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
//import jakarta.inject.Inject;

@ApplicationScoped
public class ServicesFactory {

    //@Inject
    //private AppConfiguration asConfig;
    
    private ObjectMapper mapper;

    @Produces
    public ObjectMapper mapperInstance() {
        return mapper;
    }
    
    @Produces
    @ApplicationScoped
    public EngineConfig engineConfigInstance() {
        //return asConfig.getAgamaConfiguration();
        //TODO: #1388. Temporarily return dummy instance
        return new EngineConfig();
    }

    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
    }
    
}
