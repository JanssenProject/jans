/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterLock;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.aggregation.AggregateData;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.metric.aggregation.MetricAggregationRunResult.Status;
import io.jans.service.test.MutableClock;
import io.jans.service.test.cluster.FixedClusterLockService;
import io.jans.service.test.metric.RawMetricRows;
import io.jans.service.test.metric.TestMetricAggregationManager;
import io.jans.service.test.metric.TestMetricService;
import io.jans.service.test.orm.SqlTestProfile;

/**
 * {@link MetricAggregationManager} against a real SQL database: three managers race for each slot over
 * a seeded 26-hour series of 2 hosts, 2 metrics (a counter with a subtype and a timer without one; one
 * host reports {@code intervalCount}, the other legacy cumulative counts with a restart).
 *
 * <p>Skipped unless the system property {@value SqlTestProfile#PROFILE_PROPERTY} names a profile
 * directory of {@code jans-orm/integration-test}. The database needs the {@code jansMetric},
 * {@code jansMetricAggregation} and {@code jansClusterLock} tables. The application type is random, so the
 * rows never mix with real data, and every row of it is removed in {@link #cleanUp()}, also after a
 * failure.</p>
 */
public class MetricAggregationManagerDbTest {

    private static final String METRIC_BASE_DN = TestMetricService.DEFAULT_BASE_DN;
    private static final String NODE_BASE_DN = FixedClusterLockService.DEFAULT_BASE_DN;
    private static final String COUNTER = "t05_counter";
    private static final String TIMER = "t05_timer";
    private static final String SUB = "s1";
    private static final Duration REPORT = Duration.ofMinutes(10);
    private static final int REPORT_SECONDS = 600;
    private static final int MANAGERS = 3;
    private static final int ROUNDS = 5;
    private static final int REMOVE_LIMIT = 10_000;

    private final String app = "t05x" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private final String task = MetricAggregationManager.taskName(app);

    private PersistenceEntryManager entryManager;
    private Instant dayStart;
    private final List<MetricDataEntry> seeded = new ArrayList<>();

    @BeforeClass
    public void createEntryManager() throws Exception {
        entryManager = SqlTestProfile.createEntryManager();
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        if (entryManager == null) {
            return;
        }

        try {
            String aggregationBaseDn = MetricAggregator.aggregationBaseDn(METRIC_BASE_DN);
            Filter byApp = Filter.createEqualityFilter("jansAppTyp", app);
            entryManager.remove(METRIC_BASE_DN, MetricDataEntry.class, byApp, REMOVE_LIMIT);
            entryManager.remove(aggregationBaseDn, MetricAggregationEntry.class, byApp, REMOVE_LIMIT);
            Filter byTask = Filter.createEqualityFilter("jansType", task);
            entryManager.remove(NODE_BASE_DN, ClusterLock.class, byTask, REMOVE_LIMIT);

            for (MetricDataEntry entry : entryManager.findEntries(METRIC_BASE_DN, MetricDataEntry.class, byApp)) {
                entryManager.remove(entry.getDn(), MetricDataEntry.class);
            }
            for (MetricAggregationEntry entry : entryManager.findEntries(aggregationBaseDn, MetricAggregationEntry.class, byApp)) {
                entryManager.remove(entry.getDn(), MetricAggregationEntry.class);
            }
            for (ClusterLock entry : entryManager.findEntries(NODE_BASE_DN, ClusterLock.class, byTask)) {
                entryManager.remove(entry.getDn(), ClusterLock.class);
            }
        } finally {
            entryManager.destroy();
        }
    }

    @Test
    public void threeManagersAggregateSeededSeriesOnDatabase() throws Exception {
        dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(2));
        seed();

        MutableClock clock = new MutableClock(dayStart.plus(Duration.ofHours(25)).plus(Duration.ofMinutes(30)));
        MetricAggregationConfiguration configuration = new MetricAggregationConfiguration();
        configuration.setMaxBucketsPerRun(20);
        List<TestMetricAggregationManager> managers = new ArrayList<>();
        for (int i = 0; i < MANAGERS; i++) {
            FixedClusterLockService lockService = new FixedClusterLockService(entryManager, NODE_BASE_DN, "t05-key-" + i, "t05-node-" + i,
                    null, clock);
            managers.add(new TestMetricAggregationManager(new TestMetricService(entryManager, app, "t05-node-" + i), lockService,
                    configuration, REPORT_SECONDS, 2));
        }

