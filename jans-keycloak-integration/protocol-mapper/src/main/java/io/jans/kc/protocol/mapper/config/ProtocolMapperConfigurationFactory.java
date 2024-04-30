package io.jans.kc.protocol.mapper.config;

import io.jans.conf.service.ConfigurationFactory;

public class ProtocolMapperConfigurationFactory extends ConfigurationFactory<ProtocolMapperConfiguration,ProtocolMapperConfigurationEntry> {
    
    
    @Override
    protected String getDefaultConfigurationFileName() {

        return null;
    }

    @Override
    protected Class<ProtocolMapperConfigurationEntry> getAppConfigurationType() {

        return ProtocolMapperConfigurationEntry.class;
    }

    @Overide
    protected String getApplicationConfigurationPropertyName() {

        return null;
    }
}
