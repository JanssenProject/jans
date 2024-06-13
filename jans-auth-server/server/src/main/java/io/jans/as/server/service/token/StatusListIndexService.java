package io.jans.as.server.service.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterManager;
import io.jans.as.server.service.cluster.TokenPoolService;
import io.jans.model.token.TokenPool;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.util.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

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
    private TokenPoolService tokenPoolService;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private ClusterManager clusterManager;

    private ReentrantLock allocatedLock = new ReentrantLock();
	
    private TokenPool tokenPool = null;

    public void updateStatusAtIndex(int index, TokenStatus status) {
        try {
            if (index < 0) {
                return; // invalid
            }

            log.trace("Updating status list at index {} with status {} ...", index, status);

            final int bitSize = appConfiguration.getStatusListBitSize();
            final TokenPool indexHolder = tokenPoolService.getTokenPoolByIndex(index);
            final String data = indexHolder.getData();

            final StatusList statusList = StringUtils.isNotBlank(data) ? StatusList.fromEncoded(data, bitSize) : new StatusList(bitSize);
            statusList.set(index, status.getValue());

            indexHolder.setData(statusList.getLst());

            tokenPoolService.updateWithLock(indexHolder);

            log.trace("Updated status list at index {} with status {} successfully.", index, status);

        } catch (Exception e) {
            log.error("Failed to update token list status at index " + index + " with status " + status, e);
        }
    }

    public Integer next() {
        return nextIndex().getFirst();
    }

    public Pair<Integer, TokenPool> nextIndex() {
    	// Create copy of variable to make sure that another Thread not changed it
    	TokenPool localTokenPool = tokenPool;
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
			
			// Allocate new ToeknPool
			tokenPool = tokenPoolService.allocate(clusterManager.getClusterNodeId());

			newIndex = tokenPool.nextIndex();
			return new Pair<>(newIndex, tokenPool);
		} finally {
			allocatedLock.unlock();
		}
    }
}
