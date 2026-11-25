/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.lang.reflect.Proxy;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.model.ApplicationType;
import io.jans.model.metric.MetricType;
import io.jans.model.metric.counter.CounterMetricData;
import io.jans.model.metric.counter.CounterMetricEntry;
import io.jans.model.metric.ldap.MetricEntry;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.aggregation.AggregateData;
import io.jans.service.metric.aggregation.MetricAggregator;
import io.jans.service.metric.aggregation.PeriodMath;
import io.jans.model.metric.timer.TimerMetricData;
import io.jans.model.metric.timer.TimerMetricEntry;
import io.jans.orm.PersistenceEntryManager;

public class PersistenceEntryReporterTest {

    private static final MetricRegistration COUNTER_A = new MetricRegistration(MetricType.TOKEN_ACCESS_TOKEN_COUNT, null);
    private static final MetricRegistration COUNTER_B = new MetricRegistration(MetricType.TOKEN_ID_TOKEN_COUNT, "client1");
    private static final MetricRegistration TIMER = new MetricRegistration(MetricType.USER_AUTHENTICATION_RATE, null);

    private CapturingMetricService metricService;
    private MetricRegistry registry;
    private PersistenceEntryReporter reporter;

    private static class CapturingMetricService extends MetricService {

        private static final long serialVersionUID = 1L;

        private final List<MetricRegistration> registrations = new ArrayList<>();
        private final List<List<MetricEntry>> writes = new ArrayList<>();
        private boolean failNextAdd;
        private PersistenceEntryManager persistence;

        @Override
        public Collection<MetricRegistration> getRegisteredMetrics() {
            return registrations;
        }

        @Override
        public void add(List<MetricEntry> metricEntries, Date creationTime) {
            if (persistence != null) {
                super.add(metricEntries, creationTime);
                return;
            }
            if (failNextAdd) {
                failNextAdd = false;
                throw new IllegalStateException("Simulated write failure");
            }
            writes.add(new ArrayList<>(metricEntries));
        }

        @Override
        public String baseDn() {
            return "ou=statistic,o=metric";
        }

        @Override
        public MetricService getMetricServiceInstance() {
            return this;
        }

        @Override
        public boolean isMetricReporterEnabled() {
            return true;
        }

        @Override
        public String getString() {
            return ApplicationType.OX_AUTH.getValue();
        }

        @Override
        public String getNodeIdentifier() {
            return "test-node";
        }

        @Override
        public PersistenceEntryManager getEntryManager() {
            return persistence;
        }
    }

    @BeforeEach
    public void setUp() {
        metricService = new CapturingMetricService();
        registry = new MetricRegistry();
        reporter = PersistenceEntryReporter.forRegistry(registry, metricService).build();
    }

    @AfterEach
    public void tearDown() {
        reporter.close();
    }

    @SuppressWarnings("rawtypes")
    private void report() {
        reporter.report(new TreeMap<String, Gauge>(), registry.getCounters(), new TreeMap<String, Histogram>(),
                new TreeMap<String, Meter>(), registry.getTimers());
    }

    private Counter counter(MetricRegistration registration) {
        if (!metricService.registrations.contains(registration)) {
            metricService.registrations.add(registration);
        }
        return registry.counter(registration.getRegistryName());
    }

    private Timer timer(MetricRegistration registration) {
        if (!metricService.registrations.contains(registration)) {
            metricService.registrations.add(registration);
        }
        return registry.timer(registration.getRegistryName());
    }

    private CounterMetricData counterData(int writeIndex, MetricRegistration registration) {
        for (MetricEntry entry : metricService.writes.get(writeIndex)) {
            if ((entry instanceof CounterMetricEntry) && matches(entry, registration)) {
                return ((CounterMetricEntry) entry).getMetricData();
            }
        }
        throw new AssertionError("No counter row for " + registration + " in write " + writeIndex);
    }

    private TimerMetricData timerData(int writeIndex, MetricRegistration registration) {
        for (MetricEntry entry : metricService.writes.get(writeIndex)) {
            if ((entry instanceof TimerMetricEntry) && matches(entry, registration)) {
                return ((TimerMetricEntry) entry).getMetricData();
            }
        }
        throw new AssertionError("No timer row for " + registration + " in write " + writeIndex);
    }

    private boolean matches(MetricEntry entry, MetricRegistration registration) {
        return registration.getMetricType().getValue().equals(entry.getMetricType())
                && Objects.equals(registration.getMetricSubType(), entry.getMetricSubType());
    }

