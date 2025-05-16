package io.jans.as.server.service.session;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterNodeManager;
import io.jans.model.token.SessionStatusIndexPool;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.util.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author Yuriy Z
 * @author Yuriy Movchan
 * 
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class SessionStatusListIndexService {

    @Inject
    private Logger log;

    @Inject
    private SessionStatusIndexPoolService sessionStatusIndexPoolService;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private ClusterNodeManager clusterManager;

    private final ReentrantLock allocatedLock = new ReentrantLock();
	
    private SessionStatusIndexPool sessionPool = null;

    public synchronized void updateStatusAtIndexes(List<Integer> indexes, TokenStatus status) {
        final String logPrefix = sessionStatusIndexPoolService.logPrefix();
        try {
            log.debug("{} Updating status list at indexes {} with status {} ...", logPrefix, indexes, status);

            if (indexes == null || indexes.isEmpty()) {
                return; // invalid
            }

            // first load pools for indexes
            Collection<SessionStatusIndexPool> pools = findPoolsByIndexes(indexes);
            for (SessionStatusIndexPool pool : pools) {
                updateWithLockSilently(pool, indexes, status);
            }

            log.debug("{} Updated status list at index {} with status {} successfully.", logPrefix, indexes, status);

        } catch (Exception e) {
            log.error(logPrefix + "Failed to update token list status at index " + indexes + " with status " + status, e);
        }
    }

    private Collection<SessionStatusIndexPool> findPoolsByIndexes(List<Integer> indexes) {
        // filter out nulls
        indexes = indexes.stream().filter(Objects::nonNull).collect(Collectors.toList());

        String logPrefix = sessionStatusIndexPoolService.logPrefix();
        Map<Integer, SessionStatusIndexPool> pools = new HashMap<>();
        for (Integer index : indexes) {
            int poolId = index / appConfiguration.getStatusListIndexAllocationBlockSize();

            SessionStatusIndexPool indexHolder = pools.get(poolId);
            if (indexHolder == null) {
                indexHolder = sessionStatusIndexPoolService.getPoolByIndex(index);
                log.debug("{} Found pool {} by index {}", logPrefix, indexHolder.getDn(), index);
                pools.put(indexHolder.getId(), indexHolder);
            }
        }
        return pools.values();
    }

    private void updateWithLockSilently(SessionStatusIndexPool pool, List<Integer> indexes, TokenStatus status) {
        String logPrefix = sessionStatusIndexPoolService.logPrefix();

        try {
            sessionStatusIndexPoolService.updateWithLock(pool.getDn(), indexes, status);
        } catch (Exception e) {
            log.error(logPrefix + "Failed to persist status index pool " + pool.getId(), e);
        }
    }

    public Integer next() {
        String logPrefix = sessionStatusIndexPoolService.logPrefix();
        try {
            final Integer first = nextIndex().getFirst();
            log.trace("{} Next index: {}", logPrefix, first);
            return first;
        } catch (Exception e) {
            // return -1. Even if we failed to get next index, we don't want to fail entire call
            log.error(logPrefix + "Failed to get next index", e);
            return -1;
        }
    }

    public Pair<Integer, SessionStatusIndexPool> nextIndex() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    	// Create copy of variable to make sure that another Thread not changed it
    	SessionStatusIndexPool localTokenPool = sessionPool;
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
			if (System.identityHashCode(localTokenPool) != System.identityHashCode(sessionPool)) {
				// Try to get index from new pool which another threads gets already
				localTokenPool = sessionPool;
				if (localTokenPool != null) {
                    newIndex = localTokenPool.nextIndex();
                    if (newIndex != -1) {
                        return new Pair<>(newIndex, localTokenPool);
                    }
                }
			}
			
			// Allocate new TokenPool
			sessionPool = sessionStatusIndexPoolService.allocate(clusterManager.getClusterNodeId());

			newIndex = sessionPool.nextIndex();
			return new Pair<>(newIndex, sessionPool);
		} finally {
			allocatedLock.unlock();
		}
    }
}
