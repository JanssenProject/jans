/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import net.spy.memcached.*;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author yuriyz on 02/02/2017.
 */
@ApplicationScoped
public class MemcachedProvider extends AbstractCacheProvider<MemcachedClient> {

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    private MemcachedConfiguration memcachedConfiguration;

    public MemcachedProvider() {
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    @PostConstruct
    public void init() {
        this.memcachedConfiguration = cacheConfiguration.getMemcachedConfiguration();
    }

    private MemcachedClient client;

    public void create() {
        log.debug("Starting MemcachedProvider ...");
        try {
            final ConnectionFactory connectionFactory;
            if (memcachedConfiguration.getConnectionFactoryType() == MemcachedConnectionFactoryType.BINARY) {
                connectionFactory = new BinaryConnectionFactory(memcachedConfiguration.getMaxOperationQueueLength(),
                        memcachedConfiguration.getBufferSize());
            } else {
                connectionFactory = new DefaultConnectionFactory(memcachedConfiguration.getMaxOperationQueueLength(),
                        memcachedConfiguration.getBufferSize());
            }

            client = new MemcachedClient(connectionFactory, AddrUtil.getAddresses(memcachedConfiguration.getServers()));
            testConnection();
            log.debug("MemcachedProvider started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting MemcachedProvider", e);
        }
    }

	public void configure(CacheConfiguration cacheConfiguration) {
		this.log = LoggerFactory.getLogger(MemcachedProvider.class);
		this.cacheConfiguration = cacheConfiguration;
	}

    private void testConnection() {
        put(2, "connectionTest", "connectionTestValue");
        if (!"connectionTestValue".equals(get("connectionTest"))) {
            throw new IllegalStateException("Error starting MemcachedProvider. Please check memcached configuration: " + memcachedConfiguration);
        }
    }

    public boolean isConnected() {
        put(2, "connectionTest", "connectionTestValue");
        if (!"connectionTestValue".equals(get("connectionTest"))) {
         return false;
        }
        return true;
    }

    @PreDestroy
    public void destroy() {
        log.debug("Destroying MemcachedProvider");

        try {
            client.shutdown();
            log.debug("Destroyed MemcachedProvider");
        } catch (RuntimeException e) {
            throw new IllegalStateException("Error destroying MemcachedProvider", e);
        }
    }

    @Override
    public MemcachedClient getDelegate() {
        return client;
    }

	@Override
	public boolean hasKey(String key) {
		Object value = get(key);
		
		return value != null;
	}

    @Override
    public Object get(String key) {
        try {
            if (key == null) {
                return null;
            }
            Object o = client.get(key);
            log.trace("get - key: " + key + ", value:" + o);
            return o;
        } catch (Exception e) {
            log.error("Failed to fetch object by key: " + key, e);
            return null;
        }
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        try {
            int expiration = expirationInSeconds > 0 ? expirationInSeconds : memcachedConfiguration.getDefaultPutExpiration();
            OperationFuture<Boolean> set = client.set(key, expiration, object);
            OperationStatus status = set.getStatus(); // block
            log.trace("set - key:" + key + ", expiration: " + expiration + ", status:" + status + ", get:" + get(key));
        } catch (Exception e) {
            log.error("Failed to put object in cache, key: " + key, e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            client.delete(key);
            log.trace("delete - key:" + key);
        } catch (Exception e) {
            log.error("Failed to remove object from cache, key: " + key, e);
        }
    }

    @Override
    public void clear() {
        client.flush();
        log.trace("cleared");
    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.MEMCACHED;
    }

}