    @Test
    public void counterIntervalCountIsDeltaOfCumulativeCount() {
        Counter counter = counter(COUNTER_A);

        counter.inc(5);
        report();
        counter.inc(7);
        report();
        report();

        assertEquals(3, metricService.writes.size());
        assertEquals(5, counterData(0, COUNTER_A).getIntervalCount());
        assertEquals(7, counterData(1, COUNTER_A).getIntervalCount());
        assertEquals(0, counterData(2, COUNTER_A).getIntervalCount());

        assertEquals(5, counterData(0, COUNTER_A).getCount());
        assertEquals(12, counterData(1, COUNTER_A).getCount());
        assertEquals(12, counterData(2, COUNTER_A).getCount());
    }

    @Test
    public void secondInsertFailureRollsBackBatchAndRetryCountsEventsOnce() throws Exception {
        Counter first = counter(COUNTER_A);
        Counter second = counter(COUNTER_B);
        first.inc(5);
        second.inc(7);
        List<MetricEntry> stored = new ArrayList<>();
        boolean[] failSecond = { true };
        metricService.persistence = (PersistenceEntryManager) Proxy.newProxyInstance(PersistenceEntryManager.class.getClassLoader(),
                new Class<?>[] { PersistenceEntryManager.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "hasBranchesSupport":
                        return false;
                    case "isAtomicBatchSupported":
                        return true;
                    case "persistBatch":
                        int before = stored.size();
                        try {
                            for (Object entry : (List<?>) args[0]) {
                                if (failSecond[0] && stored.size() == before + 1) {
                                    failSecond[0] = false;
                                    throw new IllegalStateException("Second insert failed");
                                }
                                stored.add((MetricEntry) entry);
                            }
                        } catch (RuntimeException failure) {
                            stored.subList(before, stored.size()).clear();
                            throw failure;
                        }
                        return null;
                    default:
                        throw new AssertionError("Unexpected persistence call: " + method.getName());
                    }
                });

