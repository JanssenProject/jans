package io.jans.kc.protocol.mapper.config.persistence;

import io.jans.conf.model.AppConfiguration;
import io.jans.conf.model.AppConfigurationEntry;

import io.jans.conf.service.ConfigurationFactory;


public class PersistenceConfigurationFactory extends ConfigurationFactory<PersistenceConfiguration,PersistenceConfigurationEntry>{
    
    @Override
    protected String getDefaultConfigurationFileName() {

        return null;
    }

    @Override
    protected Class<PersistenceConfigurationEntry> getAppConfigurationType() {

        return null;
    }

    @Override
    protected String getApplicationConfigurationPropertyName() {

        return null;
    }
}
