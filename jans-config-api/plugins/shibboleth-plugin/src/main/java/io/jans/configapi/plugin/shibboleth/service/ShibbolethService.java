package io.jans.configapi.plugin.shibboleth.service;

import io.jans.configapi.plugin.shibboleth.model.ShibbolethIdpConfiguration;
import io.jans.configapi.plugin.shibboleth.model.ShibbolethIdpConfigurationProperties;
import io.jans.configapi.plugin.shibboleth.model.TrustedServiceProvider;
import io.jans.orm.PersistenceEntryManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ShibbolethService {

    private static final String SHIBBOLETH_CONFIG_DN = "ou=shibboleth-idp,ou=configuration,o=jans";

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public ShibbolethIdpConfiguration getConfiguration() {
        logger.debug("Fetching Shibboleth IDP configuration");
        
        try {
            return persistenceEntryManager.find(ShibbolethIdpConfiguration.class, SHIBBOLETH_CONFIG_DN);
        } catch (Exception e) {
            logger.warn("Shibboleth configuration not found, returning defaults");
            return createDefaultConfiguration();
        }
    }

    public void updateConfiguration(ShibbolethIdpConfiguration configuration) {
        logger.info("Updating Shibboleth IDP configuration");
        
        configuration.setRevision(configuration.getRevision() + 1);
        
        if (persistenceEntryManager.contains(SHIBBOLETH_CONFIG_DN, ShibbolethIdpConfiguration.class)) {
            persistenceEntryManager.merge(configuration);
        } else {
            configuration.setDn(SHIBBOLETH_CONFIG_DN);
            persistenceEntryManager.persist(configuration);
        }
    }

    public List<TrustedServiceProvider> getTrustedServiceProviders() {
        logger.debug("Fetching trusted service providers");
        
        ShibbolethIdpConfiguration config = getConfiguration();
        if (config != null && config.getShibbolethIdpProperties() != null) {
            return config.getShibbolethIdpProperties().getTrustedServiceProviders();
        }
        return new ArrayList<>();
    }

    public TrustedServiceProvider getTrustedServiceProvider(String entityId) {
        logger.debug("Fetching trusted service provider: {}", entityId);
        
        List<TrustedServiceProvider> providers = getTrustedServiceProviders();
        for (TrustedServiceProvider provider : providers) {
            if (provider.getEntityId().equals(entityId)) {
                return provider;
            }
        }
        return null;
    }

    public void addTrustedServiceProvider(TrustedServiceProvider serviceProvider) {
        logger.info("Adding trusted service provider: {}", serviceProvider.getEntityId());
        
        ShibbolethIdpConfiguration config = getConfiguration();
        List<TrustedServiceProvider> providers = config.getShibbolethIdpProperties().getTrustedServiceProviders();
        if (providers == null) {
            providers = new ArrayList<>();
            config.getShibbolethIdpProperties().setTrustedServiceProviders(providers);
        }
        providers.add(serviceProvider);
        updateConfiguration(config);
    }

    public void updateTrustedServiceProvider(TrustedServiceProvider serviceProvider) {
        logger.info("Updating trusted service provider: {}", serviceProvider.getEntityId());
        
        ShibbolethIdpConfiguration config = getConfiguration();
        List<TrustedServiceProvider> providers = config.getShibbolethIdpProperties().getTrustedServiceProviders();
        
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getEntityId().equals(serviceProvider.getEntityId())) {
                providers.set(i, serviceProvider);
                break;
            }
        }
        updateConfiguration(config);
    }

    public void deleteTrustedServiceProvider(String entityId) {
        logger.info("Deleting trusted service provider: {}", entityId);
        
        ShibbolethIdpConfiguration config = getConfiguration();
        List<TrustedServiceProvider> providers = config.getShibbolethIdpProperties().getTrustedServiceProviders();
        providers.removeIf(p -> p.getEntityId().equals(entityId));
        updateConfiguration(config);
    }

    private ShibbolethIdpConfiguration createDefaultConfiguration() {
        ShibbolethIdpConfiguration config = new ShibbolethIdpConfiguration();
        config.setDn(SHIBBOLETH_CONFIG_DN);
        config.setRevision(1);
        
        ShibbolethIdpConfigurationProperties props = new ShibbolethIdpConfigurationProperties();
        props.setEnabled(false);
        props.setTrustedServiceProviders(new ArrayList<>());
        props.setMetadataProviders(new ArrayList<>());
        
        config.setShibbolethIdpProperties(props);
        return config;
    }
}
