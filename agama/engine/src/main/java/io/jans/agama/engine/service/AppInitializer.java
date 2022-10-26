package io.jans.agama.engine.service;

import io.jans.agama.timer.FlowRunsCleaner;
import io.jans.agama.timer.Transpilation;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class AppInitializer {

    @Inject
    private Logger logger;
    
    @Inject
    private Transpilation trTimer;
    
    @Inject
    private FlowRunsCleaner fcleaner;

    public void run(@Observes @ApplicationInitialized(ApplicationScoped.class) 
            ApplicationInitializedEvent event) {
        
        logger.info("Initializing Agama services");
        trTimer.initTimer();
        fcleaner.initTimer();
        
    }
    
}
