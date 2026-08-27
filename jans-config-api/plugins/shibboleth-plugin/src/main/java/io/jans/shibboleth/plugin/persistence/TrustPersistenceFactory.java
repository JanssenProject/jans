package io.jans.shibboleth.plugin.persistence;

import io.jans.orm.PersistenceEntryManager;
import io.jans.shibboleth.plugin.config.model.ShibbolethPluginConfiguration;
import io.jans.shibboleth.trust.activation.repository.LeaseRepository;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.persistence.activation.LeaseRepositoryImpl;
import io.jans.shibboleth.trust.persistence.activation.WorkItemRepositoryImpl;
import io.jans.shibboleth.trust.persistence.activation.WorkerRepositoryImpl;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipRepository;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class TrustPersistenceFactory {
    
    @Inject
    private ShibbolethPluginConfiguration pluginConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public void setPluginConfiguration(ShibbolethPluginConfiguration pluginConfiguration) {

        this.pluginConfiguration = pluginConfiguration;
    }

    public ShibbolethPluginConfiguration getPluginConfiguration() {

        return pluginConfiguration;
    }

    public void setPersistenceEntryManager(PersistenceEntryManager persistenceEntryManager) {

        this.persistenceEntryManager = persistenceEntryManager;
    }

    public PersistenceEntryManager getPersistenceEntryManager() {

        return persistenceEntryManager;
    }


    @Produces
    @ApplicationScoped
    public TrustRelationshipRepository createTrustRelationshipRepository() {

        return new TrustRelationshipRepositoryImpl(persistenceEntryManager,pluginConfiguration.getTrustRelationshipsDn());
    }

    @Produces
    @ApplicationScoped
    public LeaseRepository createLeaseRepository() {

        return new LeaseRepositoryImpl(persistenceEntryManager,pluginConfiguration.getTrustActivationLeasesDn());
    }

    @Produces
    @ApplicationScoped
    public WorkerRepository createWorkerRepository() {

        return new WorkerRepositoryImpl(persistenceEntryManager,pluginConfiguration.getTrustActivationWorkersDn());
    }

    @Produces
    @ApplicationScoped
    public WorkItemRepository createWorkItemRepository() {

        return new WorkItemRepositoryImpl(persistenceEntryManager, pluginConfiguration.getTrustActivationWorkItemsDn(), pluginConfiguration.getTrustActivationEpisodesDn());
    }

}
