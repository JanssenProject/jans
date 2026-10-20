package io.jans.agama.engine.service;

import io.jans.agama.model.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class TrackingService {
    
    @Inject
    private Logger logger;

    @Inject
    private EngineConfig engineConfig;
    
    public void track(String flowQname, int[] lines, Exception exception) {
        //TODO
        //if (!engineConfig.getTrackingSettings().isEnabled())
        //logger.debug("============== {}: {}, {}", flowQname, lines, exception== null? null : exception.getMessage());        
        //if (flowQname == null) return;
    }
    
    public void track(String flowQname, int line, Exception exception) {
        track(flowQname, new int[]{ line }, exception);
    }
    
}
