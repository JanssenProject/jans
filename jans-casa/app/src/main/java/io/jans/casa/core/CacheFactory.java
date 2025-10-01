package io.jans.casa.core;

import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.jans.service.cache.*;
import io.jans.util.security.StringEncrypter;

import org.slf4j.Logger;

import static io.jans.service.cache.CacheProviderType.*;

@ApplicationScoped
public class CacheFactory {

    private CacheProvider storeService;

    @Inject
    private Logger logger;

    @Inject
    private PersistenceService persistenceService;

    @Produces
    @ApplicationScoped
    public CacheProvider getCacheProvider() {

        //Initialize only upon first usage
        if (storeService == null) {

            CacheConfiguration cacheConfiguration = persistenceService.getCacheConfiguration();
            StringEncrypter stringEncrypter = persistenceService.getStringEncrypter();
            StandaloneCacheProviderFactory scpf = new StandaloneCacheProviderFactory(persistenceService.getEntryManager(), stringEncrypter);
            CacheProviderType type = Optional.ofNullable(cacheConfiguration).map(CacheConfiguration::getCacheProviderType)
                    .orElse(null);

            if (type != null) {
                try {
                    logger.info("Initializing store of type = {}", type);
                    storeService = scpf.getCacheProvider(cacheConfiguration);
                    logger.info("Store created");
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.warn("Cache store configuration missing!");
            }

            if (storeService == null) {
                //Try to use in-memory
                InMemoryCacheProvider imc = new InMemoryCacheProvider();
                imc.configure(cacheConfiguration);
                imc.init();
                imc.create();

                storeService = imc;
                logger.info("Defaulting to {} cache store", IN_MEMORY);
            }
        }
        return storeService;

    }

}
