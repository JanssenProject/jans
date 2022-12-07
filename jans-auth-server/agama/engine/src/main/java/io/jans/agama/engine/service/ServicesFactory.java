package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.engine.serialize.SerializerFactory;
import io.jans.agama.model.EngineConfig;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.service.cdi.event.ConfigurationUpdate;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ServicesFactory {

    @Inject
    private Logger logger;

    @Inject 
    private SerializerFactory serializerFactory;

    private ObjectMapper mapper;

    private EngineConfig econfig;

    @Produces
    public ObjectMapper mapperInstance() {
        return mapper;
    }
    
    @Produces
    @ApplicationScoped
    public EngineConfig engineConfigInstance() {
        return econfig;
    }

    public void updateConfiguration(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
        
        try {
            EngineConfig newConfig = appConfiguration.getAgamaConfiguration();

            if (newConfig == null) {
                logger.info("Agama will not be available in this deployment");

            } else {
                logger.info("Refreshing Agama configuration...");
                BeanUtils.copyProperties(econfig, newConfig);
                serializerFactory.refresh();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
    }

    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
        econfig = new EngineConfig();
    }
    
}
