package org.gluu.persist.ldap.impl;

import java.util.Properties;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.ldap.operation.impl.LdapAuthConnectionProvider;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.persist.ldap.operation.impl.LdapOperationsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDAP Netry Manager Factory
 *
 * @author Yuriy Movchan Date: 02/02/2018
 */
public class LdapEntryManagerFactory implements PersistenceEntryManagerFactory {

    private static final Logger log = LoggerFactory.getLogger(LdapEntryManager.class);

    @Override
    public LdapEntryManager createEntryManager(Properties conf) {
        LdapConnectionProvider connectionProvider = new LdapConnectionProvider(conf);
        if (!connectionProvider.isCreated()) {
            throw new ConfigurationException(String.format("Failed to create LDAP connection pool! Result code: '%s'", connectionProvider.getCreationResultCode()));
        }
        log.debug("Created connectionProvider '{}' with code '{}'", connectionProvider, connectionProvider.getCreationResultCode());

        LdapConnectionProvider bindConnectionProvider = new LdapAuthConnectionProvider(conf);
        if (!bindConnectionProvider.isCreated()) {
            throw new ConfigurationException(String.format("Failed to create LDAP bind connection pool! Result code: '%s'", bindConnectionProvider.getCreationResultCode()));
        }
        log.debug("Created bindConnectionProvider '{}' with code '{}'", bindConnectionProvider, bindConnectionProvider.getCreationResultCode());

        LdapEntryManager ldapEntryManager = new LdapEntryManager(new LdapOperationsServiceImpl(connectionProvider, bindConnectionProvider));
        log.info("Created LdapEntryManager: {}", ldapEntryManager.getOperationService());

        return ldapEntryManager;
    }

}
