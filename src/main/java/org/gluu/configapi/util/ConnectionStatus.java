package org.gluu.configapi.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import javax.inject.Inject;
import javax.enterprise.inject.Default;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.gluu.configapi.configuration.ConfigurationFactory;
import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.model.SimpleProperty;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.PropertiesDecrypter;

@Default
public class ConnectionStatus {
  
  @Inject
  private ConfigurationFactory configurationFactory;

  public boolean isUp(GluuLdapConfiguration ldapConfiguration) {
    System.out.println("\n\n\n ConnectionStatus:::isUp() - ldapConfiguration = "+ldapConfiguration+"\n\n\n");
    //FileConfiguration configuration = loadFileConfiguration();
   // System.out.println("\n\n\n ConnectionStatus:::isUp() - configuration = "+configuration+"\n\n\n");
    //Properties properties = configuration.getProperties();
    Properties properties = System.getProperties();
    properties.setProperty("bindDN", ldapConfiguration.getBindDN());
    properties.setProperty("bindPassword", ldapConfiguration.getBindPassword());
    properties.setProperty("servers", buildServersString(getServers(ldapConfiguration)));
    properties.setProperty("useSSL", Boolean.toString(ldapConfiguration.isUseSSL()));
    System.out.println("\n\n\n ConnectionStatus:::isUp() - configurationFactory = "+configurationFactory+"\n\n\n");
    System.out.println("\n\n\n ConnectionStatus:::isUp() - configurationFactory.getBaseConfiguration() = "+configurationFactory.getBaseConfiguration()+"\n\n\n");
    LdapConnectionProvider connectionProvider = new LdapConnectionProvider(PropertiesDecrypter.decryptProperties(properties, configurationFactory.getCryptoConfigurationSalt()));
    System.out.println("\n\n\n ConnectionStatus:::isUp() - connectionProvider = "+connectionProvider+"\n\n\n");
    if (connectionProvider.getConnectionPool() != null) {
      boolean isConnected = connectionProvider.isConnected();
      System.out.println("\n\n\n ConnectionStatus:::isUp() - isConnected_1 = "+isConnected+"\n\n\n");
      connectionProvider.closeConnectionPool();
      System.out.println("\n\n\n ConnectionStatus:::isUp() - isConnected_2 = "+isConnected+"\n\n\n");
      return isConnected;
     }
     return false;
  }
  
  private List<String> getServers(GluuLdapConfiguration ldapConfiguration){
    System.out.println("\n\n\n ConnectionStatus:::getServers() - ldapConfiguration.getServers() = "+ldapConfiguration.getServers()+"\n\n\n");
    List<String> servers = new ArrayList<String>();
    for(SimpleProperty server : ldapConfiguration.getServers())
      servers.add(server.getValue());  
    System.out.println("\n\n\n ConnectionStatus:::getServers() - servers = "+servers+"\n\n\n");
    return servers; 
  }

  /*
  private FileConfiguration loadFileConfiguration() {
    System.out.println("\n\n\n ConnectionStatus:::loadFileConfiguration() - Entry \n\n\n");
    FileConfiguration configuration = new FileConfiguration(ConfigurationFactory.LDAP_PROPERTIES_FILE);
    System.out.println("\n\n\n ConnectionStatus:::loadFileConfiguration() - configuration = "+configuration+" \n\n\n");
    if (!configuration.isLoaded()) {
      configuration = new FileConfiguration(LdapEntryManagerFactory.LDAP_DEFAULT_PROPERTIES_FILE);
    }
    System.out.println("\n\n\n ConnectionStatus:::loadFileConfiguration() - configuration = "+configuration+" \n\n\n");
    return configuration;
  }*/
  
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
