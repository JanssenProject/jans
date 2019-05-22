package org.gluu.persist.ldap.impl;

import java.util.HashMap;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.ldap.operation.impl.LdapAuthConnectionProvider;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.persist.ldap.operation.impl.LdapOperationsServiceImpl;
import org.gluu.persist.service.BaseFactoryService;
import org.gluu.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDAP Entry Manager Factory
 *
 * @author Yuriy Movchan Date: 02/02/2018
 */
@ApplicationScoped
public class LdapEntryManagerFactory implements PersistenceEntryManagerFactory {

    public static final String PERSISTANCE_TYPE = "ldap";
    public static final String LDAP_DEFAULT_PROPERTIES_FILE = "gluu-ldap.properties";

	private static final Logger LOG = LoggerFactory.getLogger(LdapEntryManagerFactory.class);

    @Override
    public String getPersistenceType() {
        return PERSISTANCE_TYPE;
    }

    @Override
    public HashMap<String, String> getConfigurationFileNames() {
    	HashMap<String, String> confs = new HashMap<String, String>();
    	confs.put(PERSISTANCE_TYPE, LDAP_DEFAULT_PROPERTIES_FILE);

    	return confs;
    }

	@Override
    public LdapEntryManager createEntryManager(Properties conf) {
		Properties entryManagerConf = PropertiesHelper.filterProperties(conf, PERSISTANCE_TYPE);

		LdapConnectionProvider connectionProvider = new LdapConnectionProvider(entryManagerConf);
        if (!connectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create LDAP connection pool! Result code: '%s'", connectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created connectionProvider '{}' with code '{}'", connectionProvider, connectionProvider.getCreationResultCode());

        LdapConnectionProvider bindConnectionProvider = new LdapAuthConnectionProvider(entryManagerConf);
        if (!bindConnectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create LDAP bind connection pool! Result code: '%s'", bindConnectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created bindConnectionProvider '{}' with code '{}'", bindConnectionProvider, bindConnectionProvider.getCreationResultCode());

        LdapEntryManager ldapEntryManager = new LdapEntryManager(new LdapOperationsServiceImpl(connectionProvider, bindConnectionProvider));
        LOG.info("Created LdapEntryManager: {}", ldapEntryManager.getOperationService());

        return ldapEntryManager;
    }

	@Override
	public void initStandalone(BaseFactoryService persistanceFactoryService) {}

}
