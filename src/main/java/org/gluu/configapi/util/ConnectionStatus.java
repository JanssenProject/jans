package org.gluu.configapi.util;

import org.gluu.configapi.configuration.ConfigurationFactory;
import org.gluu.model.SimpleProperty;
import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.PropertiesDecrypter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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

        Properties properties = configuration.getProperties();
        properties.setProperty("bindDN", ldapConfiguration.getBindDN());
        properties.setProperty("bindPassword", ldapConfiguration.getBindPassword());
        properties.setProperty("servers", buildServersString(getServers(ldapConfiguration)));
        properties.setProperty("useSSL", Boolean.toString(ldapConfiguration.isUseSSL()));

        LdapConnectionProvider connectionProvider = new LdapConnectionProvider(
                PropertiesDecrypter.decryptProperties(properties, configurationFactory.getCryptoConfigurationSalt()));

        if (connectionProvider.getConnectionPool() != null) {
            boolean isConnected = connectionProvider.isConnected();
            connectionProvider.closeConnectionPool();
            return isConnected;
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
