/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.util;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.model.SimpleProperty;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.ldap.operation.impl.LdapConnectionProvider;
import io.jans.util.properties.FileConfiguration;
import io.jans.util.security.PropertiesDecrypter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@ApplicationScoped
public class ConnectionStatus {

    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    public boolean isUp(GluuLdapConfiguration ldapConfiguration) {

        FileConfiguration configuration = loadFileConfiguration();
        logger.debug(
                " configuration:{}, ldapConfiguration:{}, ldapConfiguration.getBindDN():{}, ldapConfiguration.getBindPassword()):{}, ldapConfiguration.isUseSSL():{} ",
                configuration, ldapConfiguration, ldapConfiguration.getBindDN(), ldapConfiguration.getBindPassword(),
                ldapConfiguration.isUseSSL());

        Properties properties = configuration.getProperties();
        properties.setProperty("bindDN", ldapConfiguration.getBindDN());
        properties.setProperty("bindPassword", ldapConfiguration.getBindPassword());
        properties.setProperty("servers", buildServersString(getServers(ldapConfiguration)));
        properties.setProperty("useSSL", Boolean.toString(ldapConfiguration.isUseSSL()));
        Properties ldapDecryptedProperties = PropertiesDecrypter.decryptProperties(properties,
                configurationFactory.getCryptoConfigurationSalt());
       
        logger.trace("Attempting to create LDAP connection with properties: {}", ldapDecryptedProperties);
        
        LdapConnectionProvider connectionProvider = new LdapConnectionProvider(
                PropertiesDecrypter.decryptProperties(properties, configurationFactory.getCryptoConfigurationSalt()));

        logger.trace("connectionProvider: {}", connectionProvider);
        connectionProvider.create(ldapDecryptedProperties);
        
        logger.debug("Connection status connectionProvider.isConnected(): {}", connectionProvider.isConnected());
        
        if (connectionProvider.isConnected()) {
            logger.trace("Connect to LDAP");
            connectionProvider.closeConnectionPool();
            return true;
        }
       
        return false;
    }

    @NotNull
    private FileConfiguration loadFileConfiguration() {
        try {
            return new FileConfiguration(ConfigurationFactory.getAppPropertiesFile());
        } catch (Exception ex) {
            logger.error("Failed to load configuration.", ex);
            return new FileConfiguration(LdapEntryManagerFactory.LDAP_DEFAULT_PROPERTIES_FILE);
        }
    }

    private static List<String> getServers(GluuLdapConfiguration ldapConfiguration) {
        List<String> servers = new ArrayList<>();
        for (SimpleProperty server : ldapConfiguration.getServers())
            servers.add(server.getValue());
        return servers;
    }

    private static String buildServersString(List<String> servers) {
        if (servers == null) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String server : servers) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }

            sb.append(server);
        }

        return sb.toString();
    }
}
