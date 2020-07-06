package org.gluu.oxd.server.persistence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.service.MigrationService;
import org.gluu.oxd.server.service.Rp;
import org.gluu.service.cache.AbstractRedisProvider;
import org.gluu.service.cache.RedisConfiguration;
import org.gluu.service.cache.RedisProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * @author yuriyz
 */
public class RedisPersistenceService implements PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPersistenceService.class);

    private final OxdServerConfiguration configuration;
    private AbstractRedisProvider redisProvider;

    public RedisPersistenceService(OxdServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void create() {
        LOG.debug("Creating RedisPersistenceService ...");

        try {
            RedisConfiguration redisConfiguration = asRedisConfiguration(configuration);

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
            put(rp.getOxdId(), Jackson2.serializeWithoutNulls(rp));
            return true;
        } catch (IOException e) {
            LOG.error("Failed to create RP: " + rp, e);
            return false;
        }
    }

    public boolean createExpiredObject(ExpiredObject obj) {
        try {
            int objectExpirationInMinutes = 0;

            switch (obj.getType()) {
                case STATE:
                    objectExpirationInMinutes = configuration.getStateExpirationInMinutes();
                    break;
                case NONCE:
                    objectExpirationInMinutes = configuration.getNonceExpirationInMinutes();
                    break;
                case REQUEST_OBJECT:
                    objectExpirationInMinutes = configuration.getRequestObjectExpirationInMinutes();
                    break;
                case JWKS:
                    objectExpirationInMinutes = configuration.getJwksExpirationInHours() * 60;
                    break;
            }

            put(objectExpirationInMinutes * 60, obj.getKey(), obj.getValue());
            return true;
        } catch (Exception e) {
            LOG.error("Failed to create ExpiredObject: " + obj.getKey(), e);
            return false;
        }
    }

    @Override
    public boolean update(Rp rp) {
        try {
            put(rp.getOxdId(), Jackson2.serializeWithoutNulls(rp));
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

    public ExpiredObject getExpiredObject(String key) {
        String value = (String) redisProvider.get(key);

        if (!Strings.isNullOrEmpty(value)) {
            ExpiredObject expiredObjectFromDb = null;
            try {
                expiredObjectFromDb = Jackson2.createJsonMapper().readValue(value, ExpiredObject.class);
            } catch (IOException e) {
                LOG.error("Error in assigning json value to ExpiredObject value attribute.", e);
                expiredObjectFromDb = new ExpiredObject();
            }
            ExpiredObject expiredObject = new ExpiredObject(key, value, expiredObjectFromDb.getType(), expiredObjectFromDb.getIat(), expiredObjectFromDb.getExp());

            return expiredObject;
        }
        return null;
    }

    public boolean isExpiredObjectPresent(String key) {
        return getExpiredObject(key) != null;
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

    public boolean deleteExpiredObjectsByKey(String key) {
        redisProvider.remove(key);
        return true;
    }

    public boolean deleteAllExpiredObjects() {
        //Implementation not required.
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

    public void put(int expirationInSeconds, String key, String value) {
        redisProvider.put(expirationInSeconds, key, value);
    }

    public String get(String key) {
        return (String) redisProvider.get(key);
    }

    public static RedisConfiguration asRedisConfiguration(OxdServerConfiguration configuration) throws Exception {
        return asRedisConfiguration(Jackson2.asOldNode(configuration.getStorageConfiguration()));
    }

    public static RedisConfiguration asRedisConfiguration(JsonNode node) throws Exception {
        try {
            return Jackson2.createJsonMapper().treeToValue(node, RedisConfiguration.class);
        } catch (Exception e) {
            LOG.error("Failed to parse RedisConfiguration.", e);
            throw e;
        }
    }
}
