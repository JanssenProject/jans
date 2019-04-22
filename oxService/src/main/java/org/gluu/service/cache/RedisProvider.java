package org.gluu.service.cache;

import org.apache.commons.lang.StringUtils;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author yuriyz on 02/23/2017.
 */
@ApplicationScoped
public class RedisProvider extends AbstractCacheProvider<AbstractRedisProvider> {

    public static final int DEFAULT_PUT_EXPIRATION_IN_SECONDS = 60;

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    @Inject
    private StringEncrypter stringEncrypter;

    private AbstractRedisProvider redisProvider;
    private int defaultPutExpiration = DEFAULT_PUT_EXPIRATION_IN_SECONDS;

    public RedisProvider() {
    }

    @PostConstruct
    public void init() {
    }

    public void create() {
        try {
            RedisConfiguration redisConfiguration = cacheConfiguration.getRedisConfiguration();
            decryptPassword(redisConfiguration);
            log.debug("Starting RedisProvider ... configuration:" + redisConfiguration);

            defaultPutExpiration = redisConfiguration.getDefaultPutExpiration() > 0 ? redisConfiguration.getDefaultPutExpiration()
                    : DEFAULT_PUT_EXPIRATION_IN_SECONDS;

            redisProvider = RedisProviderFactory.create(cacheConfiguration.getRedisConfiguration());
            redisProvider.create();

            log.debug("RedisProvider started.");
        } catch (Exception e) {
            log.error("Failed to start RedisProvider.");
            throw new IllegalStateException("Error starting RedisProvider", e);
        }
    }

    private void decryptPassword(RedisConfiguration redisConfiguration) {
        try {
            String encryptedPassword = redisConfiguration.getPassword();
            if (StringUtils.isNotBlank(encryptedPassword)) {
                redisConfiguration.setDecryptedPassword(stringEncrypter.decrypt(encryptedPassword));
                log.trace("Decrypted redis password successfully.");
            }
        } catch (StringEncrypter.EncryptionException e) {
            log.error("Error during redis password decryption", e);
        }
    }


    @PreDestroy
    public void destroy() {
        log.debug("Destroying RedisProvider");
        redisProvider.destroy();
        log.debug("Destroyed RedisProvider");
    }

    @Override
    public AbstractRedisProvider getDelegate() {
        return redisProvider;
    }

    @Override
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        return redisProvider.get(key);
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        redisProvider.put(expirationInSeconds > 0 ? expirationInSeconds : defaultPutExpiration, key, object);
    }

    @Override
    public void remove(String key) {
        redisProvider.remove(key);
    }

    @Override
    public void clear() {
        redisProvider.clear();
    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.REDIS;
    }

}
