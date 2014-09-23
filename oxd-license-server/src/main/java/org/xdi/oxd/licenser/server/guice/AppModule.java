package org.xdi.oxd.licenser.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.licenser.server.KeyPairService;
import org.xdi.oxd.licenser.server.LicenseGenerator;
import org.xdi.oxd.licenser.server.conf.Configuration;
import org.xdi.oxd.licenser.server.conf.ConfigurationFactory;
import org.xdi.oxd.licenser.server.conf.JsonFileConfiguration;
import org.xdi.oxd.licenser.server.ldap.Conf;
import org.xdi.oxd.licenser.server.ws.GenerateLicenseWS;
import org.xdi.util.Util;
import org.xdi.util.properties.FileConfiguration;
import org.xdi.util.security.PropertiesDecrypter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

public class AppModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(AppModule.class);

    @Override
    protected void configure() {
        bind(KeyPairService.class);
        bind(LicenseGenerator.class);

        // ws
        bind(GenerateLicenseWS.class);
    }

    @Provides
    @Singleton
    public LdapEntryManager provideLdapManager() {
        final FileConfiguration fileConfiguration = ConfigurationFactory.getLdapConfiguration();
        final Properties props = PropertiesDecrypter.decryptProperties(fileConfiguration.getProperties());
        final LDAPConnectionProvider connectionProvider = new LDAPConnectionProvider(props);
        return new LdapEntryManager(new OperationsFacade(connectionProvider));
    }

    @Provides
    @Singleton
    public JsonFileConfiguration provideJsonConfiguration() {
        InputStream stream = null;
        try {
            LOG.info("Configuration file location: {}", getConfigFileLocation());
            final File configFile = new File(getConfigFileLocation());
            if (configFile.exists()) {
                stream = new FileInputStream(configFile);
            } else {
                LOG.error("No configuration file. Fail to start! Location: " + getConfigFileLocation());
            }
            return Util.createJsonMapper().readValue(stream, JsonFileConfiguration.class);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public String getConfigFileLocation() {
        return ConfigurationFactory.CONFIG_FILE_LOCATION;
    }

    @Provides
    @Singleton
    public Configuration provideConfiguration(LdapEntryManager ldapManager, JsonFileConfiguration jsonFileConfiguration) throws IOException {
        final String dn = ConfigurationFactory.getLdapConfiguration().getString("configurationEntryDN");
        try {
            final Conf conf = ldapManager.find(Conf.class, dn);
            if (conf != null) {
                return Util.createJsonMapper().readValue(conf.getConf(), Configuration.class);
            }
        } catch (LdapMappingException e) {
            LOG.trace(e.getMessage(), e);
            LOG.info("Unable to find configuration in LDAP, try to create configuration entry in LDAP... ");
            if (ConfigurationFactory.getLdapConfiguration().getBoolean("createLdapConfigurationEntryIfNotExist")) {
                if (jsonFileConfiguration != null) {
                    final Conf c = new Conf();
                    c.setDn(ConfigurationFactory.getLdapConfiguration().getString("configurationEntryDN"));
                    c.setConf(Util.createJsonMapper().writeValueAsString(jsonFileConfiguration));
                    try {
                        ldapManager.persist(c);
                        LOG.info("Configuration entry is created in LDAP.");
                    } catch (Exception ex) {
                        LOG.error(e.getMessage(), ex);
                    }
                    return jsonFileConfiguration;
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        LOG.error("Failed to create configuration.");
        return null;
    }
}