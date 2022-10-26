package io.jans.agama.engine.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.agama.model.serialize.Type;
import org.slf4j.Logger;

/**
 * Warning: This serialization strategy is not implemented yet
 */
@ApplicationScoped
public class FstSerializer implements ObjectSerializer {
    
    @Inject
    private Logger logger;

    @Override
    public Object deserialize(InputStream in) throws IOException {
        return null;
    }
    
    @Override
    public void serialize(Object data, OutputStream out) throws IOException {         
    }
    
    @Override
    public Type getType() {
        return Type.FST;
    }
    
    @PostConstruct
    private void init() {
        
    }
    
}
