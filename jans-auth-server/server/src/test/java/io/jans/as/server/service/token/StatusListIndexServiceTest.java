package io.jans.as.server.service.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterNodeManager;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import io.jans.model.token.StatusIndexPool;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class StatusListIndexServiceTest {

    private static final long HOUR = StatusListIndexService.EXPIRATION_BUCKET_MILLIS;
    private static final String POOL_DN = "jansNum=0,ou=status_index_pool,o=jans";

    @InjectMocks
    private StatusListIndexService statusListIndexService;

    @Mock
    private Logger log;

    @Mock
    private StatusIndexPoolService statusTokenPoolService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ClusterNodeManager clusterManager;

    private long retryDelay;

    private StatusListIndexService initializedService;

    @BeforeMethod
    public void setUp() {
        lenient().when(appConfiguration.getStatusListIndexAllocationBlockSize()).thenReturn(100);
        retryDelay = StatusListIndexService.REGISTER_RETRY_DELAY;
        StatusListIndexService.REGISTER_RETRY_DELAY = 0;
        statusListIndexService.init();
        initializedService = statusListIndexService;
    }

    @AfterMethod
    public void tearDown() {
        initializedService.destroy();
        StatusListIndexService.REGISTER_RETRY_DELAY = retryDelay;
    }

    @Test
    public void roundUpToBucket_shouldRoundUpToNextHour() {
        assertEquals(StatusListIndexService.roundUpToBucket(new Date(HOUR + 1)), new Date(2 * HOUR));
        assertEquals(StatusListIndexService.roundUpToBucket(new Date(2 * HOUR - 1)), new Date(2 * HOUR));
    }

    @Test
    public void roundUpToBucket_whenDateIsOnBoundary_shouldKeepIt() {
        assertEquals(StatusListIndexService.roundUpToBucket(new Date(3 * HOUR)), new Date(3 * HOUR));
    }

    @Test
    public void latest_shouldReturnLaterNonNullDate() {
        Date earlier = new Date(1000);
        Date later = new Date(2000);

        assertEquals(StatusListIndexService.latest(earlier, later), later);
        assertEquals(StatusListIndexService.latest(later, earlier), later);
        assertEquals(StatusListIndexService.latest(null, earlier), earlier);
        assertEquals(StatusListIndexService.latest(earlier, null), earlier);
        assertNull(StatusListIndexService.latest(null, null));
    }

    @Test
    public void registerTokenExpiration_whenIndexIsNull_shouldDoNothing() {
        statusListIndexService.registerTokenExpiration(null, new Date());

        verifyNoInteractions(statusTokenPoolService);
    }

    @Test
    public void registerTokenExpiration_whenIndexIsNegative_shouldDoNothing() {
        statusListIndexService.registerTokenExpiration(-1, new Date());

        verifyNoInteractions(statusTokenPoolService);
    }

    @Test
    public void registerTokenExpiration_whenExpirationIsNull_shouldDoNothing() {
        statusListIndexService.registerTokenExpiration(5, null);

        verifyNoInteractions(statusTokenPoolService);
    }

    @Test
    public void registerTokenExpiration_whenLocalPoolCoversToken_shouldNotWrite() throws Exception {
        allocateLocalPool(new Date(System.currentTimeMillis() + 48 * HOUR));
        int index = statusListIndexService.next();

        statusListIndexService.registerTokenExpiration(index, new Date(System.currentTimeMillis() + HOUR));

        verify(statusTokenPoolService, never()).extendExpiration(anyString(), any());
    }

    @Test
    public void registerTokenExpiration_whenTokenOutlivesLocalPool_shouldExtendPoolAsyncOnce() throws Exception {
        StatusIndexPool pool = allocateLocalPool(new Date(System.currentTimeMillis() + 600 * 1000L));
        int index = statusListIndexService.next();
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        CountDownLatch extended = new CountDownLatch(1);
        when(statusTokenPoolService.extendExpiration(POOL_DN, target)).thenAnswer(invocation -> {
            extended.countDown();
            return target;
        });

        statusListIndexService.registerTokenExpiration(index, tokenExpiration);
        assertTrue(extended.await(5, TimeUnit.SECONDS));
        awaitIdle();
        statusListIndexService.registerTokenExpiration(index, new Date(tokenExpiration.getTime() - 1000));
        awaitIdle();

        verify(statusTokenPoolService, times(1)).extendExpiration(POOL_DN, target);
        assertEquals(pool.getExpirationDate(), target);
    }

    @Test
    public void registerTokenExpiration_whenExtensionIsPending_shouldNotScheduleItAgain() throws Exception {
        allocateLocalPool(new Date(System.currentTimeMillis() + 600 * 1000L));
        int index = statusListIndexService.next();
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(statusTokenPoolService.extendExpiration(POOL_DN, target)).thenAnswer(invocation -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            return target;
        });

        statusListIndexService.registerTokenExpiration(index, tokenExpiration);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        statusListIndexService.registerTokenExpiration(index, tokenExpiration);
        statusListIndexService.registerTokenExpiration(index, new Date(tokenExpiration.getTime() - 1000));
        release.countDown();
        awaitIdle();

        verify(statusTokenPoolService, times(1)).extendExpiration(POOL_DN, target);
    }

    @Test
    public void registerTokenExpiration_whenExtensionFailsOnce_shouldRetry() throws Exception {
        StatusIndexPool pool = allocateLocalPool(new Date(System.currentTimeMillis() + 600 * 1000L));
        int index = statusListIndexService.next();
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        CountDownLatch attempts = new CountDownLatch(2);
        when(statusTokenPoolService.extendExpiration(POOL_DN, target)).thenAnswer(invocation -> {
            attempts.countDown();
            if (attempts.getCount() > 0) {
                throw new IllegalStateException("test");
            }
            return target;
        });

        statusListIndexService.registerTokenExpiration(index, tokenExpiration);

        assertTrue(attempts.await(5, TimeUnit.SECONDS));
        awaitIdle();
        verify(statusTokenPoolService, times(2)).extendExpiration(POOL_DN, target);
        assertEquals(pool.getExpirationDate(), target);
    }

    @Test
    public void registerTokenExpiration_whenStoredExpirationDoesNotCoverToken_shouldRetryAndKeepLocalExpiration() throws Exception {
        Date poolExpiration = new Date(System.currentTimeMillis() + 600 * 1000L);
        StatusIndexPool pool = allocateLocalPool(poolExpiration);
        int index = statusListIndexService.next();
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        Date lower = new Date(System.currentTimeMillis() + HOUR);
        CountDownLatch attempts = new CountDownLatch(StatusListIndexService.REGISTER_ATTEMPT_LIMIT);
        when(statusTokenPoolService.extendExpiration(POOL_DN, target)).thenAnswer(invocation -> {
            attempts.countDown();
            return lower;
        });

        statusListIndexService.registerTokenExpiration(index, tokenExpiration);

        assertTrue(attempts.await(5, TimeUnit.SECONDS));
        awaitIdle();
        verify(statusTokenPoolService, times(StatusListIndexService.REGISTER_ATTEMPT_LIMIT)).extendExpiration(POOL_DN, target);
        assertEquals(pool.getExpirationDate(), poolExpiration);
    }

    @Test
    public void registerTokenExpiration_whenAllAttemptsFail_shouldAcceptNewRequest() throws Exception {
        allocateLocalPool(new Date(System.currentTimeMillis() + 600 * 1000L));
        int index = statusListIndexService.next();
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        CountDownLatch attempts = new CountDownLatch(StatusListIndexService.REGISTER_ATTEMPT_LIMIT + 1);
        when(statusTokenPoolService.extendExpiration(POOL_DN, target)).thenAnswer(invocation -> {
            attempts.countDown();
            throw new IllegalStateException("test");
        });

        statusListIndexService.registerTokenExpiration(index, tokenExpiration);
        awaitCount(attempts, 1);
        awaitIdle();
        statusListIndexService.registerTokenExpiration(index, tokenExpiration);

        assertTrue(attempts.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void registerTokenExpiration_whenIndexIsFromAnotherPool_shouldExtendThatPool() throws Exception {
        allocateLocalPool(new Date(System.currentTimeMillis() + 600 * 1000L));
        statusListIndexService.next();
        StatusIndexPool other = new StatusIndexPool();
        other.setId(3);
        other.setDn("jansNum=3,ou=status_index_pool,o=jans");
        when(statusTokenPoolService.getPoolByIndex(305)).thenReturn(other);
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        CountDownLatch extended = new CountDownLatch(1);
        when(statusTokenPoolService.extendExpiration(other.getDn(), target)).thenAnswer(invocation -> {
            extended.countDown();
            return target;
        });

        statusListIndexService.registerTokenExpiration(305, tokenExpiration);

        assertTrue(extended.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void registerTokenExpiration_whenPoolIsNotFound_shouldRetryAndNotThrow() throws Exception {
        CountDownLatch attempts = new CountDownLatch(StatusListIndexService.REGISTER_ATTEMPT_LIMIT);
        when(statusTokenPoolService.getPoolByIndex(5)).thenAnswer(invocation -> {
            attempts.countDown();
            return null;
        });

        statusListIndexService.registerTokenExpiration(5, new Date(System.currentTimeMillis() + HOUR));

        assertTrue(attempts.await(5, TimeUnit.SECONDS));
        awaitIdle();
        verify(statusTokenPoolService, never()).extendExpiration(anyString(), any());
    }

    private void awaitIdle() throws Exception {
        Map<?, ?> requested = (Map<?, ?>) readField("requestedExpirations");
        long deadline = System.currentTimeMillis() + 5000;
        while (!requested.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(requested.isEmpty());
        ((ScheduledExecutorService) readField("expirationExecutor")).submit(() -> { }).get(5, TimeUnit.SECONDS);
    }

    private Object readField(String name) throws Exception {
        Field field = StatusListIndexService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(statusListIndexService);
    }

    private static void awaitCount(CountDownLatch latch, long count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (latch.getCount() > count && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(latch.getCount(), count);
    }

    private StatusIndexPool allocateLocalPool(Date expirationDate) throws Exception {
        StatusIndexPool pool = new StatusIndexPool();
        pool.setId(0);
        pool.setDn(POOL_DN);
        pool.setStartIndex(0);
        pool.setEndIndex(99);
        pool.setExpirationDate(expirationDate);
        pool.setIssueUntil(new Date(System.currentTimeMillis() + 600 * 1000L));
        when(clusterManager.getClusterNodeId()).thenReturn(1);
        when(statusTokenPoolService.allocate(1)).thenReturn(pool);
        return pool;
    }
}
