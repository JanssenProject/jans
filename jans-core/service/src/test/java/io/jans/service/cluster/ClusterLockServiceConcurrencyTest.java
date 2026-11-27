/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.cluster;

import static org.testng.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterLock;
import io.jans.service.test.MutableClock;
import io.jans.service.test.cluster.ClaimRace;
import io.jans.service.test.cluster.FixedClusterLockService;
import io.jans.service.test.orm.InMemoryEntryManager;

public class ClusterLockServiceConcurrencyTest {

    private static final int THREADS = 8;
    private static final int ROUNDS = 50;

    @Test
    public void exactlyOneOfEightThreadsWinsEachSlot() throws Exception {
        InMemoryEntryManager store = new InMemoryEntryManager();
        MutableClock clock = new MutableClock(Instant.parse("2026-11-24T10:00:00Z"));
        List<FixedClusterLockService> services = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            services.add(new FixedClusterLockService(store.getEntryManager(), "key-" + i, clock));
        }

        for (int round = 0; round < ROUNDS; round++) {
            String slot = String.valueOf(round);
            List<ClusterLock> winners = ClaimRace.run(services, "race-task", slot, Duration.ofMinutes(5));

            assertEquals(winners.size(), 1, "winners of slot " + slot);
            ClusterLock stored = store.get(winners.get(0).getDn(), ClusterLock.class);
            assertEquals(stored.getLockKey(), winners.get(0).getLockKey(), "owner of slot " + slot);
        }

        assertEquals(store.getAll(ClusterLock.class).size(), ROUNDS);
        assertEquals(store.getCallCount("persist"), THREADS * ROUNDS);
    }

}