        assertThrows(IllegalStateException.class, this::report);
        assertTrue(stored.isEmpty(), "the first insert rolls back with the second");
        report();
        assertEquals(2, stored.size());
        List<MetricDataEntry> raw = new ArrayList<>();
        for (MetricEntry entry : stored) {
            MetricDataEntry row = new MetricDataEntry();
            row.setId(entry.getId());
            row.setApplicationType(entry.getApplicationType());
            row.setMetricType(entry.getMetricType());
            row.setMetricSubType(entry.getMetricSubType());
            row.setNodeIdentifier(entry.getNodeIdentifier());
            row.setEndDate(entry.getEndDate());
            row.setData(new ObjectMapper().writeValueAsString(((CounterMetricEntry) entry).getMetricData()));
            raw.add(row);
        }
        MetricAggregator aggregator = new MetricAggregator(metricService.baseDn(), "test-node", null);
        long total = aggregator.aggregateRaw(raw, PeriodMath.bucketContaining(MetricAggregationType.HOURLY,
                raw.get(0).getEndDate().toInstant()), true, new Date()).stream()
                .map(entry -> MetricAggregator.readAggregateData(entry.getData())).mapToLong(AggregateData::getCount).sum();
        assertEquals(12, total);
    }

    @Test
    public void failedWriteDoesNotLoseInterval() {
        Counter counter = counter(COUNTER_A);

        counter.inc(5);
        report();

        counter.inc(7);
        metricService.failNextAdd = true;
        assertThrows(IllegalStateException.class, this::report);

        report();

        assertEquals(2, metricService.writes.size());
        assertEquals(5, counterData(0, COUNTER_A).getIntervalCount());
        assertEquals(7, counterData(1, COUNTER_A).getIntervalCount());
        assertEquals(12, counterData(1, COUNTER_A).getCount());
    }

    @Test
    public void failedWriteKeepsStartDateOfFailedInterval() {
        Counter counter = counter(COUNTER_A);

        counter.inc(5);
        report();
        Date firstEnd = metricService.writes.get(0).get(0).getEndDate();

        counter.inc(7);
        metricService.failNextAdd = true;
        assertThrows(IllegalStateException.class, this::report);

        report();
        Date secondStart = metricService.writes.get(1).get(0).getStartDate();

        assertEquals(firstEnd.getTime() + 1, secondStart.getTime(),
                "The row after a failed write must start where the last written row ended");
    }

    @Test
    public void timerIntervalCountIsDeltaOfCumulativeCount() {
        Timer timer = timer(TIMER);

        timer.update(10, TimeUnit.MILLISECONDS);
        timer.update(20, TimeUnit.MILLISECONDS);
        timer.update(30, TimeUnit.MILLISECONDS);
        report();
        timer.update(40, TimeUnit.MILLISECONDS);
        timer.update(50, TimeUnit.MILLISECONDS);
        report();

        assertEquals(3, timerData(0, TIMER).getIntervalCount());
        assertEquals(2, timerData(1, TIMER).getIntervalCount());
        assertEquals(5, timerData(1, TIMER).getCount());
    }

    @Test
    public void counterRegisteredBetweenReportsStartsFromItsCount() {
        Counter counterA = counter(COUNTER_A);
        counterA.inc(5);
        report();

        Counter counterB = counter(COUNTER_B);
        counterB.inc(4);
        counterA.inc(1);
        report();

        assertEquals(1, counterData(1, COUNTER_A).getIntervalCount());
        assertEquals(4, counterData(1, COUNTER_B).getIntervalCount());
    }

    @Test
    public void counterAndTimerInOneReportAreTrackedSeparately() {
        Counter counter = counter(COUNTER_A);
        Timer timer = timer(TIMER);

        counter.inc(2);
        timer.update(1, TimeUnit.MILLISECONDS);
        report();
        counter.inc(3);
        report();

        assertEquals(2, counterData(0, COUNTER_A).getIntervalCount());
        assertEquals(1, timerData(0, TIMER).getIntervalCount());
        assertEquals(3, counterData(1, COUNTER_A).getIntervalCount());
        assertEquals(0, timerData(1, TIMER).getIntervalCount());
    }

    @Test
    public void lowerCountIsTreatedAsRestart() {
        assertEquals(5, PersistenceEntryReporter.computeIntervalCount(5, null));
        assertEquals(7, PersistenceEntryReporter.computeIntervalCount(12, 5L));
        assertEquals(0, PersistenceEntryReporter.computeIntervalCount(12, 12L));
        assertEquals(3, PersistenceEntryReporter.computeIntervalCount(3, 10L));
    }

    @Test
    public void lowerCountFromReporterIsWrittenAsFullCount() {
        Counter counter = counter(COUNTER_A);
        counter.inc(10);
        report();
        counter.dec(7);
        report();

        assertEquals(3, counterData(1, COUNTER_A).getIntervalCount());
    }

    @Test
    public void intervalCountOfZeroIsSerialized() throws Exception {
        // jans-orm serializes @JsonObject attributes with a default ObjectMapper
        ObjectMapper mapper = new ObjectMapper();

        CounterMetricData counterData = new CounterMetricData(12);
        counterData.setIntervalCount(0);
        String counterJson = mapper.writeValueAsString(counterData);
        assertTrue(mapper.readTree(counterJson).has("intervalCount"), counterJson);

        TimerMetricData timerData = new TimerMetricData();
        timerData.setCount(12);
        String timerJson = mapper.writeValueAsString(timerData);
        assertTrue(mapper.readTree(timerJson).has("intervalCount"), timerJson);
    }

    @Test
    public void legacyJsonWithoutIntervalCountDeserializes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        CounterMetricData counterData = mapper.readValue("{\"count\":5}", CounterMetricData.class);
        assertEquals(5, counterData.getCount());
        assertEquals(0, counterData.getIntervalCount());

        TimerMetricData timerData = mapper.readValue("{\"count\":9,\"meanRate\":0.5,\"oneMinuteRate\":0.1,\"fiveMinuteRate\":0.2,"
                + "\"fifteenMinuteRate\":0.3,\"rateUnit\":\"second\",\"min\":1.0,\"max\":9.0,\"mean\":4.0,\"stdDev\":1.0,"
                + "\"median\":4.0,\"value75thPercentile\":5.0,\"value95thPercentile\":8.0,\"value98thPercentile\":8.5,"
                + "\"value99thPercentile\":9.0,\"value999thPercentile\":9.0,\"durationUnit\":\"milliseconds\"}", TimerMetricData.class);
        assertEquals(9, timerData.getCount());
        assertEquals(0, timerData.getIntervalCount());
        assertEquals("milliseconds", timerData.getDurationUnit());
    }

}
