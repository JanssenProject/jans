package io.jans.configapi.plugin.kc.link.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.keycloak.link.model.config.AppConfiguration;
import io.jans.keycloak.link.model.config.Conf;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class KcLinkConfigService {

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
    
    public String getKcLinkDn() {
        String dn = this.getBaseConfiguration().getString("keycloakLink_ConfigurationEntryDN");
        logger.info(" kcLinkDn:{}", dn);
        return dn;
    }

    // Config handling methods
    public Conf findKcLinkConf() {
        final String dn = getKcLinkDn();
        logger.info(" dn:{}", dn);
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("Kc Link Configuration DN is undefined!");
        }

        Conf kcLinkconf = persistenceManager.find(dn, Conf.class, null);
        logger.info(" kcLinkconf:{}", kcLinkconf);

        return kcLinkconf;
    }

    public void mergeKcLinkConfig(Conf kcLinkconf) {
        kcLinkconf.setRevision(kcLinkconf.getRevision() + 1);
        persistenceManager.merge(kcLinkconf);
    }

    public AppConfiguration find() {
        return findKcLinkConf().getDynamic();
    }

    
}
