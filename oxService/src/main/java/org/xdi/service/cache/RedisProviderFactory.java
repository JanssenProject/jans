package org.xdi.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author yuriyz
 */
public class RedisProviderFactory {

    private final static Logger LOG = LoggerFactory.getLogger(RedisProviderFactory.class);

    private RedisProviderFactory() {
    }

    public static AbstractRedisProvider create(RedisConfiguration redisConfiguration) {
        try {
            LOG.debug("Creating RedisProvider ... configuration:" + redisConfiguration);

            switch (redisConfiguration.getRedisProviderType()) {
                case STANDALONE:
                    return new RedisStandaloneProvider(redisConfiguration);
                case CLUSTER:
                    return new RedisClusterProvider(redisConfiguration);
                case SHARDED:
                    return new RedisShardedProvider(redisConfiguration);
            }

            LOG.error("Failed to create RedisProvider. RedisProviderType is not supported by current version of oxcore: " + redisConfiguration.getRedisProviderType() + ", redisConfiguration:" + redisConfiguration);
            throw new RuntimeException("RedisProviderType is not supported by current version of oxcore: " + redisConfiguration.getRedisProviderType());
        } catch (Exception e) {
            LOG.error("Failed to create RedisProvider.");
            throw new RuntimeException("Error creating RedisProvider", e);
        }
    }

    public static void destroy(AbstractRedisProvider provider) {
        if (provider != null) {
            provider.destroy();
        }
    }

    public static void destroySilently(AbstractRedisProvider provider) {
        try {
            destroy(provider);
        } catch (Exception e) {
            LOG.error("Failed to destroy redis provider.", e);
        }
    }
}
