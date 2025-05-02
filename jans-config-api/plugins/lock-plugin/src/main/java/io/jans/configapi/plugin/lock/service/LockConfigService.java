package io.jans.configapi.plugin.lock.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.Conf;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class LockConfigService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;
          
    
    public FileConfiguration getBaseConfiguration() {
        logger.info(" configurationFactory.getBaseConfiguration():{}", configurationFactory.getBaseConfiguration());
        return configurationFactory.getBaseConfiguration();
    }
    
    public String getLockDn() {
        String dn = this.getBaseConfiguration().getString("lock_ConfigurationEntryDN");
        logger.info(" lockDn:{}", dn);
        return dn;
    }

    // Config handling methods
    public Conf findLockConf() {
        final String dn = getLockDn();
        logger.info(" dn:{}", dn);
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("Lock Configuration DN is undefined!");
        }

        Conf lockconf = persistenceManager.find(dn, Conf.class, null);
        logger.info(" lockconf:{}", lockconf);

        return lockconf;
    }

    public void mergeLockConfig(Conf lockconf) {
        lockconf.setRevision(lockconf.getRevision() + 1);
        persistenceManager.merge(lockconf);
    }

    public AppConfiguration find() {
        return findLockConf().getDynamic();
    }

    
}
