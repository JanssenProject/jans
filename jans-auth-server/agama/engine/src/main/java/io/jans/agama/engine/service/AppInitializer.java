package io.jans.agama.engine.service;

import io.jans.ads.timer.DeployerTimer;
import io.jans.agama.timer.FlowRunsCleaner;
import io.jans.agama.timer.Transpilation;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class AppInitializer {
    
    @Inject
    private Transpilation trTimer;
    
    @Inject
    private FlowRunsCleaner fcleaner;
    
    @Inject
    private DeployerTimer deployerTimer;

    public void run(@Observes @ApplicationInitialized(ApplicationScoped.class) 
            ApplicationInitializedEvent event) {

        trTimer.initTimer();
        fcleaner.initTimer();
        deployerTimer.initTimer();
        
    }

}
