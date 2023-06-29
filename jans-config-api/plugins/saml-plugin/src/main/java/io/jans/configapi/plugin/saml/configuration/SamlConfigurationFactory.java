/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.configuration;

import io.jans.configapi.configuration.ConfigurationFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class SamlConfigurationFactory {

    public static final String CONFIGURATION_ENTRY_DN = "saml_ConfigurationEntryDN";

    @Inject
    private Logger log;

   @Inject
   ConfigurationFactory configurationFactory;
   
   public String getScimConfigurationDn() {
       return configurationFactory.getConfigurationDn(CONFIGURATION_ENTRY_DN);
   }

}
