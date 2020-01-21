package org.gluu.conf.service;

import static org.testng.Assert.assertNotNull;

import org.gluu.service.cache.CacheConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Yuriy Movchan
 * @version 0.1, 01/02/2020
 */
public class DummyConfigurationFactoryTest {

	private DummyConfigurationFactory dummyConfigurationFactory;

    @BeforeClass
    public void beforeClass() {
    	this.dummyConfigurationFactory = new DummyConfigurationFactory();
    }

    @Test(enabled = false)
    public void checkAppConfigurationLoad() {
    	assertNotNull(dummyConfigurationFactory.getAppConfiguration());
    }

    @Test(enabled = false)
    public void checkCacheConfiguration() {
    	CacheConfiguration cacheConfiguration = dummyConfigurationFactory.getCacheConfiguration();
    	assertNotNull(cacheConfiguration);
    	assertNotNull(cacheConfiguration.getCacheProviderType());
    }

}
