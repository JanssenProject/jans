package io.jans.agama.engine.service;

import io.jans.ads.timer.DeployerTimer;
import io.jans.agama.timer.FlowRunsCleaner;
import io.jans.agama.timer.Transpilation;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import io.jans.service.document.store.manager.DocumentStoreManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class AppInitializer {

    private final static String DOCUMENT_STORE_MANAGER_JANS_AGAMA_TYPE = "agama"; // Module name
    
    @Inject
    private Transpilation trTimer;
    
    @Inject
    private FlowRunsCleaner fcleaner;
    
    @Inject
    private DeployerTimer deployerTimer;

    @Inject
    private DocumentStoreManager documentStoreManager;
 
    public void run(@Observes @ApplicationInitialized(ApplicationScoped.class) 
            ApplicationInitializedEvent event) {
    	
        // Add Agama type to Document Store Manager
    	documentStoreManager.getSupportedServiceTypes().add(DOCUMENT_STORE_MANAGER_JANS_AGAMA_TYPE);

        trTimer.initTimer();
        fcleaner.initTimer();
        deployerTimer.initTimer();
        
    }

}
