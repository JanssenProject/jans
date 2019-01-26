package org.xdi.service.cache;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * @author yuriyz on 02/21/2017.
 */

@ApplicationScoped
public class InMemoryCacheProvider extends AbstractCacheProvider<ExpiringMap> {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryCacheProvider.class);

    @Inject
    private CacheConfiguration cacheConfiguration;

    private ExpiringMap<String, Object> map = ExpiringMap.builder().build();

    private InMemoryConfiguration inMemoryConfiguration;

    public InMemoryCacheProvider() {
    }

    @PostConstruct
    public void init() {
        this.inMemoryConfiguration = cacheConfiguration.getInMemoryConfiguration();
    }

    public void create() {
        LOG.debug("Starting InMemoryCacheProvider ...");
        try {
            map = ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).variableExpiration().build();

            LOG.debug("InMemoryCacheProvider started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting InMemoryCacheProvider", e);
        }
    }

    @PreDestroy
    public void destroy() {
        LOG.debug("Destroying InMemoryCacheProvider");

        map.clear();

        LOG.debug("Destroyed InMemoryCacheProvider");
    }

    @Override
    public ExpiringMap getDelegate() {
        return map;
    }

    @Override
    public Object get(String region, String key) {
        return map.get(key);
    }

    @Override // it is so weird but we use as workaround "region" field to pass "expiration"
              // for put operation
    public void put(String expirationInSeconds, String key, Object object) {
        // if key already exists and hash is the same for value then expiration time is
        // not updated
        // net.jodah.expiringmap.ExpiringMap.putInternal()
        // therefore we first remove entry and then put it
        map.remove(key);
        map.put(key, object, ExpirationPolicy.CREATED, putExpiration(expirationInSeconds), TimeUnit.SECONDS);
    }

    private int putExpiration(String expirationInSeconds) {
        try {
            return Integer.parseInt(expirationInSeconds);
        } catch (Exception e) {
            return inMemoryConfiguration.getDefaultPutExpiration();
        }
    }

    @Override
    public void remove(String region, String key) {
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.IN_MEMORY;
    }

}
