/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginConfiguration;
import io.jans.orm.PersistenceEntryManager;

import io.jans.service.document.store.provider.DocumentStoreProviderFactory;
import io.jans.service.document.store.service.DocumentStoreService;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.provider.DocumentStore;
import io.jans.service.document.store.provider.DocumentStoreProvider;

import io.jans.staging.adapter.DefaultFileStorageLayout;
import io.jans.staging.adapter.DocumentStoreContentStore;
import io.jans.staging.FileStagingService;
import io.jans.staging.adapter.SystemTimeSource;
import io.jans.staging.adapter.UuidTokenGenerator;

import io.jans.staging.persistence.StagedFileRepositoryImpl;
import io.jans.staging.Destination;
import io.jans.staging.port.ContentStore;
import io.jans.staging.port.FileStorageLayout;
import io.jans.staging.port.StagedFileRepository;
import io.jans.staging.port.TimeSource;
import io.jans.staging.port.TokenGenerator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;

@ApplicationScoped
@Named("shibbolethPluginAppInitializer")
public class ShibbolethPluginAppInitializer {

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private PersistenceEntryManager persistenceEntryManagerInstance;

    @Inject
    DocumentStoreProviderFactory documentStoreProviderFactory;

    @Inject
    private DocumentStoreService documentStoreService;

    @Inject
    ShibbolethPluginConfigurationFactory shibbolethPluginConfigurationFactory;

    @Inject
    ShibbolethPluginConfiguration shibbolethPluginConfiguration;

    public void onAppStart() {
        log.info("=============  Initializing Shibboleth Plugin ========================");

        // configuration
        this.shibbolethPluginConfigurationFactory.create();

        log.info("==============  Shibboleth Plugin IS UP AND RUNNING ===================");
    }

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  Shibboleth Plugin STOPPED  ==========================");
        log.info("init:{}", init);
        log.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public ShibbolethPluginConfigurationFactory getShibbolethPluginConfigurationFactory() {
        return shibbolethPluginConfigurationFactory;
    }

    @Produces
    @ApplicationScoped
    public DocumentStoreType getProviderType() {
        return documentStoreService.getProviderType();
    }

    @Produces
    @ApplicationScoped
    public DocumentStoreProvider getDocumentStoreProvider() {
        return documentStoreProviderFactory.getDocumentStoreProvider();
    }

    @Produces
    @ApplicationScoped
    public StagedFileRepository getStagedFileRepository() {
        return new StagedFileRepositoryImpl(persistenceEntryManagerInstance, this.getStagedFilesBase());
    }

    @Produces
    @ApplicationScoped
    public DocumentStoreContentStore getDocumentStoreContentStore() {
        return new DocumentStoreContentStore(getDocumentStoreProvider());
    }

    @Produces
    @ApplicationScoped
    public Destination getDestination() {
        return Destination.of(getStagedFilesBase()).getValue();
    }

    @Produces
    @ApplicationScoped
    public DefaultFileStorageLayout getDefaultFileStorageLayout() {
        return DefaultFileStorageLayout.withDefaults(Destination.of(getShibbolethMetadataDir()).getValue());
    }

    @Produces
    @ApplicationScoped
    public TimeSource getTimeSource() {
        return new SystemTimeSource();
    }

    @Produces
    @ApplicationScoped
    public TokenGenerator getTokenGenerator() {
        return new UuidTokenGenerator();
    }

    @Produces
    @ApplicationScoped
    public Duration getDuration() {
        return Duration.ofMinutes(getTokenDuration());
    }

    @Produces
    @ApplicationScoped
    public FileStagingService getFileStagingService() {
        return FileStagingService.create(getStagedFileRepository(), getDocumentStoreContentStore(),
                getDefaultFileStorageLayout(), getTimeSource(), getTokenGenerator(), getDuration()).getValue();
    }

    /* Helper methods */

    private String getShibbolethMetadataDir() {
        return shibbolethPluginConfiguration.getShibbolethMetadataDir();
    }

    private String getStagedFilesBase() {
        return shibbolethPluginConfiguration.getStagedFilesBase();
    }

    private int getTokenDuration() {
        return shibbolethPluginConfiguration.getTokenDuration();
    }

}
