package io.jans.as.server.service.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterNodeManager;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.util.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Makes sure the pool that holds the index is not reused before the token expires.
     *
     * @param index status list index of the token
     * @param tokenExpiration token expiration date
     */
    public void registerTokenExpiration(Integer index, Date tokenExpiration) {
        if (index == null || index < 0 || tokenExpiration == null) {
            return;
        }

        try {
            final Date target = roundUpToBucket(tokenExpiration);

            final StatusIndexPool localPool = tokenPool;
            if (localPool != null && localPool.containsIndex(index)) {
                if (coversExpiration(localPool, target)) {
                    return;
                }
                synchronized (localPool) {
                    if (coversExpiration(localPool, target)) {
                        return;
                    }
                    final Date persisted = statusTokenPoolService.extendExpiration(localPool.getDn(), target);
                    if (persisted != null) {
                        localPool.setExpirationDate(persisted);
                    }
                }
                return;
            }

            final StatusIndexPool pool = statusTokenPoolService.getPoolByIndex(index);
            if (pool != null) {
                statusTokenPoolService.extendExpiration(pool.getDn(), target);
            }
        } catch (Exception e) {
            log.error("Failed to register token expiration " + tokenExpiration + " for status list index " + index, e);
        }
    }

    private static boolean coversExpiration(StatusIndexPool pool, Date expiration) {
        final Date poolExpiration = pool.getExpirationDate();
        return poolExpiration != null && !poolExpiration.before(expiration);
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
