package io.jans.agama.engine.serialize;

import io.jans.agama.model.EngineConfig;
import io.jans.agama.model.serialize.Type;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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

    public ObjectSerializer get(Type type) {
        return services.stream().filter(s -> s.getType().equals(type))
                .findFirst().orElse(null);
    }
    
    public void refresh() {
        serializer = get(engineConf.getSerializerType());
    }

}
