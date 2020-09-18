package org.gluu.configapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.gluu.model.SimpleProperty;
import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.PropertiesDecrypter;
import org.gluu.configapi.configuration.ConfigurationFactory;

@ApplicationScoped
public class ConnectionStatus {

  @Inject
  Logger logger;

  @Inject
  ConfigurationFactory configurationFactory;

  public boolean isUp(GluuLdapConfiguration ldapConfiguration) {
    Properties properties = new Properties();
    FileConfiguration configuration = loadFileConfiguration();
    if (configuration != null)
      properties = configuration.getProperties();
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

  private FileConfiguration loadFileConfiguration() {
    FileConfiguration configuration = null;
    try {
      configuration = new FileConfiguration(ConfigurationFactory.getAppPropertiesFile());
    } catch (Exception ex) {
      logger.error("ConnectionStatus:::loadFileConfiguration() - ****Exception**** = " + ex);
      configuration = new FileConfiguration(LdapEntryManagerFactory.LDAP_DEFAULT_PROPERTIES_FILE);
    }
    return configuration;
  }

  private List<String> getServers(GluuLdapConfiguration ldapConfiguration) {
    List<String> servers = new ArrayList<String>();
    for (SimpleProperty server : ldapConfiguration.getServers())
      servers.add(server.getValue());
    return servers;
  }

  private String buildServersString(List<String> servers) {
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
