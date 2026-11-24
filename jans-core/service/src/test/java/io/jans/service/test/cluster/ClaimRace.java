/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.cluster;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.jans.model.cluster.ClusterLock;
import io.jans.service.cluster.ClusterLockService;

/**
 * Starts {@link ClusterLockService#tryClaim} of one slot on several services at the same moment.
 */
public final class ClaimRace {

    private ClaimRace() {
    }

    /**
     * @return the claims won, one list element per winning service
     */
    public static List<ClusterLock> run(List<? extends ClusterLockService> services, String task, String slotKey, Duration lease)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(services.size());
        try {
            CountDownLatch ready = new CountDownLatch(services.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Optional<ClusterLock>>> futures = new ArrayList<>();
            for (ClusterLockService service : services) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.tryClaim(task, slotKey, lease);
                }));
            }

            if (!ready.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Race threads did not start");
            }
            start.countDown();

            List<ClusterLock> winners = new ArrayList<>();
            for (Future<Optional<ClusterLock>> future : futures) {
                future.get(60, TimeUnit.SECONDS).ifPresent(winners::add);
            }

            return winners;
        } finally {
            executor.shutdownNow();
        }
    }

}
