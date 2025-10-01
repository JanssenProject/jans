/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.config.GluuConfiguration;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.model.PersistenceConfiguration;
import io.jans.configapi.model.status.StatsData;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.util.StringHelper;

import io.jans.orm.model.PersistenceMetadata;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ConfigurationService {
    
    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    private StatsData statsData;

    public Conf findConf() {
        final String dn = configurationFactory.getAuthConfigurationDn();
        return persistenceManager.find(dn, Conf.class, null);
    }

    public void merge(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public void merge(GluuConfiguration conf) {
        persistenceManager.merge(conf);
    }

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamic();
    }

    public GluuConfiguration findGluuConfiguration() {
        String configurationDn = findConf().getStatics().getBaseDn().getConfiguration();
        if (StringHelper.isEmpty(configurationDn)) {
            return null;
        }
        return persistenceManager.find(GluuConfiguration.class, configurationDn);
    }
    
    public DocumentStoreConfiguration getDocumentStoreConfiguration() {
        GluuConfiguration gluuConfiguration = this.findGluuConfiguration();
        logger.info("gluuConfiguration:{}",gluuConfiguration);
        DocumentStoreConfiguration documentStoreConfiguration = null;
        if(gluuConfiguration == null) {
            throw new WebApplicationException("Cannot fetch DocumentStoreConfiguration as GluuConfiguration is null! ");
        }
        
        documentStoreConfiguration = gluuConfiguration.getDocumentStoreConfiguration();
        logger.info("Fetched documentStoreConfiguration:{}",documentStoreConfiguration);
        return documentStoreConfiguration;
    }
    
    public DocumentStoreConfiguration updateDocumentStoreConfiguration(DocumentStoreConfiguration documentStoreConfiguration) {
        logger.info("documentStoreConfiguration:{}",documentStoreConfiguration);
        if(documentStoreConfiguration == null) {
            return documentStoreConfiguration;
        }
        
        GluuConfiguration gluuConfiguration = findGluuConfiguration();
        logger.info("gluuConfiguration:{}",gluuConfiguration);
        
        if(gluuConfiguration==null) {
            throw new WebApplicationException("Cannot update DocumentStoreConfiguration as GluuConfiguration is null! ");
        }
        
        gluuConfiguration.setDocumentStoreConfiguration(documentStoreConfiguration);
        merge(gluuConfiguration);
        
        documentStoreConfiguration = gluuConfiguration.getDocumentStoreConfiguration();
        logger.info("Updated documentStoreConfiguration:{}",documentStoreConfiguration);
        return documentStoreConfiguration;
    }
    

    public String getPersistenceType() {
        return configurationFactory.getBaseConfiguration().getString("persistence.type");
    }

    public StatsData getStatsData() {
        return statsData;
    }

    public void setStatsData(StatsData statsData) {
        this.statsData = statsData;
    }    
  
    public boolean isLowercaseFilter(String baseDn) {        
        return !PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceManager.getPersistenceType(baseDn));
    }
    
    public String getRevokeUrl() {
        return configurationFactory.getApiAppConfiguration().getAuthOpenidRevokeUrl();
    }
    
    public PersistenceConfiguration getPersistenceMetadata() {
         PersistenceMetadata persistenceMetadata  = persistenceManager.getPersistenceMetadata("o=jans");
         return getPersistenceConfiguration(persistenceMetadata);
    }
    
    private PersistenceConfiguration getPersistenceConfiguration(PersistenceMetadata persistenceMetadata) {
        PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration();
        if(persistenceMetadata == null) {
            return persistenceConfiguration;

        }
        persistenceConfiguration.setDatabaseName(persistenceMetadata.getDatabaseName());
        persistenceConfiguration.setSchemaName(persistenceMetadata.getSchemaName());
        persistenceConfiguration.setProductName(persistenceMetadata.getProductName());
        persistenceConfiguration.setProductVersion(persistenceMetadata.getProductVersion());
        persistenceConfiguration.setDriverName(persistenceMetadata.getDriverName());
        persistenceConfiguration.setDriverVersion(persistenceMetadata.getDriverVersion());
        return persistenceConfiguration;
    }
}
