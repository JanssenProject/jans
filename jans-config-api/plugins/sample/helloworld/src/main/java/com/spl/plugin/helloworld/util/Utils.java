/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package com.spl.plugin.helloworld.util;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.service.ClientService;
import io.jans.configapi.core.service.ConfService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

@ApplicationScoped
public class Utils {
    
    @Inject
    Logger log;
    
    @Inject
    ClientService clientService;
    
    @Inject
    ConfService confService;

    public String getIssuer() {
       log.debug("\n Utils::getIssuer() - confService = "+confService+"\n\n");
        return confService.find().getIssuer();
    }
    
    public Client getClient(String clientId) {
        log.debug("\n Utils::getClient() - clientService = "+clientService+", clientId = "+clientId+"\n\n");
        return clientService.getClientByInum(clientId);
    }

    

}