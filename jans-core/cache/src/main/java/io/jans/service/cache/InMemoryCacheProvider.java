/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author yuriyz on 02/21/2017.
 */

@ApplicationScoped
public class InMemoryCacheProvider extends AbstractCacheProvider<ExpiringMap> {

    @Inject
    private Logger log;

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
    	log.debug("Starting InMemoryCacheProvider ...");
        try {
            map = ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).variableExpiration().build();

            log.debug("InMemoryCacheProvider started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting InMemoryCacheProvider", e);
        }
    }

	public void configure(CacheConfiguration cacheConfiguration) {
		this.log = LoggerFactory.getLogger(InMemoryCacheProvider.class);
		this.cacheConfiguration = cacheConfiguration;
	}

    @PreDestroy
    public void destroy() {
    	log.debug("Destroying InMemoryCacheProvider");

        map.clear();

        log.debug("Destroyed InMemoryCacheProvider");
    }

    @Override
    public ExpiringMap getDelegate() {
        return map;
    }

	@Override
	public boolean hasKey(String key) {
		return map.containsKey(key);
	}

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        // if key already exists and hash is the same for value then expiration time is
        // not updated
        // net.jodah.expiringmap.ExpiringMap.putInternal()
        // therefore we first remove entry and then put it
        map.remove(key);
        expirationInSeconds = expirationInSeconds >= 0 ? expirationInSeconds : inMemoryConfiguration.getDefaultPutExpiration();
        map.put(key, object, ExpirationPolicy.CREATED, expirationInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void remove(String key) {
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
