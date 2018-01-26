package org.xdi.oxd.server.persistence;

import com.google.common.collect.Sets;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.service.MigrationService;
import org.xdi.oxd.server.service.Rp;
import org.xdi.service.cache.AbstractRedisProvider;
import org.xdi.service.cache.RedisConfiguration;
import org.xdi.service.cache.RedisProviderFactory;

import java.io.IOException;
import java.util.Set;

/**
 * @author yuriyz
 */
public class RedisPersistenceService implements PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPersistenceService.class);

    private final Configuration configuration;
    private AbstractRedisProvider redisProvider;

    public RedisPersistenceService(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void create() {
        LOG.debug("Creating RedisPersistenceService ...");

        try {
            RedisConfiguration redisConfiguration = asRedisConfiguration(configuration.getStorageConfiguration());

            redisProvider = RedisProviderFactory.create(redisConfiguration);
            redisProvider.create();
            LOG.debug("RedisPersistenceService started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting RedisPersistenceService", e);
        }
    }

    @Override
    public boolean create(Rp rp) {
        try {
            put(rp.getOxdId(), Jackson.asJson(rp));
            return true;
        } catch (IOException e) {
            LOG.error("Failed to create RP: " + rp, e);
            return false;
        }
    }

    @Override
    public boolean update(Rp rp) {
        try {
            put(rp.getOxdId(), Jackson.asJson(rp));
            return true;
        } catch (IOException e) {
            LOG.error("Failed to create RP: " + rp, e);
            return false;
        }
    }

    @Override
    public Rp getRp(String oxdId) {
        return MigrationService.parseRp(get(oxdId));
    }

    @Override
    public boolean removeAllRps() {
        return false;
    }

    @Override
    public Set<Rp> getRps() {
        return Sets.newHashSet();
    }

    @Override
    public void destroy() {
        LOG.debug("Destroying RedisProvider");

        redisProvider.destroy();

        LOG.debug("Destroyed RedisProvider");
    }

    @Override
    public boolean remove(String oxdId) {
        redisProvider.remove(oxdId);
        return true;
    }

    private void testConnection() {
        put("testKey", "testValue");
        if (!"testValue".equals(get("testKey"))) {
            throw new RuntimeException("Failed to connect to redis server. Storage configuration: " + configuration.getStorageConfiguration());
        }
    }

    public void put(String key, String value) {
        redisProvider.put(key, value);
    }

    public String get(String key) {
        return (String) redisProvider.get(key);
    }

    public static RedisConfiguration asRedisConfiguration(Configuration configuration) throws Exception {
        return asRedisConfiguration(configuration.getStorageConfiguration());
    }

    public static RedisConfiguration asRedisConfiguration(JsonNode node) throws Exception {
        try {
            return CoreUtils.createJsonMapper().readValue(node, RedisConfiguration.class);
        } catch (Exception e) {
            LOG.error("Failed to parse RedisConfiguration.", e);
            throw e;
        }
    }
}
