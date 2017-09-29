package org.xdi.oxd.server.service;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.persistence.RedisPersistenceService;
import org.xdi.oxd.web.TestAppModule;
import org.xdi.service.cache.RedisConfiguration;
import org.xdi.service.cache.RedisProviderType;

import java.io.File;
import java.io.FileInputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author yuriyz
 */
@Guice(modules = TestAppModule.class)
public class RedisConfigurationTest {

    @Test
    public void configurationTest() throws Exception {
        RedisConfiguration redisConfiguration = RedisPersistenceService.asRedisConfiguration(redisTestConfiguration());

        assertNotNull(redisConfiguration);
        assertEquals(redisConfiguration.getRedisProviderType(), RedisProviderType.STANDALONE);
    }

    public static Configuration redisTestConfiguration() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File("./oxd-server/src/test/resources/oxd-conf-test-redis.json"));
            return ConfigurationService.createConfiguration(fis);
        } catch (Exception e) {
            IOUtils.closeQuietly(fis);
            return null;
        }
    }
}
