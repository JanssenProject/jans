package io.jans.configapi.plugin.scim.service;

import io.jans.scim.model.conf.Conf;
import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
//import io.jans.configapi.plugin.scim.model.config.ScimAppConfiguration;
//import io.jans.configapi.plugin.scim.model.config.ScimConf;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigService {

    @Inject
    Logger log;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ScimConfigurationFactory scimConfigurationFactory;

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamicConf();
    }
    public Conf findConf() {
        final String dn = scimConfigurationFactory.getScimConfigurationDn();
        return persistenceManager.find(dn, Conf.class, null);
    }

    public void merge(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }
    
    /*
    public AppConfiguration findConf() {
        final String dn = scimConfigurationFactory.getScimConfigurationDn();
        log.debug("\n\n ScimConfigService::findConf() - dn:{} ", dn);
        return persistenceManager.find(dn, AppConfiguration.class, null);
    }

    public void merge(AppConfiguration appConfiguration) {
        conf.setRevision(appConfiguration.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public AppConfiguration find() {
        final AppConfiguration conf = findConf();
       log.debug(
               "\n\n ScimConfigService::find() - new - conf.getDn:{}, conf.getDynamicConf:{}, conf.getStaticConf:{}, conf.getRevision:{}",
               conf.getDn(), conf.getDynamicConf(), conf.getStaticConf(), conf.getRevision());
        return conf;
    }
    */

}
