package org.xdi.service.cache;

import org.python.google.common.base.Stopwatch;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author yuriyz
 */
public class RedisProviderTest {

    private AbstractRedisProvider standaloneProvider;
    private AbstractRedisProvider clusterProvider;
    private AbstractRedisProvider shardedProvider;

    @BeforeClass
    public void beforeClass() {
        RedisConfiguration config = new RedisConfiguration();
//        config.setServers("localhost:7000,localhost:7001,localhost:7002,localhost:7003,localhost:7004,localhost:7005");
        config.setServers("localhost:6379");
        //config.setDecryptedPassword("foobared");

        standaloneProvider = new RedisStandaloneProvider(config);
        clusterProvider = new RedisClusterProvider(config);
        shardedProvider = new RedisShardedProvider(config);
    }

    @AfterClass
    public void afterClass() {
        RedisProviderFactory.destroySilently(standaloneProvider);
        RedisProviderFactory.destroySilently(clusterProvider);
        RedisProviderFactory.destroySilently(shardedProvider);
    }

    @Test(enabled = false)
    public void standaloneSimpleTest() throws InterruptedException {
        standaloneProvider.create();
        simpleTest(standaloneProvider);
    }

    @Test(enabled = false)
    public void clusterSimpleTest() throws InterruptedException {
        simpleTest(clusterProvider);
    }

    @Test(enabled = false)
    public void shardedSimpleTest() throws InterruptedException {
        simpleTest(clusterProvider);
    }

    private static void simpleTest(AbstractRedisProvider cache) throws InterruptedException {
        String value = "simpleTest";
        cache.put(3, "myKey", value);

        assertEquals(cache.get("myKey"), value); // value must be there
        Thread.sleep(1000);
        assertEquals(cache.get("myKey"), value); // value still must be there -> not expired
        Thread.sleep(3000);
        assertNull(cache.get("myKey")); // value must be expired;
    }

    @Test(enabled = false)
    public void standaloneMultiThread() throws InterruptedException {
        multiThreadTest(standaloneProvider, 1000);
    }

    @Test(enabled = false)
    public void clusterMultiThread() throws InterruptedException {
        multiThreadTest(clusterProvider, 1000);
    }

    @Test(enabled = false)
    public void shardedMultiThread() throws InterruptedException {
        multiThreadTest(shardedProvider, 1000);
    }

    private static void multiThreadTest(final AbstractRedisProvider cache, final int count) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        try {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            final AtomicInteger counter = new AtomicInteger(1);
            for (int i = 0; i < count; i++) {
                final int index = i;
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        String key = "myKey" + index;
                        cache.put(10, key, index);
                        cache.get(key);
                        cache.get(key);
                        if (counter.incrementAndGet() == count) {
                            System.out.println("Done in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
                        }
                    }
                });
            }

            Thread.sleep(4000);
        } finally {
            executorService.shutdown();
        }
    }
}
