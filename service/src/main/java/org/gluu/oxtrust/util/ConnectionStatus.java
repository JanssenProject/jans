package org.gluu.oxtrust.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.service.config.ConfigurationFactory;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.PropertiesDecrypter;

@ApplicationScoped
public class ConnectionStatus {

	@Inject
	private ConfigurationFactory<?> configurationFactory;

	public boolean isUp(LdapConnectionData ldapConnectionData) {
		FileConfiguration configuration = loadFileConfiguration();
		Properties properties = configuration.getProperties();
		properties.setProperty("bindDN", ldapConnectionData.getBindDN());
		properties.setProperty("bindPassword", ldapConnectionData.getBindPassword());
		properties.setProperty("servers", buildServersString(ldapConnectionData.getServers()));
		properties.setProperty("useSSL", Boolean.toString(ldapConnectionData.isUseSSL()));

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
		FileConfiguration configuration = new FileConfiguration(ConfigurationFactory.APP_PROPERTIES_FILE);
		if (!configuration.isLoaded()) {
			configuration = new FileConfiguration(LdapEntryManagerFactory.LDAP_DEFAULT_PROPERTIES_FILE);
		}
		return configuration;
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
