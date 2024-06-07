package io.jans.as.server.service.token;

import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.cluster.TokenPoolService;
import io.jans.model.token.TokenPool;
import io.jans.util.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
    private TokenPoolService tokenPoolService;

	@Inject
	private ConfigurationFactory configurationFactory;

    private ReentrantLock allocatedLock = new ReentrantLock();
	
    private TokenPool tokenPool = null;

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
			tokenPool = tokenPoolService.allocate(configurationFactory.getNodeId());

			newIndex = tokenPool.nextIndex();
			return new Pair<>(newIndex, tokenPool);
		} finally {
			allocatedLock.unlock();
		}
    }
}
