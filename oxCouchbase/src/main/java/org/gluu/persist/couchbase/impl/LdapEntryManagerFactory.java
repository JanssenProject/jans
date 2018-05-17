package org.gluu.persist.couchbase.impl;

import java.util.Properties;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;

/**
 * LDAP Netry Manager Factory
 *
 * @author Yuriy Movchan Date: 02/02/2018
 */
public class LdapEntryManagerFactory implements PersistenceEntryManagerFactory {

    @Override
    public PersistenceEntryManager createEntryManager(Properties conf) {
        // TODO Auto-generated method stub
        return null;
    }
/*
    private static final Logger LOG = LoggerFactory.getLogger(LdapEntryManager.class);

    @Override
    public LdapEntryManager createEntryManager(Properties conf) {
        LdapConnectionProvider connectionProvider = new LdapConnectionProvider(conf);
        if (!connectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create LDAP connection pool! Result code: '%s'", connectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created connectionProvider '{}' with code '{}'", connectionProvider, connectionProvider.getCreationResultCode());

        LdapConnectionProvider bindConnectionProvider = new LdapAuthConnectionProvider(conf);
        if (!bindConnectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create LDAP bind connection pool! Result code: '%s'", bindConnectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created bindConnectionProvider '{}' with code '{}'", bindConnectionProvider, bindConnectionProvider.getCreationResultCode());

        LdapEntryManager ldapEntryManager = new LdapEntryManager(new LdapOperationsServiceImpl(connectionProvider, bindConnectionProvider));
        LOG.info("Created LdapEntryManager: {}", ldapEntryManager.getOperationService());

        return ldapEntryManager;
    }
*/
}