        List<Integer> hourlyPerRound = new ArrayList<>();
        Set<Long> slots = new HashSet<>();
        for (int round = 0; round < ROUNDS; round++) {
            List<MetricAggregationRunResult> results = runConcurrently(managers);
            MetricAggregationRunResult done = null;
            int notClaimed = 0;
            for (MetricAggregationRunResult result : results) {
                if (result.getStatus() == Status.DONE) {
                    assertTrue(done == null, "more than one DONE in round " + round + ": " + results);
                    done = result;
                } else if (result.getStatus() == Status.NOT_CLAIMED) {
                    notClaimed++;
                }
            }
            assertNotNull(done, "no DONE in round " + round + ": " + results);
            assertEquals(notClaimed, MANAGERS - 1, "round " + round + ": " + results);
            assertEquals(done.getTotalFailedBuckets(), 0, "round " + round + ": " + done);
            hourlyPerRound.add(done.getBuckets(MetricAggregationType.HOURLY));
            slots.add(done.getSlot());
            clock.advance(Duration.ofSeconds(configuration.getRunIntervalSeconds()));
        }
        assertEquals(hourlyPerRound, Arrays.asList(20, 20, 8, 2, 2));

        List<ClusterLock> claims = new ArrayList<>();
        for (ClusterLock entry : entryManager.findEntries(NODE_BASE_DN, ClusterLock.class, Filter.createEqualityFilter("jansType", task))) {
            if (!ClusterLock.STATUS_STATE.equals(entry.getStatus())) {
                claims.add(entry);
            }
        }
        assertEquals(claims.size(), ROUNDS, "one claim per slot");
        Set<String> claimIds = new HashSet<>();
        for (ClusterLock claim : claims) {
            assertEquals(claim.getStatus(), ClusterLock.STATUS_DONE, claim.getId());
            claimIds.add(claim.getId());
        }
        Set<String> expectedIds = new HashSet<>();
        for (Long slot : slots) {
            expectedIds.add(task + "_" + slot);
        }
        assertEquals(claimIds, expectedIds);

        Map<String, Long> expectedHourly = expectedHourly();
        Map<String, Long> actualHourly = new TreeMap<>();
        Map<String, AggregateData> daily = new HashMap<>();
        for (MetricAggregationEntry entry : entryManager.findEntries(MetricAggregator.aggregationBaseDn(METRIC_BASE_DN),
                MetricAggregationEntry.class, Filter.createEqualityFilter("jansAppTyp", app))) {
            AggregateData data = MetricAggregator.readAggregateData(entry.getData());
            assertNotNull(data, entry.getDn());
            String key = key(entry.getMetricType(), entry.getMetricSubType(), entry.getStartDate().toInstant());
            if (entry.getAggregationType() == MetricAggregationType.HOURLY) {
                actualHourly.put(key, data.getCount());
                assertTrue(data.isComplete(), key);
                assertEquals(entry.getEndDate().toInstant(), entry.getStartDate().toInstant().plus(Duration.ofHours(1)).minusMillis(1));
            } else if (entry.getAggregationType() == MetricAggregationType.DAILY) {
                daily.put(key, data);
            }
        }
        assertEquals(actualHourly.size(), 26 * 2);
        assertEquals(actualHourly, expectedHourly);

