package org.xdi.service.cache;

import net.spy.memcached.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuriyz on 02/02/2017.
 */
public class MemcachedProvider extends AbstractCacheProvider<MemcachedClient> {

    private static final Logger log = LoggerFactory.getLogger(MemcachedProvider.class);

    private MemcachedConfiguration memcachedConfiguration;

    public MemcachedProvider(MemcachedConfiguration memcachedConfiguration) {
        this.memcachedConfiguration = memcachedConfiguration;
    }

    private MemcachedClient client;

    public void create() {
        log.debug("Starting MemcachedProvider ...");
        try {
            final ConnectionFactory connectionFactory;
            if (memcachedConfiguration.getConnectionFactoryType() == MemcachedConnectionFactoryType.BINARY) {
                connectionFactory = new BinaryConnectionFactory(memcachedConfiguration.getMaxOperationQueueLength(), memcachedConfiguration.getBufferSize());
            } else {
                connectionFactory = new DefaultConnectionFactory(memcachedConfiguration.getMaxOperationQueueLength(), memcachedConfiguration.getBufferSize());
            }

            client = new MemcachedClient(connectionFactory, AddrUtil.getAddresses(memcachedConfiguration.getServers()));
            log.debug("MemcachedProvider started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting MemcachedProvider", e);
        }
    }

    public void destroy() {
        log.debug("Destroying MemcachedProvider");

        try {
            client.shutdown();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Error destroying MemcachedProvider", e);
        }
    }

    @Override
    public MemcachedClient getDelegate() {
        return client;
    }

    @Override
    public Object get(String region, String key) {
        try {
            return client.get(key);
        } catch (Exception e) {
            log.error("Failed to fetch object by key: " + key, e);
            return null;
        }
    }

    @Override // it is so weird but we use as workaround "region" field to pass "expiration" for put operation
    public void put(String expirationInSeconds, String key, Object object) {
        try {
            client.set(key, putExpiration(expirationInSeconds), object);
        } catch (Exception e) {
            log.error("Failed to put object in cache, key: " + key, e);
        }
    }

    private int putExpiration(String expirationInSeconds) {
        try {
            return Integer.parseInt(expirationInSeconds);
        } catch (Exception e) {
            return memcachedConfiguration.getDefaultPutExpiration();
        }
    }

    @Override
    public void remove(String region, String key) {
        try {
            client.delete(key);
        } catch (Exception e) {
            log.error("Failed to remove object from cache, key: " + key, e);
        }
    }

    @Override
    public void clear() {
        client.flush();
    }
}
