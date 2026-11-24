/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterLock;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.aggregation.AggregateData;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.model.metric.aggregation.TimerAggregateData;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;
import io.jans.service.cdi.event.MetricAggregationEvent;
import io.jans.service.cluster.ClusterLockService;
import io.jans.service.metric.aggregation.MetricAggregationRunResult.Status;
import io.jans.service.test.MutableClock;
import io.jans.service.test.cluster.FixedClusterLockService;
import io.jans.service.test.metric.RawMetricRows;
import io.jans.service.test.metric.TestMetricAggregationManager;
import io.jans.service.test.metric.TestMetricService;
import io.jans.service.test.orm.InMemoryEntryManager;

/**
 * {@link MetricAggregationManager} on an in-memory entry manager with a fake clock.
 */
public class MetricAggregationManagerTest {

    private static final String APP = "test_app";
    private static final String TASK = MetricAggregationManager.taskName(APP);
    private static final String METRIC_BASE_DN = TestMetricService.DEFAULT_BASE_DN;
    private static final String COUNTER = "c1";
    private static final String TIMER = "t1";
    private static final String SUB = "s";
    private static final Duration REPORT = Duration.ofMinutes(5);
    private static final int REPORT_SECONDS = 300;
    /** A 5-minute boundary; the last closed hour is [11:00, 12:00). */
    private static final Instant T0 = Instant.parse("2026-11-24T12:15:00Z");
    private static final Date FAR_FUTURE = Date.from(Instant.parse("2030-01-01T00:00:00Z"));

    private InMemoryEntryManager store;
    private MutableClock clock;
    private final List<Filter> rawQueries = new CopyOnWriteArrayList<>();
    private final List<Filter> aggregateQueries = new CopyOnWriteArrayList<>();
    private final AtomicInteger allCalls = new AtomicInteger();
    private final Map<String, AtomicInteger> aggregateCalls = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile BiFunction<String, Object[], RuntimeException> extraFailure;

    @BeforeMethod
    public void setUp() {
        store = new InMemoryEntryManager();
        clock = new MutableClock(T0);
        rawQueries.clear();
        aggregateQueries.clear();
        allCalls.set(0);
        aggregateCalls.clear();
        extraFailure = null;
        store.setFailureInjector((name, args) -> {
            allCalls.incrementAndGet();
            boolean aggregateCall = (args.length > 0) && ((args[0] instanceof MetricAggregationEntry)
                    || (("find".equals(name)) && (args[0] == MetricAggregationEntry.class)));
            if (aggregateCall) {
                aggregateCalls.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
            }
            if ("findEntries".equals(name) && (args.length == 9)) {
                if (args[1] == MetricDataEntry.class) {
                    rawQueries.add((Filter) args[2]);
                } else if (args[1] == MetricAggregationEntry.class) {
                    aggregateQueries.add((Filter) args[2]);
                }
            }
            BiFunction<String, Object[], RuntimeException> extra = extraFailure;
            return extra == null ? null : extra.apply(name, args);
        });
    }

    // ---------------------------------------------------------------- gating, claims, statuses

    @Test
    public void gatingOffReadsAndWritesNothing() {
        TestMetricService metricService = new TestMetricService(store.getEntryManager(), APP, "agg-node");
        metricService.setReporterEnabled(false);
        TestMetricAggregationManager manager = new TestMetricAggregationManager(metricService, lockService("a"), config(), REPORT_SECONDS, 2);
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        int seeded = store.size();
        allCalls.set(0);

        assertEquals(manager.runOnce().getStatus(), Status.DISABLED);

        MetricAggregationConfiguration disabled = config();
        disabled.setEnabled(false);
        metricService.setReporterEnabled(true);
        manager.setConfiguration(disabled);
        assertEquals(manager.runOnce().getStatus(), Status.DISABLED);

        assertEquals(allCalls.get(), 0, "entry manager calls");
        assertEquals(store.size(), seeded);
    }

    @Test
    public void finishedRunIsDoneWithSummaryAndCorrectTotals() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        seedCounter("h2", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 2);
        seedTimer("h1", TIMER, SUB, at("2026-11-24T10:00:00Z"), at("2026-11-24T11:00:00Z"));

        MetricAggregationRunResult result = manager("a", 2).runOnce();

        assertEquals(result.getStatus(), Status.DONE);
        assertEquals(result.getBuckets(MetricAggregationType.HOURLY), 48);
        assertEquals(result.getTotalFailedBuckets(), 0);
        assertEquals(result.getRowsWritten(), 4);
        for (String hour : Arrays.asList("09", "10", "11")) {
            AggregateData data = hourly(COUNTER, null, "2026-11-24T" + hour + ":00:00Z");
            assertEquals(data.getCount(), 36, hour);
            assertEquals(data.getNodes(), 2);
            assertEquals(data.getSourceRows(), 24);
            assertTrue(data.isComplete());
            assertFalse(data.isLegacyDelta());
        }
        TimerAggregateData timer = (TimerAggregateData) hourly(TIMER, SUB, "2026-11-24T10:00:00Z");
        assertEquals(timer.getCount(), 24);

        MetricAggregationEntry entry = store.get(dn(MetricAggregationType.HOURLY, COUNTER, null, "2026-11-24T09:00:00Z"),
                MetricAggregationEntry.class);
        assertEquals(entry.getStartDate().toInstant(), at("2026-11-24T09:00:00Z"));
        assertEquals(entry.getEndDate().toInstant(), at("2026-11-24T09:59:59.999Z"));
        assertEquals(entry.getNodeIdentifier(), "agg-node-a");

