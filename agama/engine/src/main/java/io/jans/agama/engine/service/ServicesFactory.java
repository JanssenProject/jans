package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.model.Config;
import io.jans.agama.model.EngineConfig;
import io.jans.as.model.configuration.AppConfiguration;
import jakarta.annotation.PostConstruct;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ServicesFactory {

    @Inject
    private Logger logger;

    @Inject
    private AppConfiguration asConfig;
    
    @Inject
    private Config config;
    
    private ObjectMapper mapper;

    @Produces
    public ObjectMapper mapperInstance() {
        return mapper;
    }
    
    @Produces
    @ApplicationScoped
    public EngineConfig engineConfigInstance() {
        return config.getEngineConf();
    }

    @PostConstruct
    public void init() {

        mapper = new ObjectMapper();
        EngineConfig econf = config.getEngineConf();

        int inter = econf.getInterruptionTime();
        int unauth = asConfig.getSessionIdUnauthenticatedUnusedLifetime();
        if (inter == 0 || inter > unauth) {
            //Ensure interruption time is lower than or equal to unauthenticated unused
            econf.setInterruptionTime(unauth);
            logger.warn("Agama flow interruption time modified to {}", unauth);
        }

    }
    
}
