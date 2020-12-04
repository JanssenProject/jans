/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author yuriyz
 */
public class InMemoryCacheProviderTest {

    InMemoryCacheProvider cache = new InMemoryCacheProvider();

    @BeforeClass
    public void beforeClass() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cache = new InMemoryCacheProvider();
        cache.configure(cacheConfiguration);
        cache.create();
    }

    @Test(enabled = true)
    public void simpleTest() throws InterruptedException {
        String value = "simpleTest";
        cache.put(3, "myKey", value);

        assertEquals(cache.get("myKey"), value); // value must be there
        Thread.sleep(1000);
        assertEquals(cache.get("myKey"), value); // value still must be there -> not expired

        cache.put(5, "myKey", value);
        Thread.sleep(4000);
        assertEquals(cache.get("myKey"), value); // value still must be there -> not expired

        Thread.sleep(2000);
        assertNull(cache.get("myKey")); // value must be expired;
    }
}
