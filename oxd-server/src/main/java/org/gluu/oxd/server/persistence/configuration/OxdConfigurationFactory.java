package org.gluu.oxd.server.persistence.configuration;

import org.gluu.conf.service.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OxdConfigurationFactory extends ConfigurationFactory<OxdAppConfiguration, OxdAppConfigurationEntry> {

    private final Logger LOG = LoggerFactory.getLogger(OxdConfigurationFactory.class);

    private static class ConfigurationSingleton {
        static OxdConfigurationFactory INSTANCE = new OxdConfigurationFactory();
    }

    public static OxdConfigurationFactory instance() {
        return ConfigurationSingleton.INSTANCE;
    }

    protected String getDefaultConfigurationFileName() {
        return "gluu.properties";
    }

    protected Class<OxdAppConfigurationEntry> getAppConfigurationType() {
        return OxdAppConfigurationEntry.class;
    }

    protected String getApplicationConfigurationPropertyName() {
        return "oxd_ConfigurationEntryDN";
    }

    protected String getDefaultPersistanceConfigurationFileName() {
        return "gluu-ldap.properties";
    }
}
