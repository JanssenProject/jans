package io.jans.as.server.service.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterNodeManager;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.util.Pair;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Yuriy Z
 * @author Yuriy Movchan
 * 
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class StatusListIndexService {

    public static final long EXPIRATION_BUCKET_MILLIS = 60 * 60 * 1000L;
    public static final int REGISTER_ATTEMPT_LIMIT = 6;
    public static long REGISTER_RETRY_DELAY = 1000L; // milliseconds, doubled on each next attempt

    @Inject
    private Logger log;

    @Inject
    private StatusIndexPoolService statusTokenPoolService;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private ClusterNodeManager clusterManager;

    private final ReentrantLock allocatedLock = new ReentrantLock();
	
    private StatusIndexPool tokenPool = null;

    private final ConcurrentMap<Integer, Date> requestedExpirations = new ConcurrentHashMap<>();

    private ScheduledExecutorService expirationExecutor;

    public synchronized void updateStatusAtIndexes(List<Integer> indexes, TokenStatus status) {
        try {
            log.debug("Updating status list at indexes {} with status {} ...", indexes, status);

            if (indexes == null || indexes.isEmpty()) {
                return; // invalid
            }

            // first load pools for indexes
            Collection<StatusIndexPool> pools = findPoolsByIndexes(indexes);
            for (StatusIndexPool pool : pools) {
                updateWithLockSilently(pool, indexes, status);
            }

            log.debug("Updated status list at index {} with status {} successfully.", indexes, status);

        } catch (Exception e) {
            log.error("Failed to update token list status at index " + indexes + " with status " + status, e);
        }
    }

    private Collection<StatusIndexPool> findPoolsByIndexes(List<Integer> indexes) {
        Map<Integer, StatusIndexPool> pools = new HashMap<>();
        for (Integer index : indexes) {
            int poolId = index / appConfiguration.getStatusListIndexAllocationBlockSize();

            StatusIndexPool indexHolder = pools.get(poolId);
            if (indexHolder == null) {
                indexHolder = statusTokenPoolService.getPoolByIndex(index);
                log.debug("Found pool {} by index {}", indexHolder.getDn(), index);
                pools.put(indexHolder.getId(), indexHolder);
            }
        }
        return pools.values();
    }

    private void updateWithLockSilently(StatusIndexPool pool, List<Integer> indexes, TokenStatus status) {
        try {
            statusTokenPoolService.updateWithLock(pool.getDn(), indexes, status);
        } catch (Exception e) {
            log.error("Failed to persist status index pool " + pool.getId(), e);
        }
    }

    @PostConstruct
    public void init() {
        expirationExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("status_list_expiration");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    public void destroy() {
        if (expirationExecutor != null) {
            expirationExecutor.shutdownNow();
        }
    }

    /**
     * Makes sure the pool that holds the index is not reused before the token expires. Does not block token
     * issuance: the pool expiration is extended asynchronously and retried on failure.
     *
     * @param index status list index of the token
     * @param tokenExpiration token expiration date
     */
    public void registerTokenExpiration(Integer index, Date tokenExpiration) {
        if (index == null || index < 0 || tokenExpiration == null) {
            return;
        }

        final Date target = roundUpToBucket(tokenExpiration);
        final StatusIndexPool localPool = tokenPool;
        if (localPool != null && localPool.containsIndex(index) && coversExpiration(localPool.getExpirationDate(), target)) {
            return;
        }

        final int poolId = index / appConfiguration.getStatusListIndexAllocationBlockSize();
        final boolean[] requested = {false};
        requestedExpirations.compute(poolId, (key, pending) -> {
            if (coversExpiration(pending, target)) {
                return pending;
            }
            requested[0] = true;
            return target;
        });

        if (requested[0]) {
            scheduleExtension(index, poolId, target, 1, 0);
        }
    }

    private void scheduleExtension(int index, int poolId, Date target, int attempt, long delay) {
        try {
            expirationExecutor.schedule(() -> extendExpiration(index, poolId, target, attempt), delay, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            requestedExpirations.remove(poolId, target);
            log.error("Failed to schedule extension of status index pool {} to {}", poolId, target, e);
        }
    }

    private void extendExpiration(int index, int poolId, Date target, int attempt) {
        try {
            final StatusIndexPool localPool = tokenPool;
            final boolean isLocal = localPool != null && localPool.containsIndex(index);
            final String poolDn = isLocal ? localPool.getDn() : getPoolDnByIndex(index);

            final Date persisted = statusTokenPoolService.extendExpiration(poolDn, target);
            if (!coversExpiration(persisted, target)) {
                throw new IllegalStateException("Status index pool expiration " + persisted + " does not cover " + target);
            }

            if (isLocal) {
                localPool.setExpirationDate(persisted);
            }
            requestedExpirations.remove(poolId, target);
        } catch (Exception e) {
            if (attempt >= REGISTER_ATTEMPT_LIMIT) {
                requestedExpirations.remove(poolId, target);
                log.error("Failed to extend expiration of status index pool {} to {} after {} attempts", poolId, target, attempt, e);
                return;
            }

            log.warn("Failed to extend expiration of status index pool {} to {}, attempt {}. Retrying.", poolId, target, attempt, e);
            scheduleExtension(index, poolId, target, attempt + 1, REGISTER_RETRY_DELAY * (1L << (attempt - 1)));
        }
    }

    private String getPoolDnByIndex(int index) {
        final StatusIndexPool pool = statusTokenPoolService.getPoolByIndex(index);
        if (pool == null) {
            throw new IllegalStateException("Status index pool is not found for index " + index);
        }
        return pool.getDn();
    }

    private static boolean coversExpiration(Date poolExpiration, Date expiration) {
        return poolExpiration != null && !poolExpiration.before(expiration);
    }

    public static Date latest(Date first, Date second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.after(second) ? first : second;
    }

    public static Date roundUpToBucket(Date date) {
        final long time = date.getTime();
        return new Date(((time + EXPIRATION_BUCKET_MILLIS - 1) / EXPIRATION_BUCKET_MILLIS) * EXPIRATION_BUCKET_MILLIS);
    }

    public Integer next() {
        try {
            return nextIndex().getFirst();
        } catch (Exception e) {
            // return -1. Even if we failed to get next index, we don't want to fail entire call
            log.error("Failed to get next index", e);
            return -1;
        }
    }

    public Pair<Integer, StatusIndexPool> nextIndex() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    	// Create copy of variable to make sure that another Thread not changed it
    	StatusIndexPool localTokenPool = tokenPool;
    	int newIndex = -1;
    	if (localTokenPool != null) {
    		newIndex = localTokenPool.nextIndex();
    		if (newIndex != -1) {
    			return new Pair<>(newIndex, localTokenPool);
    		}
    	}

    	// Attempt to lock before TokenPool allocating
    	allocatedLock.lock();
		try {
			// Check if TokenPool were changed since method call
			if (System.identityHashCode(localTokenPool) != System.identityHashCode(tokenPool)) {
				// Try to get index from new pool which another threads gets already
				localTokenPool = tokenPool;
				if (localTokenPool != null) {
                    newIndex = localTokenPool.nextIndex();
                    if (newIndex != -1) {
                        return new Pair<>(newIndex, localTokenPool);
                    }
                }
			}
			
			// Allocate new TokenPool
			tokenPool = statusTokenPoolService.allocate(clusterManager.getClusterNodeId());

			newIndex = tokenPool.nextIndex();
			return new Pair<>(newIndex, tokenPool);
		} finally {
			allocatedLock.unlock();
		}
    }
}
