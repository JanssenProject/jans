package org.xdi.service.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.jboss.seam.annotations.*;
import org.jboss.seam.cache.CacheProvider;
import org.jboss.seam.log.Log;

import static org.jboss.seam.ScopeType.APPLICATION;

/**
 * @author yuriyz on 02/02/2017.
 */
@Name("memcachedProvider")
@Scope(APPLICATION)
@AutoCreate
@Startup
public class MemcachedProvider extends CacheProvider<MemcachedClient> {

    @Logger
    private Log log;

    @In(required = true)
    private MemcachedConfiguration memcachedConfiguration;

    private MemcachedClient client;

    @Create
    public void create() {
        log.debug("Starting MemcachedProvider...");
        try {
            client = new MemcachedClient(
                    new DefaultConnectionFactory(memcachedConfiguration.getMaxOperationQueueLength(), memcachedConfiguration.getBufferSize()),
                    AddrUtil.getAddresses(memcachedConfiguration.getServers()));
            log.debug("MemcachedProvider started");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting MemcachedProvider", e);
        }
    }

    @Destroy
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
            return memcachedConfiguration.getPutExpiration();
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