        ClusterLock claim = claim(slot(T0));
        assertEquals(claim.getStatus(), ClusterLock.STATUS_DONE);
        assertTrue(claim.getData().contains("\"slot\":" + slot(T0)), claim.getData());
        assertTrue(claim.getData().contains("\"HOURLY\":48"), claim.getData());
        assertTrue(claim.getData().contains("\"rows\":4"), claim.getData());

        Map<MetricAggregationType, Instant> state = state();
        assertEquals(state.get(MetricAggregationType.HOURLY), at("2026-11-24T12:00:00Z"));
        assertEquals(state.get(MetricAggregationType.DAILY), at("2026-11-24T00:00:00Z"));
        assertEquals(state.get(MetricAggregationType.WEEKLY), at("2026-11-23T00:00:00Z"));
        assertEquals(state.get(MetricAggregationType.MONTHLY), at("2026-11-01T00:00:00Z"));
    }

    @Test
    public void twoManagersAtTheSameInstantExactlyOneWorks() throws Exception {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        List<TestMetricAggregationManager> managers = Arrays.asList(manager("a", 2), manager("b", 2));

        for (int round = 0; round < 3; round++) {
            List<MetricAggregationRunResult> results = runConcurrently(managers);
            int done = 0;
            int notClaimed = 0;
            for (MetricAggregationRunResult result : results) {
                if (result.getStatus() == Status.DONE) {
                    done++;
                } else if (result.getStatus() == Status.NOT_CLAIMED) {
                    notClaimed++;
                }
            }
            assertEquals(done, 1, "round " + round + ": " + results);
            assertEquals(notClaimed, 1, "round " + round + ": " + results);
            clock.advance(Duration.ofSeconds(300));
        }

        List<ClusterLock> claims = claims();
        assertEquals(claims.size(), 3);
        for (ClusterLock claim : claims) {
            assertEquals(claim.getStatus(), ClusterLock.STATUS_DONE);
        }
        assertEquals(hourly(COUNTER, null, "2026-11-24T10:00:00Z").getCount(), 12);
    }

    @Test
    public void activePreviousSlotGivesSkippedThenNextSlotIsDone() {
        FixedClusterLockService other = lockService("other");
        ClusterLock running = other.tryClaim(TASK, Long.toString(slot(T0) - 1), Duration.ofSeconds(600)).get();
        assertTrue(other.isActive(running, T0));

        MetricAggregationRunResult skipped = manager("a", 2).runOnce();

        assertEquals(skipped.getStatus(), Status.SKIPPED);
        ClusterLock claim = claim(slot(T0));
        assertEquals(claim.getStatus(), ClusterLock.STATUS_SKIPPED);
        assertTrue(claim.getData().contains("\"activeSlot\":" + (slot(T0) - 1)), claim.getData());
        assertTrue(rawQueries.isEmpty());
        assertTrue(store.getAll(MetricAggregationEntry.class).isEmpty());
        assertNull(store.get(lockService("x").buildStateDn(TASK), ClusterLock.class));

        other.release(running, ClusterLock.STATUS_DONE, null);
        clock.advance(Duration.ofSeconds(300));
        assertEquals(manager("a", 2).runOnce().getStatus(), Status.DONE);
        assertEquals(claim(slot(T0) + 1).getStatus(), ClusterLock.STATUS_DONE);
    }

    @Test
    public void scheduledEventsRunOncePerSlot() {
        TestMetricAggregationManager manager = manager("a", 2);

        manager.processMetricAggregationEvent(new MetricAggregationEvent());
        manager.processMetricAggregationEvent(new MetricAggregationEvent());
        assertEquals(claims().size(), 1);
        assertNull(manager.runOnSchedule());

        clock.advance(Duration.ofSeconds(60));
        manager.processMetricAggregationEvent(new MetricAggregationEvent());
        assertEquals(claims().size(), 1);

        clock.advance(Duration.ofSeconds(240));
        manager.processMetricAggregationEvent(new MetricAggregationEvent());
        assertEquals(claims().size(), 2);
    }

    // ---------------------------------------------------------------- crash, re-run, backfill

    @Test
    public void crashWithinLeaseIsSkippedAndNextSlotCompletes() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        Map<String, String> reference = referenceRows(T0.plusSeconds(600));

        crash(1, 600);
        Set<String> afterCrash = rows(MetricAggregationType.HOURLY).keySet();
        assertEquals(afterCrash.size(), 2, "the crashed run wrote hours 09 and 10");
        assertEquals(claim(slot(T0)).getStatus(), ClusterLock.STATUS_RUNNING);
        assertTrue(state().isEmpty(), "the crashed run saved no watermark");

        clock.advance(Duration.ofSeconds(300));
        assertEquals(manager("b", 1).runOnce().getStatus(), Status.SKIPPED);
        assertEquals(claim(slot(T0) + 1).getStatus(), ClusterLock.STATUS_SKIPPED);

        clock.advance(Duration.ofSeconds(300));
        MetricAggregationRunResult result = manager("b", 1).runOnce();
        assertEquals(result.getStatus(), Status.DONE);
        assertEquals(allRows(), reference);
        assertEquals(hourly(COUNTER, null, "2026-11-24T11:00:00Z").getCount(), 12);
        assertNull(store.get(lockService("x").buildClaimDn(TASK, Long.toString(slot(T0))), ClusterLock.class),
                "the expired claim of the crashed run is purged");
    }

    @Test
    public void crashedClaimIsTakenOverAfterTheLease() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        Map<String, String> reference = referenceRows(T0.plusSeconds(300));

        crash(1, 200);
        ClusterLock crashed = claim(slot(T0));
        assertEquals(crashed.getStatus(), ClusterLock.STATUS_RUNNING);

        clock.advance(Duration.ofSeconds(300));
        assertFalse(lockService("x").isActive(crashed, clock.instant()), "lease of 200 s is over");
        TestMetricAggregationManager taker = manager("b", 1, leaseConfig(200));
        MetricAggregationRunResult result = taker.runOnce();

        assertEquals(result.getStatus(), Status.DONE);
        assertEquals(allRows(), reference);
        assertEquals(claim(slot(T0) + 1).getStatus(), ClusterLock.STATUS_DONE);
        assertEquals(state().get(MetricAggregationType.HOURLY), at("2026-11-24T12:00:00Z"));
    }

    @Test
    public void forcedRerunOfTheSameSlotGivesIdenticalRows() {
        seedCounter("h1", COUNTER, null, at("2026-11-23T20:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        seedCounter("h2", COUNTER, SUB, at("2026-11-23T20:00:00Z"), at("2026-11-24T12:00:00Z"), 3);
        seedTimer("h2", TIMER, null, at("2026-11-23T22:00:00Z"), at("2026-11-24T03:00:00Z"));

        assertEquals(manager("a", 2).runOnce().getStatus(), Status.DONE);
        Map<String, String> first = allRows();
        assertFalse(rows(MetricAggregationType.DAILY).isEmpty());

        String claimDn = lockService("x").buildClaimDn(TASK, Long.toString(slot(T0)));
        store.getEntryManager().remove(claimDn, ClusterLock.class);
        assertEquals(manager("b", 2).runOnce().getStatus(), Status.DONE, "re-run with the stored watermarks");
        assertEquals(allRows(), first);

        store.getEntryManager().remove(claimDn, ClusterLock.class);
        store.getEntryManager().remove(lockService("x").buildStateDn(TASK), ClusterLock.class);
        MetricAggregationRunResult full = manager("c", 2).runOnce();
        assertEquals(full.getStatus(), Status.DONE, "re-run from scratch");
        assertEquals(full.getBuckets(MetricAggregationType.HOURLY), 48);
        assertEquals(allRows(), first);
    }

    @Test
    public void backfillAfterThreeDaysOfDowntimeRunsInChunksThenDaily() {
        Set<Instant> emptyHours = new java.util.HashSet<>(Arrays.asList(at("2026-11-22T03:00:00Z"), at("2026-11-22T04:00:00Z"),
                at("2026-11-23T15:00:00Z")));
        Instant from = at("2026-11-21T12:00:00Z");
        Instant to = at("2026-11-24T12:00:00Z");
        for (Instant hour = from; hour.isBefore(to); hour = hour.plus(Duration.ofHours(1))) {
            if (!emptyHours.contains(hour)) {
                seedCounter("h1", COUNTER, null, hour, hour.plus(Duration.ofHours(1)), 1);
            }
        }
        FixedClusterLockService seedLock = lockService("seed");
        assertTrue(seedLock.saveState(TASK, "{\"HOURLY\":\"2026-11-21T12:00:00Z\",\"DAILY\":\"2026-11-21T00:00:00Z\","
                + "\"WEEKLY\":\"2026-11-16T00:00:00Z\",\"MONTHLY\":\"2026-11-01T00:00:00Z\"}"));

        MetricAggregationConfiguration configuration = config();
        configuration.setMaxBucketsPerRun(24);
        TestMetricAggregationManager manager = manager("a", 4, configuration);

        List<Integer> hourlyPerRun = new ArrayList<>();
        List<Integer> dailyPerRun = new ArrayList<>();
        for (int run = 0; run < 4; run++) {
            MetricAggregationRunResult result = manager.runOnce();
            assertEquals(result.getStatus(), Status.DONE);
            hourlyPerRun.add(result.getBuckets(MetricAggregationType.HOURLY));
            dailyPerRun.add(result.getBuckets(MetricAggregationType.DAILY));
            clock.advance(Duration.ofSeconds(300));
        }

        assertEquals(hourlyPerRun, Arrays.asList(24, 24, 24, 2));
        assertEquals(dailyPerRun, Arrays.asList(1, 1, 1, 1), "DAILY follows the HOURLY watermark");
        assertEquals(state().get(MetricAggregationType.HOURLY), to);
        assertEquals(state().get(MetricAggregationType.DAILY), at("2026-11-24T00:00:00Z"));

        Map<Instant, Integer> queriesByStart = new TreeMap<>();
        for (Filter query : rawQueries) {
            queriesByStart.merge(lowerBound(query, "jansEndDate"), 1, Integer::sum);
        }
        for (Instant emptyHour : emptyHours) {
            assertEquals(queriesByStart.get(emptyHour), Integer.valueOf(1), "queries of empty hour " + emptyHour);
            assertNull(store.get(dn(MetricAggregationType.HOURLY, COUNTER, null, emptyHour.toString()), MetricAggregationEntry.class));
        }

        assertEquals(rows(MetricAggregationType.HOURLY).size(), 72 - emptyHours.size());
        assertEquals(dailyCount("2026-11-21T00:00:00Z"), 12 * 12);
        assertEquals(dailyCount("2026-11-22T00:00:00Z"), 22 * 12);
        assertEquals(dailyCount("2026-11-23T00:00:00Z"), 23 * 12);
        for (String day : Arrays.asList("2026-11-21T00:00:00Z", "2026-11-22T00:00:00Z", "2026-11-23T00:00:00Z")) {
            assertEquals(dailyCount(day), sumHourly(at(day)), day);
            assertTrue(daily(day).isComplete());
        }
        assertNotNull(store.get(dn(MetricAggregationType.WEEKLY, COUNTER, null, "2026-11-16T00:00:00Z"), MetricAggregationEntry.class));
    }

    @Test
    public void applicationWithoutRawRowsCatchesUpThenQueriesOncePerClosedBucket() {
        MetricAggregationConfiguration configuration = config();
        configuration.setMaxBucketsPerRun(10);
        TestMetricAggregationManager manager = manager("a", 1, configuration);

        List<Integer> hourlyPerRun = new ArrayList<>();
        for (int run = 0; run < 3; run++) {
            hourlyPerRun.add(manager.runOnce().getBuckets(MetricAggregationType.HOURLY));
            clock.advance(Duration.ofSeconds(300));
        }
        assertEquals(hourlyPerRun, Arrays.asList(10, 10, 4));
        assertEquals(state().get(MetricAggregationType.HOURLY), at("2026-11-24T12:00:00Z"));

        rawQueries.clear();
        aggregateQueries.clear();
        int persistsBefore = store.getCallCount("persist");
        MetricAggregationRunResult steady = manager.runOnce();
        assertEquals(steady.getStatus(), Status.DONE);
        assertEquals(rawQueries.size(), 2, "one query per HOURLY bucket of the recompute window");
        assertEquals(aggregateQueries.size(), 3, "one query each for the DAILY, WEEKLY and MONTHLY recompute bucket");
        assertEquals(store.getCallCount("persist") - persistsBefore, 1, "only the claim is inserted");
        assertTrue(store.getAll(MetricAggregationEntry.class).isEmpty());

        clock.advance(Duration.ofHours(1));
        rawQueries.clear();
        MetricAggregationRunResult nextHour = manager.runOnce();
        assertEquals(nextHour.getBuckets(MetricAggregationType.HOURLY), 2);
        assertEquals(rawQueries.size(), 2);
        assertEquals(state().get(MetricAggregationType.HOURLY), at("2026-11-24T13:00:00Z"));
    }

    // ---------------------------------------------------------------- write order (D14)

    @Test
    public void newBucketsArePersistFirstAndSteadyStateRecomputeIsFindFirst() {
        seedCounter("h1", COUNTER, null, at("2026-11-23T20:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        seedCounter("h2", COUNTER, SUB, at("2026-11-23T20:00:00Z"), at("2026-11-24T12:00:00Z"), 2);

        MetricAggregationRunResult first = manager("a", 2).runOnce();
        assertEquals(first.getStatus(), Status.DONE);
        assertTrue(first.getRowsWritten() > 0);
        assertEquals(aggregateCalls("persist"), first.getRowsWritten(), "new buckets: one persist per row");
        assertEquals(aggregateCalls("find"), 0, "new buckets: no find before persist");
        assertEquals(aggregateCalls("merge"), 0);
        Map<String, String> rowsAfterFirst = allRows();

        for (int run = 0; run < 2; run++) {
            clock.advance(Duration.ofSeconds(300));
            aggregateCalls.clear();
            int persistsBefore = store.getCallCount("persist");
            MetricAggregationRunResult steady = manager("b", 2).runOnce();

            assertEquals(steady.getStatus(), Status.DONE);
            assertEquals(steady.getBuckets(MetricAggregationType.HOURLY), 2);
            assertEquals(store.getCallCount("persist") - persistsBefore, 1, "only the claim is inserted");
            assertEquals(aggregateCalls("persist"), 0, "recompute buckets: no aggregate persist");
            assertEquals(aggregateCalls("find"), steady.getRowsWritten());
            assertEquals(aggregateCalls("merge"), steady.getRowsWritten());
            assertTrue(steady.getRowsWritten() >= 6, "2 HOURLY buckets and 1 DAILY bucket of 2 keys: " + steady);
            assertEquals(allRows(), rowsAfterFirst);
        }

        String removed = dn(MetricAggregationType.HOURLY, COUNTER, null, "2026-11-24T11:00:00Z");
        store.getEntryManager().remove(removed, MetricAggregationEntry.class);
        clock.advance(Duration.ofSeconds(300));
        aggregateCalls.clear();
        assertEquals(manager("c", 2).runOnce().getStatus(), Status.DONE);
        assertEquals(aggregateCalls("persist"), 1, "a missing row inside the recompute window is inserted after the find");
        assertEquals(allRows(), rowsAfterFirst);
    }

    @Test
    public void persistFirstFallsBackToMergeOnDuplicate() {
        MetricAggregationEntry stored = aggregateEntry("{\"kind\":\"counter\",\"count\":1}");
        store.put(stored);
        MetricAggregationEntry computed = aggregateEntry("{\"kind\":\"counter\",\"count\":5}");

        MetricAggregationManager.upsert(store.getEntryManager(), computed, false);

        assertEquals(store.get(computed.getDn(), MetricAggregationEntry.class).getData(), computed.getData());
        assertEquals(aggregateCalls("persist"), 1);
        assertEquals(aggregateCalls("find"), 1);
        assertEquals(aggregateCalls("merge"), 1);
    }

    @Test
    public void findFirstRaceFallsBackToMergeOnDuplicate() {
        MetricAggregationEntry stored = aggregateEntry("{\"kind\":\"counter\",\"count\":1}");
        store.put(stored);
        MetricAggregationEntry computed = aggregateEntry("{\"kind\":\"counter\",\"count\":5}");
        AtomicInteger finds = new AtomicInteger();
        // The first find misses the row, as if another node inserted it right after the read
        extraFailure = (name, args) -> "find".equals(name) && (args[0] == MetricAggregationEntry.class) && (finds.incrementAndGet() == 1)
                ? InMemoryEntryManager.missing(String.valueOf(args[1]))
                : null;

        MetricAggregationManager.upsert(store.getEntryManager(), computed, true);

        assertEquals(store.get(computed.getDn(), MetricAggregationEntry.class).getData(), computed.getData());
        assertEquals(aggregateCalls("find"), 2);
        assertEquals(aggregateCalls("persist"), 1);
        assertEquals(aggregateCalls("merge"), 1);
    }

    @Test
    public void findFirstInsertsWhenTheRowIsMissing() {
        MetricAggregationEntry computed = aggregateEntry("{\"kind\":\"counter\",\"count\":5}");

        MetricAggregationManager.upsert(store.getEntryManager(), computed, true);

        assertEquals(store.get(computed.getDn(), MetricAggregationEntry.class).getData(), computed.getData());
        assertEquals(aggregateCalls("find"), 1);
        assertEquals(aggregateCalls("persist"), 1);
        assertEquals(aggregateCalls("merge"), 0);
    }

    @Test
    public void findFirstTreatsANullResultAsMissing() {
        InMemoryEntryManager nullOnMissing = new InMemoryEntryManager() {
            @Override
            protected Object find(Class<?> entryClass, Object primaryKey) {
                return get(String.valueOf(primaryKey), entryClass);
            }
        };
        MetricAggregationEntry computed = aggregateEntry("{\"kind\":\"counter\",\"count\":5}");

        MetricAggregationManager.upsert(nullOnMissing.getEntryManager(), computed, true);

        assertEquals(nullOnMissing.get(computed.getDn(), MetricAggregationEntry.class).getData(), computed.getData());
        assertEquals(nullOnMissing.getCallCount("persist"), 1);
    }

    @Test
    public void findFirstReadErrorFailsWithoutInsert() {
        MetricAggregationEntry computed = aggregateEntry("{\"kind\":\"counter\",\"count\":5}");
        extraFailure = (name, args) -> "find".equals(name)
                ? new EntryPersistenceException("Failed to find entry", new SearchException("query failed", new SQLException("timeout")))
                : null;

        try {
            MetricAggregationManager.upsert(store.getEntryManager(), computed, true);
            fail("A read error must fail the bucket");
        } catch (EntryPersistenceException expected) {
            assertEquals(aggregateCalls("persist"), 0);
        }

        assertTrue(MetricAggregationManager.isMissingEntry(InMemoryEntryManager.missing("dn")));
        assertTrue(MetricAggregationManager.isMissingEntry(new EntryPersistenceException("no cause")));
        assertFalse(MetricAggregationManager.isMissingEntry(new EntryPersistenceException("failed",
                new SearchException("query failed", new SQLException("timeout")))));
    }

    // ---------------------------------------------------------------- legacy rows

    @Test
    public void legacyRowsUseOneLookbackQueryAndCarryCountsBetweenBuckets() {
        long count = 1000;
        for (Instant report = at("2026-11-23T08:05:00Z"); !report.isAfter(at("2026-11-24T12:00:00Z")); report = report.plus(REPORT)) {
            count += 3;
            store.put(RawMetricRows.row(METRIC_BASE_DN, APP, "h1", COUNTER, null, report, REPORT, RawMetricRows.legacyCounter(count),
                    FAR_FUTURE));
        }

        MetricAggregationRunResult result = manager("a", 1).runOnce();

        assertEquals(result.getStatus(), Status.DONE);
        assertEquals(result.getBuckets(MetricAggregationType.HOURLY), 24);
        assertEquals(rawQueries.size(), 25, "24 bucket queries plus one lookback query");
        Instant firstBucket = at("2026-11-23T12:00:00Z");
        assertEquals(lowerBound(rawQueries.get(1), "jansEndDate"), firstBucket.minus(Duration.ofDays(1)));
        AggregateData first = hourly(COUNTER, null, firstBucket.toString());
        assertEquals(first.getCount(), 36, "previous count comes from the lookback, not a baseline of 0");
        assertTrue(first.isLegacyDelta());
        assertEquals(hourly(COUNTER, null, "2026-11-24T05:00:00Z").getCount(), 36, "carried from the previous bucket");
    }

    // ---------------------------------------------------------------- failures

    @Test
    public void missingClusterLockTableKeepsTheRunIdle() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        extraFailure = (name, args) -> "persist".equals(name) && (args[0] instanceof ClusterLock)
                ? InMemoryEntryManager.missingTable(((ClusterLock) args[0]).getDn())
                : null;
        TestMetricAggregationManager manager = manager("a", 2);

        for (int run = 0; run < 2; run++) {
            assertEquals(manager.runOnce().getStatus(), Status.NOT_CLAIMED);
            clock.advance(Duration.ofSeconds(300));
        }

        assertEquals(store.getCallCount("find"), 0, "no state read");
        assertTrue(rawQueries.isEmpty());
        assertTrue(store.getAll(MetricAggregationEntry.class).isEmpty());
        assertTrue(claims().isEmpty());
    }

    @Test
    public void missingAggregationTableFailsTheBucketWithoutException() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        extraFailure = (name, args) -> "persist".equals(name) && (args[0] instanceof MetricAggregationEntry)
                ? InMemoryEntryManager.missingTable(((MetricAggregationEntry) args[0]).getDn())
                : null;
        TestMetricAggregationManager manager = manager("a", 1);

        for (int run = 0; run < 2; run++) {
            MetricAggregationRunResult result = manager.runOnce();
            assertEquals(result.getStatus(), Status.DONE);
            assertEquals(result.getFailedBuckets(MetricAggregationType.HOURLY), 1);
            assertEquals(state().get(MetricAggregationType.HOURLY), at("2026-11-24T09:00:00Z"), "stops before the failed bucket");
            clock.advance(Duration.ofSeconds(300));
        }

        assertTrue(store.getAll(MetricAggregationEntry.class).isEmpty());
        assertEquals(claim(slot(T0)).getStatus(), ClusterLock.STATUS_DONE);
    }

    @Test
    public void stateReadErrorSkipsTheRun() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        extraFailure = (name, args) -> "find".equals(name) && String.valueOf(args[1]).contains("_state,")
                ? new EntryPersistenceException("Failed to find entry", new SearchException("query failed", new SQLException("timeout")))
                : null;

        MetricAggregationRunResult result = manager("a", 2).runOnce();

        assertEquals(result.getStatus(), Status.STATE_ERROR);
        assertEquals(claim(slot(T0)).getStatus(), ClusterLock.STATUS_FAILED);
        assertTrue(rawQueries.isEmpty());
        assertTrue(store.getAll(MetricAggregationEntry.class).isEmpty());
    }

    @Test
    public void lostHeartbeatStopsWithoutReleaseOrWatermark() {
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);
        AtomicInteger beats = new AtomicInteger();
        FixedClusterLockService losing = new FixedClusterLockService(store.getEntryManager(), FixedClusterLockService.DEFAULT_BASE_DN, "a",
                "node-a", null, clock) {
            @Override
            public boolean heartbeat(ClusterLock claim) {
                return beats.incrementAndGet() < 3 && super.heartbeat(claim);
            }
        };
        TestMetricAggregationManager manager = new TestMetricAggregationManager(
                new TestMetricService(store.getEntryManager(), APP, "agg-node-a"), losing, config(), REPORT_SECONDS, 1);

        MetricAggregationRunResult result = manager.runOnce();

        assertEquals(result.getStatus(), Status.LOST);
        assertEquals(result.getBuckets(MetricAggregationType.HOURLY), 3);
        assertEquals(claim(slot(T0)).getStatus(), ClusterLock.STATUS_RUNNING);
        assertTrue(state().isEmpty());
    }

    @Test
    public void spannerRefusesTheClaim() {
        store.setPersistenceType(PersistenceEntryManager.PERSITENCE_TYPES.spanner.name());
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);

        assertEquals(manager("a", 2).runOnce().getStatus(), Status.NOT_CLAIMED);

        assertTrue(claims().isEmpty());
        assertTrue(rawQueries.isEmpty());
        assertTrue(store.getAll(MetricAggregationEntry.class).isEmpty());
    }

    @Test
    public void ldapCreatesTheAggregationBranchBeforeWriting() {
        store.setPersistenceType(PersistenceEntryManager.PERSITENCE_TYPES.ldap.name());
        seedCounter("h1", COUNTER, null, at("2026-11-24T09:00:00Z"), at("2026-11-24T12:00:00Z"), 1);

        assertEquals(manager("a", 2).runOnce().getStatus(), Status.DONE);

        SimpleBranch branch = store.get("ou=aggregation," + METRIC_BASE_DN, SimpleBranch.class);
        assertNotNull(branch);
        assertEquals(branch.getOrganizationalUnitName(), "aggregation");
        assertEquals(hourly(COUNTER, null, "2026-11-24T09:00:00Z").getCount(), 12);
    }

    // ---------------------------------------------------------------- plan and state

    @Test
    public void planHasRecomputeWindowPlusForwardBucketsWithinTheCap() {
        Duration grace = PeriodMath.grace(REPORT_SECONDS);
        List<PeriodBucket> caughtUp = MetricAggregationManager.plan(MetricAggregationType.HOURLY, at("2026-11-24T12:00:00Z"), T0, grace, 2,
                48, null);
        assertEquals(starts(caughtUp), Arrays.asList(at("2026-11-24T10:00:00Z"), at("2026-11-24T11:00:00Z")));

        List<PeriodBucket> behind = MetricAggregationManager.plan(MetricAggregationType.HOURLY, at("2026-11-24T05:00:00Z"), T0, grace, 2,
                48, null);
        assertEquals(behind.size(), 7);
        assertEquals(behind.get(0).getStart(), at("2026-11-24T05:00:00Z"));

        List<PeriodBucket> capped = MetricAggregationManager.plan(MetricAggregationType.HOURLY, at("2026-11-20T00:00:00Z"), T0, grace, 2,
                5, null);
        assertEquals(starts(capped).get(0), at("2026-11-20T00:00:00Z"));
        assertEquals(capped.size(), 5);

        List<PeriodBucket> notClosed = MetricAggregationManager.plan(MetricAggregationType.HOURLY, at("2026-11-24T12:00:00Z"),
                at("2026-11-24T13:10:59Z"), grace, 0, 48, null);
        assertTrue(notClosed.isEmpty(), "the 12:00 hour closes at 13:11");

        List<PeriodBucket> daily = MetricAggregationManager.plan(MetricAggregationType.DAILY, at("2026-11-21T00:00:00Z"), T0, grace, 1, 48,
                at("2026-11-23T05:00:00Z"));
        assertEquals(starts(daily), Arrays.asList(at("2026-11-21T00:00:00Z"), at("2026-11-22T00:00:00Z")), "limited by the HOURLY watermark");
    }

    @Test
    public void stateJsonRoundTripAndBadInput() {
        Map<MetricAggregationType, Instant> watermarks = new EnumMap<>(MetricAggregationType.class);
        watermarks.put(MetricAggregationType.HOURLY, at("2026-11-24T12:00:00Z"));
        watermarks.put(MetricAggregationType.MONTHLY, at("2026-11-01T00:00:00Z"));

        String json = MetricAggregationManager.toStateJson(watermarks);
        assertEquals(json, "{\"HOURLY\":\"2026-11-24T12:00:00Z\",\"MONTHLY\":\"2026-11-01T00:00:00Z\"}");
        assertEquals(MetricAggregationManager.parseState(json), watermarks);
        assertTrue(MetricAggregationManager.parseState(null).isEmpty());
        assertTrue(MetricAggregationManager.parseState("not json").isEmpty());
        assertEquals(MetricAggregationManager.parseState("{\"HOURLY\":\"bad\",\"DAILY\":\"2026-11-24T00:00:00Z\",\"X\":1}"),
                Collections.singletonMap(MetricAggregationType.DAILY, at("2026-11-24T00:00:00Z")));
    }

    @Test
    public void taskAndSlotNamesFitTheClaimRules() {
        FixedClusterLockService service = lockService("x");
        for (String app : Arrays.asList("jans_auth", "fido2", "lock", "config_api")) {
            String task = MetricAggregationManager.taskName(app);
            String dn = service.buildClaimDn(task, Long.toString(MetricAggregationManager.slotOf(T0, 1)));
            assertTrue(dn.startsWith("jansId=metric-aggregation_" + app + "_"), dn);
            assertNotNull(service.buildStateDn(task));
        }
        assertEquals(MetricAggregationManager.slotOf(T0, 300), T0.getEpochSecond() / 300);
    }

    // ---------------------------------------------------------------- helpers

    private void crash(int keepDays, int leaseSeconds) {
        FixedClusterLockService crashing = new FixedClusterLockService(store.getEntryManager(), FixedClusterLockService.DEFAULT_BASE_DN,
                "crash", "node-crash", null, clock) {
            private final AtomicInteger beats = new AtomicInteger();

            @Override
            public boolean heartbeat(ClusterLock claim) {
                if (beats.incrementAndGet() > 22) {
                    throw new SimulatedCrash();
                }
                return super.heartbeat(claim);
            }
        };
        TestMetricAggregationManager manager = new TestMetricAggregationManager(
                new TestMetricService(store.getEntryManager(), APP, "agg-node-crash"), crashing, leaseConfig(leaseSeconds), REPORT_SECONDS,
                keepDays);
        try {
            manager.runOnce();
            fail("The run should crash");
        } catch (SimulatedCrash expected) {
            // the claim stays running, as after a JVM crash
        }
    }

    /**
     * Rows that a single clean run at {@code instant} writes from the same raw rows.
     */
    private Map<String, String> referenceRows(Instant instant) {
        InMemoryEntryManager reference = new InMemoryEntryManager();
        for (MetricDataEntry row : store.getAll(MetricDataEntry.class)) {
            reference.put(row);
        }
        MutableClock referenceClock = new MutableClock(instant);
        FixedClusterLockService lock = new FixedClusterLockService(reference.getEntryManager(), "ref", referenceClock);
        TestMetricAggregationManager manager = new TestMetricAggregationManager(
                new TestMetricService(reference.getEntryManager(), APP, "agg-node-ref"), lock, config(), REPORT_SECONDS, 1);
        assertEquals(manager.runOnce().getStatus(), Status.DONE);

        Map<String, String> result = new TreeMap<>();
        for (MetricAggregationEntry entry : reference.getAll(MetricAggregationEntry.class)) {
            result.put(describe(entry), entry.getData());
        }
        return result;
    }

    private int aggregateCalls(String method) {
        AtomicInteger counter = aggregateCalls.get(method);
        return counter == null ? 0 : counter.get();
    }

    private static MetricAggregationEntry aggregateEntry(String data) {
        Instant start = at("2026-11-24T10:00:00Z");
        String id = MetricAggregator.aggregateId(MetricAggregationType.HOURLY, APP, COUNTER, null, start);
        MetricAggregationEntry entry = new MetricAggregationEntry(
                "uniqueIdentifier=" + id + "," + MetricAggregator.aggregationBaseDn(METRIC_BASE_DN), id, Date.from(T0));
        entry.setAggregationType(MetricAggregationType.HOURLY);
        entry.setStartDate(Date.from(start));
        entry.setEndDate(Date.from(start.plus(Duration.ofHours(1)).minusMillis(1)));
        entry.setApplicationType(APP);
        entry.setMetricType(COUNTER);
        entry.setData(data);
        entry.setDeletable(true);
        return entry;
    }

    private Map<String, String> allRows() {
        Map<String, String> result = new TreeMap<>();
        for (MetricAggregationEntry entry : store.getAll(MetricAggregationEntry.class)) {
            result.put(describe(entry), entry.getData());
        }
        return result;
    }

    private static String describe(MetricAggregationEntry entry) {
        return entry.getDn() + " " + entry.getAggregationType() + " " + entry.getStartDate().toInstant() + " " + entry.getEndDate().toInstant()
                + " " + entry.getApplicationType() + " " + entry.getMetricType() + " " + entry.getMetricSubType();
    }

    private Map<String, String> rows(MetricAggregationType type) {
        Map<String, String> result = new TreeMap<>();
        for (MetricAggregationEntry entry : store.getAll(MetricAggregationEntry.class)) {
            if (entry.getAggregationType() == type) {
                result.put(entry.getDn(), entry.getData());
            }
        }
        return result;
    }

    private long sumHourly(Instant dayStart) {
        long sum = 0;
        for (MetricAggregationEntry entry : store.getAll(MetricAggregationEntry.class)) {
            Instant start = entry.getStartDate().toInstant();
            if ((entry.getAggregationType() == MetricAggregationType.HOURLY) && !start.isBefore(dayStart)
                    && start.isBefore(dayStart.plus(Duration.ofDays(1)))) {
                sum += MetricAggregator.readAggregateData(entry.getData()).getCount();
            }
        }
        return sum;
    }

    private AggregateData hourly(String metricType, String subType, String start) {
        return data(MetricAggregationType.HOURLY, metricType, subType, start);
    }

    private AggregateData daily(String start) {
        return data(MetricAggregationType.DAILY, COUNTER, null, start);
    }

    private long dailyCount(String start) {
        return daily(start).getCount();
    }

    private AggregateData data(MetricAggregationType type, String metricType, String subType, String start) {
        MetricAggregationEntry entry = store.get(dn(type, metricType, subType, start), MetricAggregationEntry.class);
        assertNotNull(entry, type + " " + metricType + " " + start);
        return MetricAggregator.readAggregateData(entry.getData());
    }

    private static String dn(MetricAggregationType type, String metricType, String subType, String start) {
        String id = MetricAggregator.aggregateId(type, APP, metricType, subType, at(start));
        return "uniqueIdentifier=" + id + "," + MetricAggregator.aggregationBaseDn(METRIC_BASE_DN);
    }

    private ClusterLock claim(long slot) {
        ClusterLock claim = store.get(lockService("x").buildClaimDn(TASK, Long.toString(slot)), ClusterLock.class);
        assertNotNull(claim, "claim of slot " + slot);
        return claim;
    }

    private List<ClusterLock> claims() {
        List<ClusterLock> result = new ArrayList<>();
        for (ClusterLock entry : store.getAll(ClusterLock.class)) {
            if (!ClusterLock.STATUS_STATE.equals(entry.getStatus())) {
                result.add(entry);
            }
        }
        return result;
    }

    private Map<MetricAggregationType, Instant> state() {
        ClusterLock state = store.get(lockService("x").buildStateDn(TASK), ClusterLock.class);
        return state == null ? new HashMap<>() : MetricAggregationManager.parseState(state.getData());
    }

    private static Instant lowerBound(Filter filter, String attribute) {
        for (Filter child : filter.getFilters()) {
            if ((child.getType() == FilterType.GREATER_OR_EQUAL) && attribute.equals(child.getAttributeName())) {
                return Instant.parse(child.getAssertionValue().toString());
            }
        }
        throw new AssertionError("No lower bound on " + attribute + " in " + filter);
    }

    private static List<Instant> starts(List<PeriodBucket> buckets) {
        List<Instant> result = new ArrayList<>();
        for (PeriodBucket bucket : buckets) {
            result.add(bucket.getStart());
        }
        return result;
    }

    private void seedCounter(String host, String metricType, String subType, Instant from, Instant to, long perRow) {
        long count = 0;
        for (Instant report = from.plus(REPORT); !report.isAfter(to); report = report.plus(REPORT)) {
            count += perRow;
            store.put(RawMetricRows.row(METRIC_BASE_DN, APP, host, metricType, subType, report, REPORT, RawMetricRows.counter(count, perRow),
                    FAR_FUTURE));
        }
    }

    private void seedTimer(String host, String metricType, String subType, Instant from, Instant to) {
        long count = 0;
        for (Instant report = from.plus(REPORT); !report.isAfter(to); report = report.plus(REPORT)) {
            count += 2;
            store.put(RawMetricRows.row(METRIC_BASE_DN, APP, host, metricType, subType, report, REPORT,
                    RawMetricRows.timer(count, 2, 1.0, 9.0, 4.0), FAR_FUTURE));
        }
    }

    private List<MetricAggregationRunResult> runConcurrently(List<TestMetricAggregationManager> managers) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(managers.size());
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<MetricAggregationRunResult>> futures = new ArrayList<>();
            for (TestMetricAggregationManager manager : managers) {
                Callable<MetricAggregationRunResult> task = () -> {
                    start.await();
                    return manager.runOnce();
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            List<MetricAggregationRunResult> results = new ArrayList<>();
            for (Future<MetricAggregationRunResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private TestMetricAggregationManager manager(String key, int keepDays) {
        return manager(key, keepDays, config());
    }

    private TestMetricAggregationManager manager(String key, int keepDays, MetricAggregationConfiguration configuration) {
        return new TestMetricAggregationManager(new TestMetricService(store.getEntryManager(), APP, "agg-node-" + key), lockService(key),
                configuration, REPORT_SECONDS, keepDays);
    }

    private FixedClusterLockService lockService(String key) {
        return new FixedClusterLockService(store.getEntryManager(), key, clock);
    }

    private static MetricAggregationConfiguration config() {
        return new MetricAggregationConfiguration();
    }

    private static MetricAggregationConfiguration leaseConfig(int leaseSeconds) {
        MetricAggregationConfiguration configuration = config();
        configuration.setLeaseSeconds(leaseSeconds);
        return configuration;
    }

    private static long slot(Instant instant) {
        return MetricAggregationManager.slotOf(instant, MetricAggregationConfiguration.DEFAULT_RUN_INTERVAL_SECONDS);
    }

    private static Instant at(String value) {
        return Instant.parse(value);
    }

    private static final class SimulatedCrash extends Error {
        private static final long serialVersionUID = 1L;
    }

}
