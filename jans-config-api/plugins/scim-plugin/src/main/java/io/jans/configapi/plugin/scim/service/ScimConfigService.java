package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
import io.jans.configapi.plugin.scim.model.config.ScimAppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConf;
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

    public ScimConf findConf() {
        final String dn = scimConfigurationFactory.getScimConfigurationDn();
        log.debug("\n\n ScimConfigService::findConf() - dn:{} ", dn);
        return persistenceManager.find(dn, ScimConf.class, null);
    }

    public void merge(ScimConf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public ScimAppConfiguration find() {
        final ScimConf conf = findConf();
        log.debug(
                "\n\n ScimConfigService::find() - new - conf.getDn:{}, conf.getDynamicConf:{}, conf.getStaticConf:{}, conf.getRevision:{}",
                conf.getDn(), conf.getDynamicConf(), conf.getStaticConf(), conf.getRevision());
        return conf.getDynamicConf();
    }

}
