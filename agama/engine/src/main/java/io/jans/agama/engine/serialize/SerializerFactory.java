package io.jans.agama.engine.serialize;
        
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.jans.agama.model.EngineConfig;

@ApplicationScoped
public class SerializerFactory {
    
    @Inject
    private EngineConfig engineConf;
    
    @Inject @Any 
    private Instance<ObjectSerializer> services;
    
    private ObjectSerializer serializer;
    
    public ObjectSerializer get() {
        return serializer;
    }

    @PostConstruct
    private void init() {        
        serializer = services.stream()
                .filter(s -> s.getType().equals(engineConf.getSerializerType()))
                .findFirst().orElse(null);                
    }
    

}
