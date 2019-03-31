package org.gluu.oxd.server.service;

import org.testng.annotations.Guice;
import org.gluu.oxd.server.guice.GuiceModule;

/**
 * @author yuriyz
 */
@Guice(modules = GuiceModule.class)
public class RedisConfigurationTest {

//    @Test
//    public void configurationTest() throws Exception {
//        RedisConfiguration redisConfiguration = RedisPersistenceService.asRedisConfiguration(redisTestConfiguration());
//
//        assertNotNull(redisConfiguration);
//        assertEquals(redisConfiguration.getRedisProviderType(), RedisProviderType.STANDALONE);
//    }
//
//    public static OxdServerConfiguration redisTestConfiguration() {
//        FileInputStream fis = null;
//        try {
//            fis = new FileInputStream(new File("./oxd-server/src/test/resources/oxd-conf-test-redis.json"));
//            return ConfigurationService.createConfiguration(fis);
//        } catch (Exception e) {
//            IOUtils.closeQuietly(fis);
//            return null;
//        }
//    }
}
