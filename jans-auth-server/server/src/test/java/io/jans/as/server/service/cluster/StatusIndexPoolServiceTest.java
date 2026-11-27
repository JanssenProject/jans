package io.jans.as.server.service.cluster;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.token.StatusIndexPoolData;
import io.jans.model.token.StatusIndexPoolExpiration;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.search.filter.Filter;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static io.jans.as.server.service.cluster.ClusterNodeService.LOCK_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class StatusIndexPoolServiceTest {

    private static final String BASE_DN = "ou=status_index_pool,o=jans";
    private static final long HOUR = 60 * 60 * 1000L;
    private static final int NODE_ID = 7;

    @Spy
    @InjectMocks
    private StatusIndexPoolService service;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private PersistenceEntryManager entryManager;

    @BeforeMethod
    public void setUp() {
        BaseDnConfiguration baseDn = new BaseDnConfiguration();
        baseDn.setStatusIndexPool(BASE_DN);
        lenient().when(staticConfiguration.getBaseDn()).thenReturn(baseDn);
        lenient().when(appConfiguration.getStatusListIndexAllocationBlockSize()).thenReturn(100);
        lenient().when(appConfiguration.getAccessTokenLifetime()).thenReturn(300);
        lenient().when(appConfiguration.getIdTokenLifetime()).thenReturn(3600);
        lenient().when(appConfiguration.getLogoutStatusJwtLifetime()).thenReturn(86400);
        service.init();
    }

    @Test
    public void getMaxTokenLifetimeInSeconds_shouldReturnMaxOfConfiguredLifetimes() {
        assertEquals(service.getMaxTokenLifetimeInSeconds(), 86400L);
    }

    @Test
    public void getMaxTokenLifetimeInSeconds_whenLogoutStatusJwtLifetimeIsNull_shouldIgnoreIt() {
        when(appConfiguration.getLogoutStatusJwtLifetime()).thenReturn(null);

        assertEquals(service.getMaxTokenLifetimeInSeconds(), 3600L);
    }

    @Test
    public void getReuseDelay_whenMaxTokenLifetimeExceedsDefaultDelay_shouldReturnMaxTokenLifetime() {
        assertEquals(service.getReuseDelay(), 86400 * 1000L);
    }

    @Test
    public void getReuseDelay_whenMaxTokenLifetimeIsBelowDefaultDelay_shouldReturnDefaultDelay() {
        when(appConfiguration.getLogoutStatusJwtLifetime()).thenReturn(60);

        assertEquals(service.getReuseDelay(), AbstractStatusIndexPoolService.DELAY_AFTER_EXPIRATION);
    }

    @Test
    public void isReusable_whenExpirationPlusDelayHasPassed_shouldReturnTrue() {
        assertTrue(service.isReusable(pool(0, new Date(System.currentTimeMillis() - 25 * HOUR))));
    }

    @Test
    public void isReusable_whenExpirationPlusDelayHasNotPassed_shouldReturnFalse() {
        assertFalse(service.isReusable(pool(0, new Date(System.currentTimeMillis() - 23 * HOUR))));
    }

    @Test
    public void isReusable_whenExpirationIsNull_shouldReturnTrue() {
        assertTrue(service.isReusable(pool(0, null)));
    }

    @Test
    public void isReusable_whenPoolIsNull_shouldReturnFalse() {
        assertFalse(service.isReusable(null));
    }

    @Test
    public void extendExpiration_whenNewDateIsLater_shouldMergeNewDate() {
        Date current = new Date(System.currentTimeMillis() + HOUR);
        Date target = new Date(System.currentTimeMillis() + 24 * HOUR);
        StatusIndexPoolExpiration entry = expiration(current);
        when(entryManager.find(StatusIndexPoolExpiration.class, entry.getDn())).thenReturn(entry);

        Date result = service.extendExpiration(entry.getDn(), target);

        assertEquals(result, target);
        ArgumentCaptor<StatusIndexPoolExpiration> captor = ArgumentCaptor.forClass(StatusIndexPoolExpiration.class);
        verify(entryManager).merge(captor.capture());
        assertEquals(captor.getValue().getExpirationDate(), target);
        assertNotNull(captor.getValue().getLastUpdate());
    }

    @Test
    public void extendExpiration_whenCurrentDateIsLater_shouldNotLowerIt() {
        Date current = new Date(System.currentTimeMillis() + 24 * HOUR);
        Date target = new Date(System.currentTimeMillis() + HOUR);
        StatusIndexPoolExpiration entry = expiration(current);
        when(entryManager.find(StatusIndexPoolExpiration.class, entry.getDn())).thenReturn(entry);

        Date result = service.extendExpiration(entry.getDn(), target);

        assertEquals(result, current);
        verify(entryManager, never()).merge(any());
    }

    @Test
    public void extendExpiration_whenPoolIsNotFound_shouldReturnNull() {
        assertNull(service.extendExpiration(service.createDn(0), new Date()));
        verify(entryManager, never()).merge(any());
    }

    @Test
    public void updateData_shouldMergeOnlyDataLockKeyAndLastUpdate() {
        StatusIndexPool pool = pool(0, new Date(System.currentTimeMillis() + 24 * HOUR));
        pool.setData("encoded");
        pool.setLockKey(LOCK_KEY);
        pool.setLastUpdate(new Date());

        service.updateData(pool);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entryManager).merge(captor.capture());
        assertTrue(captor.getValue() instanceof StatusIndexPoolData);
        StatusIndexPoolData merged = (StatusIndexPoolData) captor.getValue();
        assertEquals(merged.getDn(), pool.getDn());
        assertEquals(merged.getData(), "encoded");
        assertEquals(merged.getLockKey(), LOCK_KEY);
        assertEquals(merged.getLastUpdate(), pool.getLastUpdate());
    }

    @Test
    public void updateWithLock_shouldNotWriteExpiration() throws Exception {
        when(appConfiguration.getStatusListBitSize()).thenReturn(2);
        StatusIndexPool loaded = pool(0, new Date(System.currentTimeMillis() + HOUR));
        StatusIndexPool reloaded = pool(0, new Date(System.currentTimeMillis() + 24 * HOUR));
        reloaded.setLockKey(LOCK_KEY);
        when(entryManager.find(StatusIndexPool.class, loaded.getDn())).thenReturn(loaded, reloaded);

        StatusIndexPool result = service.updateWithLock(loaded.getDn(), Collections.singletonList(5), TokenStatus.INVALID);

        assertEquals(result, reloaded);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entryManager).merge(captor.capture());
        assertTrue(captor.getValue() instanceof StatusIndexPoolData);
        StatusList statusList = StatusList.fromEncoded(((StatusIndexPoolData) captor.getValue()).getData(), 2);
        assertEquals(statusList.get(5), TokenStatus.INVALID.getValue());
    }

    @Test
    public void allocate_whenExpiredPoolWasExtendedAfterQuery_shouldNotReuseIt() throws Exception {
        StatusIndexPool queried = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        StatusIndexPool reloaded = pool(0, new Date(System.currentTimeMillis() + 20 * HOUR));
        when(entryManager.findEntries(eq(BASE_DN), eq(StatusIndexPool.class), any(Filter.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(queried)));
        when(entryManager.find(StatusIndexPool.class, queried.getDn())).thenReturn(reloaded);
        mockNewPoolCreation(1);

        StatusIndexPool allocated = service.allocate(NODE_ID);

        assertEquals(allocated.getId(), Integer.valueOf(1));
        verify(entryManager, never()).merge(any());
        verify(service, never()).markAllIndexesAsValid(anyList());
    }

    @Test
    public void allocate_whenExpiredPoolIsReusable_shouldReuseItAndSetIssueUntil() throws Exception {
        StatusIndexPool queried = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        StatusIndexPool reloaded = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        StatusIndexPool locked = pool(0, new Date(System.currentTimeMillis() + 600 * 1000L));
        locked.setLockKey(LOCK_KEY);
        locked.setNodeId(NODE_ID);
        when(entryManager.findEntries(eq(BASE_DN), eq(StatusIndexPool.class), any(Filter.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(queried)));
        when(entryManager.find(StatusIndexPool.class, queried.getDn())).thenReturn(reloaded, locked);
        doNothing().when(service).markAllIndexesAsValid(anyList());

        StatusIndexPool allocated = service.allocate(NODE_ID);

        assertEquals(allocated.getId(), Integer.valueOf(0));
        assertNotNull(allocated.getIssueUntil());
        assertTrue(allocated.getIssueUntil().after(new Date()));
        verify(entryManager).merge(reloaded);
        verify(service).markAllIndexesAsValid(anyList());
    }

    @Test
    public void allocate_whenNoExpiredPools_shouldCreateNewPoolWithIssueUntil() throws Exception {
        when(entryManager.findEntries(eq(BASE_DN), eq(StatusIndexPool.class), any(Filter.class)))
                .thenReturn(new ArrayList<>());
        mockNewPoolCreation(0);

        StatusIndexPool allocated = service.allocate(NODE_ID);

        assertEquals(allocated.getId(), Integer.valueOf(0));
        assertNotNull(allocated.getIssueUntil());
        assertTrue(allocated.getIssueUntil().after(new Date()));
        assertEquals(allocated.nextIndex(), 0);
    }

    @Test
    public void allocate_whenExpiredPoolBelongsToAnotherNode_shouldTakeOverAndPersistNodeId() throws Exception {
        StatusIndexPool queried = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        queried.setNodeId(3);
        StatusIndexPool reloaded = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        reloaded.setNodeId(3);
        reloaded.setLockKey("another-node-lock-key");
        StatusIndexPool locked = pool(0, new Date(System.currentTimeMillis() + 600 * 1000L));
        locked.setLockKey(LOCK_KEY);
        locked.setNodeId(NODE_ID);
        when(entryManager.findEntries(eq(BASE_DN), eq(StatusIndexPool.class), any(Filter.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(queried)));
        when(entryManager.find(StatusIndexPool.class, queried.getDn())).thenReturn(reloaded, locked);
        doNothing().when(service).markAllIndexesAsValid(anyList());

        StatusIndexPool allocated = service.allocate(NODE_ID);

        assertEquals(allocated.getId(), Integer.valueOf(0));
        assertEquals(allocated.getNodeId(), Integer.valueOf(NODE_ID));
        ArgumentCaptor<StatusIndexPool> captor = ArgumentCaptor.forClass(StatusIndexPool.class);
        verify(entryManager).merge(captor.capture());
        assertEquals(captor.getValue().getNodeId(), Integer.valueOf(NODE_ID));
        assertEquals(captor.getValue().getLockKey(), LOCK_KEY);
    }

    @Test
    public void allocate_whenAnotherNodeWinsLockOnExpiredPool_shouldCreateNewPool() throws Exception {
        StatusIndexPool queried = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        StatusIndexPool reloaded = pool(0, new Date(System.currentTimeMillis() - 25 * HOUR));
        StatusIndexPool lockedByOther = pool(0, new Date(System.currentTimeMillis() + 600 * 1000L));
        lockedByOther.setLockKey("another-node-lock-key");
        lockedByOther.setNodeId(3);
        when(entryManager.findEntries(eq(BASE_DN), eq(StatusIndexPool.class), any(Filter.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(queried)));
        when(entryManager.find(StatusIndexPool.class, queried.getDn())).thenReturn(reloaded, lockedByOther);
        mockNewPoolCreation(1);

        StatusIndexPool allocated = service.allocate(NODE_ID);

        assertEquals(allocated.getId(), Integer.valueOf(1));
        verify(service, never()).markAllIndexesAsValid(anyList());
    }

    private void mockNewPoolCreation(int expectedId) {
        PagedResult<StatusIndexPool> lastPools = new PagedResult<>();
        lastPools.setEntries(new ArrayList<>());
        lastPools.setEntriesCount(0);
        if (expectedId > 0) {
            StatusIndexPool last = pool(expectedId - 1, new Date());
            lastPools.setEntries(new ArrayList<>(Collections.singletonList(last)));
            lastPools.setEntriesCount(1);
        }
        when(entryManager.findPagedEntries(eq(BASE_DN), eq(StatusIndexPool.class), any(Filter.class), isNull(),
                eq("jansNum"), any(), anyInt(), anyInt(), anyInt())).thenReturn(lastPools);

        StatusIndexPool created = pool(expectedId, new Date(System.currentTimeMillis() + 600 * 1000L));
        created.setLockKey(LOCK_KEY);
        created.setNodeId(NODE_ID);
        when(entryManager.find(StatusIndexPool.class, service.createDn(expectedId))).thenReturn(created);
        lenient().when(entryManager.getPersistenceType(anyString())).thenReturn("sql");
    }

    private StatusIndexPool pool(int id, Date expirationDate) {
        StatusIndexPool pool = new StatusIndexPool();
        pool.setId(id);
        pool.setDn(service.createDn(id));
        pool.setExpirationDate(expirationDate);
        return pool;
    }

    private StatusIndexPoolExpiration expiration(Date expirationDate) {
        StatusIndexPoolExpiration entry = new StatusIndexPoolExpiration();
        entry.setDn(service.createDn(0));
        entry.setExpirationDate(expirationDate);
        return entry;
    }
}