        for (String metric : Arrays.asList(COUNTER, TIMER)) {
            String subType = COUNTER.equals(metric) ? SUB : null;
            long expectedDay = 0;
            for (int hour = 0; hour < 24; hour++) {
                expectedDay += expectedHourly.get(key(metric, subType, dayStart.plus(Duration.ofHours(hour))));
            }
            AggregateData dayData = daily.get(key(metric, subType, dayStart));
            assertNotNull(dayData, "DAILY " + metric);
            assertEquals(dayData.getCount(), expectedDay, "DAILY " + metric);
            assertEquals(dayData.getNodes(), 2);
            assertTrue(dayData.isComplete());
            assertTrue(dayData.isLegacyDelta());

            AggregateData previousDay = daily.get(key(metric, subType, dayStart.minus(Duration.ofDays(1))));
            assertNotNull(previousDay, "DAILY of the previous day " + metric);
            assertEquals(previousDay.getCount(), (long) expectedHourly.get(key(metric, subType, dayStart.minus(Duration.ofHours(1))))
                    , "DAILY of the previous day " + metric);
        }
    }

    private void seed() {
        Date expiration = Date.from(Instant.now().plus(Duration.ofDays(1)));
        long h1Counter = 0;
        long h2Counter = 0;
        long h1Timer = 0;
        long h2Timer = 0;
        int i = 0;
        for (Instant report = dayStart.minus(Duration.ofHours(1)).plus(REPORT); !report.isAfter(dayStart.plus(Duration.ofHours(25)));
                report = report.plus(REPORT), i++) {
            long h1CounterIncrement = (i % 5) + 1;
            h1Counter += h1CounterIncrement;
            long h2CounterIncrement = (i % 3) + 2;
            h2Counter = (i == 80) ? h2CounterIncrement : h2Counter + h2CounterIncrement;
            long h1TimerIncrement = (i % 4) + 1;
            h1Timer += h1TimerIncrement;
            h2Timer = (i == 100) ? 2 : h2Timer + 2;

            add(RawMetricRows.row(METRIC_BASE_DN, app, "t05-h1", COUNTER, SUB, report, REPORT, RawMetricRows.counter(h1Counter,
                    h1CounterIncrement), expiration));
            add(RawMetricRows.row(METRIC_BASE_DN, app, "t05-h2", COUNTER, SUB, report, REPORT, RawMetricRows.legacyCounter(h2Counter),
                    expiration));
            add(RawMetricRows.row(METRIC_BASE_DN, app, "t05-h1", TIMER, null, report, REPORT,
                    RawMetricRows.timer(h1Timer, h1TimerIncrement, 1.0, 20.0, 5.0), expiration));
            add(RawMetricRows.row(METRIC_BASE_DN, app, "t05-h2", TIMER, null, report, REPORT, RawMetricRows.legacyTimer(h2Timer, 2.0, 30.0, 7.0),
                    expiration));
        }
        assertEquals(seeded.size(), 26 * 6 * 4);
    }

    private void add(MetricDataEntry entry) {
        entryManager.persist(entry);
        seeded.add(entry);
    }

    /**
     * Expected HOURLY counts, computed directly from the seeded rows: {@code intervalCount} when present;
     * otherwise the difference to the previous row of the series, the count itself after a restart, and 0
     * for the first row of a series.
     */
    private Map<String, Long> expectedHourly() {
        Map<String, Long> previousCounts = new HashMap<>();
        Map<String, Long> result = new TreeMap<>();
        for (MetricDataEntry row : seeded) {
            String series = row.getNodeIdentifier() + "|" + row.getMetricType();
            String data = row.getData();
            long count = Long.parseLong(data.replaceAll("^\\{\"count\":(\\d+).*$", "$1"));
            Long previous = previousCounts.put(series, count);
            long delta;
            if (data.contains("\"intervalCount\":")) {
                delta = Long.parseLong(data.replaceAll("^.*\"intervalCount\":(\\d+).*$", "$1"));
            } else if (previous == null) {
                delta = 0;
            } else if (count < previous) {
                delta = count;
            } else {
                delta = count - previous;
            }
            Instant hour = row.getEndDate().toInstant().truncatedTo(ChronoUnit.HOURS);
            result.merge(key(row.getMetricType(), row.getMetricSubType(), hour), delta, Long::sum);
        }
        return result;
    }

    private static String key(String metricType, String subType, Instant start) {
        return metricType + "|" + (subType == null ? "" : subType) + "|" + start;
    }

    private static List<MetricAggregationRunResult> runConcurrently(List<TestMetricAggregationManager> managers) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(managers.size());
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<MetricAggregationRunResult>> futures = new ArrayList<>();
            for (TestMetricAggregationManager manager : managers) {
                Callable<MetricAggregationRunResult> run = () -> {
                    start.await();
                    return manager.runOnce();
                };
                futures.add(executor.submit(run));
            }
            start.countDown();
            List<MetricAggregationRunResult> results = new ArrayList<>();
            for (Future<MetricAggregationRunResult> future : futures) {
                results.add(future.get(10, TimeUnit.MINUTES));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

}
