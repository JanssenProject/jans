package io.jans.as.server.service.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterNodeManager;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.util.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

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

            final int bitSize = appConfiguration.getStatusListBitSize();

            // first load pool for indexes and update in-memory
            Map<Integer, StatusIndexPool> pools = new HashMap<>();
            for (Integer index : indexes) {
                int poolId = index / appConfiguration.getStatusListIndexAllocationBlockSize();

                StatusIndexPool indexHolder = pools.get(poolId);
                if (indexHolder == null) {
                    indexHolder = statusTokenPoolService.getPoolByIndex(index);
                    log.debug("Found pool {} by index {}", indexHolder.getDn(), index);
                    pools.put(indexHolder.getId(), indexHolder);
                }

                final String data = indexHolder.getData();

                final StatusList statusList = StringUtils.isNotBlank(data) ? StatusList.fromEncoded(data, bitSize) : new StatusList(bitSize);
                statusList.set(index, status.getValue());

                indexHolder.setData(statusList.getLst());
            }

            for (StatusIndexPool pool : pools.values()) {
                updateWithLockSilently(pool);
            }

            log.debug("Updated status list at index {} with status {} successfully.", indexes, status);

        } catch (Exception e) {
            log.error("Failed to update token list status at index " + indexes + " with status " + status, e);
        }
    }

    private void updateWithLockSilently(StatusIndexPool pool) {
        try {
            statusTokenPoolService.updateWithLock(pool);
        } catch (Exception e) {
            log.error("Failed to persist status index pool " + pool.getId(), e);
        }
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

    public Pair<Integer, StatusIndexPool> nextIndex() {
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
