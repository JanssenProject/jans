package org.xdi.service.cache;

import net.spy.memcached.*;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;

/**
 * @author yuriyz on 02/02/2017.
 */
public class MemcachedProvider extends AbstractCacheProvider<MemcachedClient> {

    private static final Log log = Logging.getLog(MemcachedProvider.class);

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
            testConnection();
            log.debug("MemcachedProvider started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting MemcachedProvider", e);
        }
    }

    private void testConnection() {
        put("2", "connectionTest", "connectionTestValue");
        if (!"connectionTestValue".equals(get("connectionTest"))) {
            throw new IllegalStateException("Error starting MemcachedProvider. Please check memcached configuration: " + memcachedConfiguration);
        }
    }

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
            int expiration = putExpiration(expirationInSeconds);
            OperationFuture<Boolean> set = client.set(key, expiration, object);
            OperationStatus status = set.getStatus();// block
            log.trace("set - key:" + key + ", expiration: " + expiration + ", status:" + status + ", get:" + get(key));
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
            log.trace("delete - key:" + key);
        } catch (Exception e) {
            log.error("Failed to remove object from cache, key: " + key, e);
        }
    }

    @Override
    public void clear() {
        client.flush();
    }
}
