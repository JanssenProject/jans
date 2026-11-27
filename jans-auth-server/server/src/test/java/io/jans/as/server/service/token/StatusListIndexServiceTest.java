package io.jans.as.server.service.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterNodeManager;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import io.jans.model.token.StatusIndexPool;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class StatusListIndexServiceTest {

    private static final long HOUR = StatusListIndexService.EXPIRATION_BUCKET_MILLIS;

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
    public void registerTokenExpiration_whenTokenOutlivesLocalPool_shouldExtendPoolOnce() throws Exception {
        StatusIndexPool pool = allocateLocalPool(new Date(System.currentTimeMillis() + 600 * 1000L));
        int index = statusListIndexService.next();
        Date tokenExpiration = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = StatusListIndexService.roundUpToBucket(tokenExpiration);
        when(statusTokenPoolService.extendExpiration(pool.getDn(), target)).thenReturn(target);

        statusListIndexService.registerTokenExpiration(index, tokenExpiration);
        statusListIndexService.registerTokenExpiration(index, new Date(tokenExpiration.getTime() - 1000));

        verify(statusTokenPoolService, times(1)).extendExpiration(pool.getDn(), target);
        assertEquals(pool.getExpirationDate(), target);
    }

    @Test
    public void registerTokenExpiration_whenLocalPoolCoversToken_shouldNotWrite() throws Exception {
        allocateLocalPool(new Date(System.currentTimeMillis() + 48 * HOUR));
        int index = statusListIndexService.next();

        statusListIndexService.registerTokenExpiration(index, new Date(System.currentTimeMillis() + HOUR));

        verify(statusTokenPoolService, never()).extendExpiration(anyString(), any());
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

        statusListIndexService.registerTokenExpiration(305, tokenExpiration);

        verify(statusTokenPoolService).extendExpiration(other.getDn(), StatusListIndexService.roundUpToBucket(tokenExpiration));
    }

    @Test
    public void registerTokenExpiration_whenPoolServiceFails_shouldNotThrow() {
        when(statusTokenPoolService.getPoolByIndex(anyInt())).thenThrow(new RuntimeException("test"));

        statusListIndexService.registerTokenExpiration(5, new Date());
    }

    private StatusIndexPool allocateLocalPool(Date expirationDate) throws Exception {
        StatusIndexPool pool = new StatusIndexPool();
        pool.setId(0);
        pool.setDn("jansNum=0,ou=status_index_pool,o=jans");
        pool.setStartIndex(0);
        pool.setEndIndex(99);
        pool.setExpirationDate(expirationDate);
        pool.setIssueUntil(new Date(System.currentTimeMillis() + 600 * 1000L));
        when(clusterManager.getClusterNodeId()).thenReturn(1);
        when(statusTokenPoolService.allocate(1)).thenReturn(pool);
        return pool;
    }
}
