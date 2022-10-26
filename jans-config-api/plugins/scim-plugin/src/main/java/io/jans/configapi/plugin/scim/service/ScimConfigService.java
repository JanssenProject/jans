package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.scim.model.conf.Conf;

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

    public Conf findConf() {
        final String dn = scimConfigurationFactory.getScimConfigurationDn();
        log.debug("\n\n ScimConfigService::findConf() - dn:{} ", dn);
        return persistenceManager.find(dn, Conf.class, null);
    }

    public void merge(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public AppConfiguration find() {
        final Conf conf = findConf();
        log.debug(
                "\n\n ScimConfigService::find() - new - conf.getDn:{}, conf.getDynamicConf:{}, conf.getRevision:{}",
                conf.getDn(), conf.getDynamicConf(), conf.getRevision());
        return conf.getDynamicConf();
    }

}
