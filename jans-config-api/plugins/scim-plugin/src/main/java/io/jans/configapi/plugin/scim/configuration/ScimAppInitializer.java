/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.configuration;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimAppInitializer {
    
    @Inject
    private Logger logger;

    @Inject
    private ScimConfigurationFactory scimConfigurationFactory;
    
    public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {

        logger.info("SCIM configuration initializing...");
        

        scimConfigurationFactory.create();
        
        logger.info("SCIM timer initializing...");
        
        //scimConfigurationFactory.initTimer();
       

    }


}
