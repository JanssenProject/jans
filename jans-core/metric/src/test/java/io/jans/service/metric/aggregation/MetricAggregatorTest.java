/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.aggregation.AggregateData;
import io.jans.model.metric.aggregation.CounterAggregateData;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.model.metric.aggregation.TimerAggregateData;
import io.jans.model.metric.counter.CounterMetricData;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.model.metric.timer.TimerMetricData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricAggregatorTest {

    private static final String METRIC_BASE_DN = "ou=statistic,o=metric";
    private static final String COMPUTING_NODE = "computing-node";
    private static final String APP = "jans_auth";
    private static final String COUNTER_TYPE = "tkn_access_token_count";
    private static final String TIMER_TYPE = "user_authentication_rate";
    private static final Date CREATED = Date.from(Instant.parse("2026-11-24T12:00:00Z"));

    private static final PeriodBucket HOUR = PeriodMath.bucketContaining(MetricAggregationType.HOURLY, at("2026-11-23T10:00:00Z"));
    private static final PeriodBucket DAY = PeriodMath.bucketContaining(MetricAggregationType.DAILY, at("2026-11-23T10:00:00Z"));

    private final ObjectMapper defaultMapper = new ObjectMapper();
    private final MetricAggregator aggregator = new MetricAggregator(METRIC_BASE_DN, COMPUTING_NODE, null);

    private static Instant at(String value) {
        return Instant.parse(value);
    }

    private static MetricDataEntry raw(String host, String metricType, String subType, Instant end, String data) {
        String id = UUID.nameUUIDFromBytes((host + "|" + metricType + "|" + subType + "|" + end.toEpochMilli() + "|" + data)
                .getBytes(StandardCharsets.UTF_8)).toString();
        MetricDataEntry entry = new MetricDataEntry();
        entry.setId(id);
        entry.setDn("uniqueIdentifier=" + id + ",ou=2026-11,ou=jans_auth," + METRIC_BASE_DN);
        entry.setApplicationType(APP);
        entry.setMetricType(metricType);
        entry.setMetricSubType(subType);
        entry.setNodeIdentifier(host);
        entry.setStartDate(Date.from(end.minus(Duration.ofMinutes(5))));
        entry.setEndDate(Date.from(end));
        entry.setCreationDate(Date.from(end));
        entry.setData(data);
        return entry;
    }

    private static String legacyCounter(long count) {
        return "{\"count\":" + count + "}";
    }

    private String counter(long count, long intervalCount) throws Exception {
        CounterMetricData data = new CounterMetricData(count);
        data.setIntervalCount(intervalCount);
        return defaultMapper.writeValueAsString(data);
    }

    private String timer(long count, Long intervalCount, double min, double max, double mean, double median, double p75, double p95,
            double p98, double p99, double p999) throws Exception {
        TimerMetricData data = new TimerMetricData(count, 1.5, 0.1, 0.2, 0.3, "second", min, max, mean, 2.5, median, p75, p95, p98,
                p99, p999, "milliseconds");
        if (intervalCount == null) {
            ObjectNode node = defaultMapper.valueToTree(data);
            node.remove("intervalCount");
            return defaultMapper.writeValueAsString(node);
        }
        data.setIntervalCount(intervalCount);
        return defaultMapper.writeValueAsString(data);
    }

    private static AggregateData data(MetricAggregationEntry entry) {
        AggregateData data = MetricAggregator.readAggregateData(entry.getData());
        assertTrue(data != null, "unreadable jansData: " + entry.getData());
        return data;
    }

    private static MetricAggregationEntry only(List<MetricAggregationEntry> entries) {
        assertEquals(1, entries.size(), "entries: " + entries);
        return entries.get(0);
    }

    private static Instant inHour(int minute) {
        return HOUR.getStart().plus(Duration.ofMinutes(minute)).minusMillis(1);
    }

    @Test
    void restartInsideBucket() {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, HOUR.getStart().minusSeconds(300), legacyCounter(10)),
                raw("h1", COUNTER_TYPE, null, inHour(10), legacyCounter(15)),
                raw("h1", COUNTER_TYPE, null, inHour(20), legacyCounter(3)),
                raw("h1", COUNTER_TYPE, null, inHour(30), legacyCounter(8)));

        AggregateData data = data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));

        assertEquals(13, data.getCount());
        assertEquals(3, data.getSourceRows());
        assertTrue(data.isLegacyDelta());
    }

    @Test
    void legacyBaselineCountsZero() {
        List<MetricDataEntry> rows = Collections.singletonList(raw("h1", COUNTER_TYPE, null, inHour(10), legacyCounter(50000)));

        AggregateData data = data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));

        assertEquals(0, data.getCount());
        assertEquals(1, data.getSourceRows());
        assertTrue(data.isLegacyDelta());

        List<MetricDataEntry> twoRows = Arrays.asList(rows.get(0), raw("h1", COUNTER_TYPE, null, inHour(15), legacyCounter(50012)));
        assertEquals(12, data(only(aggregator.aggregateRaw(twoRows, HOUR, true, CREATED))).getCount());
    }

    @Test
    void mixedLegacyAndIntervalCountRows() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, HOUR.getStart().minusSeconds(300), legacyCounter(100)),
                raw("h1", COUNTER_TYPE, null, inHour(5), legacyCounter(110)),
                raw("h1", COUNTER_TYPE, null, inHour(10), counter(115, 5)),
                raw("h1", COUNTER_TYPE, null, inHour(15), counter(2, 2)),
                raw("h1", COUNTER_TYPE, null, inHour(20), legacyCounter(9)),
                raw("h2", COUNTER_TYPE, null, inHour(10), counter(40, 0)),
                raw("h2", COUNTER_TYPE, null, inHour(15), counter(47, 7)));

        AggregateData data = data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));

        assertEquals(10 + 5 + 2 + 7 + 0 + 7, data.getCount());
        assertEquals(6, data.getSourceRows());
        assertEquals(2, data.getNodes());
        assertTrue(data.isLegacyDelta());
    }

    @Test
    void intervalCountRowsOnlyAreNotLegacy() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, inHour(10), counter(500, 0)),
                raw("h1", COUNTER_TYPE, null, inHour(15), counter(504, 4)));

        AggregateData data = data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));

        assertEquals(4, data.getCount());
        assertFalse(data.isLegacyDelta());
    }

    @Test
    void hostsAreSummedAndSubtypesStaySeparate() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, inHour(10), counter(3, 3)),
                raw("h2", COUNTER_TYPE, null, inHour(10), counter(4, 4)),
                raw("h2", COUNTER_TYPE, "", inHour(15), counter(5, 1)),
                raw("h1", COUNTER_TYPE, "client_a", inHour(10), counter(20, 20)),
                raw("h2", COUNTER_TYPE, "client_a", inHour(10), counter(30, 30)),
                raw("h1", COUNTER_TYPE, "client_b", inHour(10), counter(7, 7)));

        List<MetricAggregationEntry> entries = aggregator.aggregateRaw(rows, HOUR, true, CREATED);

        assertEquals(3, entries.size());
        MetricAggregationEntry noSubType = entries.get(0);
        assertNull(noSubType.getMetricSubType());
        assertEquals(8, data(noSubType).getCount());
        assertEquals(2, data(noSubType).getNodes());

        assertEquals("client_a", entries.get(1).getMetricSubType());
        assertEquals(50, data(entries.get(1)).getCount());
        assertEquals("client_b", entries.get(2).getMetricSubType());
        assertEquals(7, data(entries.get(2)).getCount());

        assertNotEquals(entries.get(0).getId(), entries.get(1).getId());
        assertNotEquals(entries.get(1).getId(), entries.get(2).getId());
    }

    @Test
    void rowAtBucketEndGoesToNextBucket() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, HOUR.getStart(), counter(1, 1)),
                raw("h1", COUNTER_TYPE, null, HOUR.getEnd().minusMillis(1), counter(3, 2)),
                raw("h1", COUNTER_TYPE, null, HOUR.getEnd(), counter(7, 4)));

        assertEquals(3, data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED))).getCount());
        assertEquals(4, data(only(aggregator.aggregateRaw(rows, PeriodMath.next(HOUR), true, CREATED))).getCount());
    }

    @Test
    void emptyBucketGivesNoRows() throws Exception {
        assertTrue(aggregator.aggregateRaw(Collections.<MetricDataEntry>emptyList(), HOUR, true, CREATED).isEmpty());
        assertTrue(aggregator.aggregateRaw(null, HOUR, true, CREATED).isEmpty());

        List<MetricDataEntry> lookbackOnly = Collections.singletonList(
                raw("h1", COUNTER_TYPE, null, HOUR.getStart().minusSeconds(1), counter(3, 3)));
        assertTrue(aggregator.aggregateRaw(lookbackOnly, HOUR, true, CREATED).isEmpty());
    }

    @Test
    void unreadableRowsAreSkipped() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, inHour(10), "not json"),
                raw("h1", COUNTER_TYPE, null, inHour(11), "{\"other\":1}"),
                raw("h1", COUNTER_TYPE, null, inHour(12), null),
                raw("h1", COUNTER_TYPE, null, inHour(13), counter(2, 2)));

        AggregateData data = data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));
        assertEquals(2, data.getCount());
        assertEquals(1, data.getSourceRows());
    }

    @Test
    void detectsRawKindFromJsonShape() throws Exception {
        assertEquals(TimerAggregateData.KIND, MetricAggregator.detectRawKind(timer(1, null, 1, 1, 1, 1, 1, 1, 1, 1, 1)));
        assertEquals(CounterAggregateData.KIND, MetricAggregator.detectRawKind(counter(1, 1)));
        assertEquals(CounterAggregateData.KIND, MetricAggregator.detectRawKind(legacyCounter(1)));
        assertNull(MetricAggregator.detectRawKind("{\"value\":1}"));
        assertNull(MetricAggregator.detectRawKind("[1]"));
        assertNull(MetricAggregator.detectRawKind("broken"));
        assertNull(MetricAggregator.detectRawKind(null));
    }

    @Test
    void counterJsonIsExact() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", COUNTER_TYPE, null, HOUR.getStart().minusSeconds(300), legacyCounter(10)),
                raw("h1", COUNTER_TYPE, null, inHour(10), legacyCounter(15)),
                raw("h2", COUNTER_TYPE, null, inHour(10), counter(8, 8)));

        MetricAggregationEntry entry = only(aggregator.aggregateRaw(rows, HOUR, true, CREATED));

        assertEquals("{\"kind\":\"counter\",\"count\":13,\"sourceRows\":2,\"nodes\":2,\"complete\":true,\"legacyDelta\":true}",
                entry.getData());
    }

    @Test
    void timerMergeIsWeightedAndJsonIsExact() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", TIMER_TYPE, null, inHour(10), timer(110, 10L, 1, 20, 5, 4, 6, 10, 12, 15, 19)),
                raw("h2", TIMER_TYPE, null, inHour(10), timer(30, 30L, 2, 40, 9, 8, 10, 20, 25, 30, 38)));

        MetricAggregationEntry entry = only(aggregator.aggregateRaw(rows, HOUR, false, CREATED));
        TimerAggregateData data = (TimerAggregateData) data(entry);

        assertEquals(40, data.getCount());
        assertEquals(1.0, data.getMin());
        assertEquals(40.0, data.getMax());
        assertEquals(8.0, data.getMean());
        assertEquals(7.0, data.getMedian());
        assertEquals(17.5, data.getValue95thPercentile());
        assertEquals(40d / 3600, data.getRate());
        assertTrue(data.isApproximate());
        assertFalse(data.isComplete());

        String expected = "{\"kind\":\"timer\",\"count\":40,\"rate\":" + (40d / 3600) + ",\"rateUnit\":\"second\",\"min\":1.0,"
                + "\"max\":40.0,\"mean\":8.0,\"median\":7.0,\"value75thPercentile\":9.0,\"value95thPercentile\":17.5,"
                + "\"value98thPercentile\":21.75,\"value99thPercentile\":26.25,\"value999thPercentile\":33.25,"
                + "\"durationUnit\":\"milliseconds\",\"approximate\":true,\"sourceRows\":2,\"nodes\":2,\"complete\":false,"
                + "\"legacyDelta\":false}";
        assertEquals(expected, entry.getData());
    }

    @Test
    void rawTimerStatisticsAreNormalizedToMilliseconds() throws Exception {
        String milliseconds = timer(1, 1L, 1, 9, 3, 4, 5, 6, 7, 8, 9);
        ObjectNode seconds = (ObjectNode) defaultMapper.readTree(milliseconds);
        seconds.put("durationUnit", "seconds");
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", TIMER_TYPE, null, inHour(10), seconds.toString()),
                raw("h2", TIMER_TYPE, null, inHour(10), milliseconds));

        assertMixedUnitTimerStatistics((TimerAggregateData) data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED))));
    }

    @Test
    void invalidTimerUnitDoesNotReplaceLegacyBaseline() throws Exception {
        String first = timer(10, null, 1, 2, 1, 1, 1, 1, 1, 1, 1);
        String invalid = timer(100, null, 1, 2, 1, 1, 1, 1, 1, 1, 1).replace("milliseconds", "invalid");
        String last = timer(15, null, 1, 2, 1, 1, 1, 1, 1, 1, 1);
        List<MetricDataEntry> rows = Arrays.asList(raw("h1", TIMER_TYPE, null, inHour(5), first),
                raw("h1", TIMER_TYPE, null, inHour(10), invalid), raw("h1", TIMER_TYPE, null, inHour(15), last));
        AggregateData result = data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));
        assertEquals(5, result.getCount());
        assertEquals(2, result.getSourceRows());
    }

    @Test
    void childTimerStatisticsAreNormalizedToMilliseconds() throws Exception {
        String milliseconds = timer(1, 1L, 1, 9, 3, 4, 5, 6, 7, 8, 9);
        MetricAggregationEntry first = only(aggregator.aggregateRaw(Collections.singletonList(
                raw("h1", TIMER_TYPE, null, inHour(10), milliseconds)), HOUR, true, CREATED));
        ObjectNode seconds = (ObjectNode) defaultMapper.readTree(first.getData());
        seconds.put("durationUnit", "seconds");
        first.setData(seconds.toString());
        PeriodBucket next = PeriodMath.next(HOUR);
        MetricAggregationEntry second = only(aggregator.aggregateRaw(Collections.singletonList(
                raw("h1", TIMER_TYPE, null, next.getStart().plusSeconds(60), milliseconds)), next, true, CREATED));

        assertMixedUnitTimerStatistics((TimerAggregateData) data(only(
                aggregator.rollup(Arrays.asList(first, second), DAY, DAY.getEnd(), CREATED))));
    }

    private static void assertMixedUnitTimerStatistics(TimerAggregateData data) {
        assertEquals("milliseconds", data.getDurationUnit());
        assertEquals(2, data.getCount());
        assertEquals(1.0, data.getMin());
        assertEquals(9000.0, data.getMax());
        assertEquals(1501.5, data.getMean());
        assertEquals(2002.0, data.getMedian());
        assertEquals(2502.5, data.getValue75thPercentile());
        assertEquals(3003.0, data.getValue95thPercentile());
        assertEquals(3503.5, data.getValue98thPercentile());
        assertEquals(4004.0, data.getValue99thPercentile());
        assertEquals(4504.5, data.getValue999thPercentile());
    }

    @Test
    void timerRowsWithoutEventsDoNotAffectStatistics() throws Exception {
        List<MetricDataEntry> rows = Arrays.asList(
                raw("h1", TIMER_TYPE, null, inHour(10), timer(10, 10L, 2, 20, 5, 5, 5, 5, 5, 5, 5)),
                raw("h2", TIMER_TYPE, null, inHour(10), timer(99, 0L, 0.01, 9000, 700, 700, 700, 700, 700, 700, 700)),
                raw("h3", TIMER_TYPE, null, inHour(10), timer(5000, null, 0.02, 8000, 600, 600, 600, 600, 600, 600, 600)));

        TimerAggregateData data = (TimerAggregateData) data(only(aggregator.aggregateRaw(rows, HOUR, true, CREATED)));

        assertEquals(10, data.getCount());
        assertEquals(2.0, data.getMin());
        assertEquals(20.0, data.getMax());
        assertEquals(5.0, data.getMean());
        assertEquals(3, data.getSourceRows());
        assertEquals(3, data.getNodes());
        assertTrue(data.isLegacyDelta());
    }

    @Test
    void timerWithoutEventsHasNullStatistics() throws Exception {
        List<MetricDataEntry> rows = Collections.singletonList(raw("h1", TIMER_TYPE, null, inHour(10), timer(7, 0L, 1, 2, 1, 1, 1, 1, 1, 1, 1)));

        MetricAggregationEntry entry = only(aggregator.aggregateRaw(rows, HOUR, true, CREATED));
        TimerAggregateData data = (TimerAggregateData) data(entry);

        assertEquals(0, data.getCount());
        assertEquals(0.0, data.getRate());
        assertNull(data.getMin());
        assertNull(data.getMean());
        assertFalse(entry.getData().contains("NaN"));
    }

    @Test
    void entryAttributes() throws Exception {
        MetricAggregationEntry entry = only(aggregator.aggregateRaw(
                Collections.singletonList(raw("h1", COUNTER_TYPE, "sub", inHour(10), counter(1, 1))), HOUR, true, CREATED));

        String expectedId = UUID.nameUUIDFromBytes(("6:HOURLY9:jans_auth22:tkn_access_token_count3:sub13:" + HOUR.getStart().toEpochMilli())
                .getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(expectedId, entry.getId());
        assertEquals("uniqueIdentifier=" + expectedId + ",ou=aggregation,ou=statistic,o=metric", entry.getDn());
        assertTrue(entry.getDn().length() <= MetricAggregator.MAX_DN_LENGTH);
        assertEquals(MetricAggregationType.HOURLY, entry.getAggregationType());
        assertEquals(HOUR.getStartDate(), entry.getStartDate());
        assertEquals(Date.from(HOUR.getEnd().minusMillis(1)), entry.getEndDate());
        assertEquals(Date.from(Instant.parse("2026-11-23T10:59:59.999Z")), entry.getEndDate());
        assertEquals(APP, entry.getApplicationType());
        assertEquals(COUNTER_TYPE, entry.getMetricType());
        assertEquals("sub", entry.getMetricSubType());
        assertEquals(COMPUTING_NODE, entry.getNodeIdentifier());
        assertEquals(CREATED, entry.getCreationDate());
        assertEquals(Date.from(CREATED.toInstant().plus(Duration.ofDays(90))), entry.getExpirationDate());
        assertEquals(90 * 86400, entry.getTtl());
        assertTrue(entry.isDeletable());
    }

    @Test
    void idWithoutSubTypeUsesEmptyString() {
        String expected = UUID.nameUUIDFromBytes(("5:DAILY3:app4:type0:13:" + DAY.getStart().toEpochMilli()).getBytes(StandardCharsets.UTF_8))
                .toString();
        assertEquals(expected, MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "type", null, DAY.getStart()));
        assertEquals(expected, MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "type", "", DAY.getStart()));
        assertNotEquals(expected, MetricAggregator.aggregateId(MetricAggregationType.HOURLY, "app", "type", null, DAY.getStart()));
    }

    @Test
    void idSeparatesFieldsContainingDelimiters() {
        String id = MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "a|b", "c", DAY.getStart());
        assertNotEquals(id, MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "a", "b|c", DAY.getStart()));
        assertEquals(id, MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "a|b", "c", DAY.getStart()));
        assertNotEquals(
                MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app|a", "b", "c", DAY.getStart()),
                MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "a|b", "c", DAY.getStart()));
        assertNotEquals(
                MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "a:1", "b", DAY.getStart()),
                MetricAggregator.aggregateId(MetricAggregationType.DAILY, "app", "a", "1:b", DAY.getStart()));
    }

    @Test
    void retentionByLevelFromConfiguration() {
        MetricAggregationConfiguration configuration = new MetricAggregationConfiguration();
        configuration.setKeepDailyDays(10);
        MetricAggregator custom = new MetricAggregator(METRIC_BASE_DN, COMPUTING_NODE, configuration);

        assertEquals(90, custom.keepDays(MetricAggregationType.HOURLY));
        assertEquals(10, custom.keepDays(MetricAggregationType.DAILY));
        assertEquals(1825, custom.keepDays(MetricAggregationType.WEEKLY));
        assertEquals(3650, custom.keepDays(MetricAggregationType.MONTHLY));
    }

    @Test
    void dnLongerThanLimitIsRejected() {
        assertEquals("ou=aggregation,ou=statistic,o=metric", aggregator.getAggregationBaseDn());

        char[] longOu = new char[80];
        Arrays.fill(longOu, 'x');
        MetricAggregator longBase = new MetricAggregator("ou=" + new String(longOu) + ",o=metric", COMPUTING_NODE, null);
        assertThrows(IllegalArgumentException.class, () -> longBase.buildDn(UUID.randomUUID().toString()));
    }

    private List<MetricDataEntry> randomRows(Random random, PeriodBucket hour) throws Exception {
        List<MetricDataEntry> rows = new ArrayList<>();
        String[] hosts = { "h1", "h2", "h3" };
        String[] subTypes = { null, "client_a", "client_b" };
        for (String host : hosts) {
            for (String subType : subTypes) {
                long count = random.nextInt(1000);
                long timerCount = random.nextInt(1000);
                for (int i = -2; i < 12; i++) {
                    Instant end = hour.getStart().plus(Duration.ofMinutes(5L * i + 5)).minusMillis(1);
                    long increment = random.nextInt(50);
                    count += increment;
                    timerCount += increment;
                    boolean legacy = random.nextBoolean();
                    rows.add(raw(host, COUNTER_TYPE, subType, end, legacy ? legacyCounter(count) : counter(count, increment)));
                    rows.add(raw(host, TIMER_TYPE, subType, end, timer(timerCount, legacy ? null : increment,
                            random.nextDouble(), 100 * random.nextDouble(), 10 * random.nextDouble(), 10 * random.nextDouble(),
                            20 * random.nextDouble(), 30 * random.nextDouble(), 40 * random.nextDouble(),
                            50 * random.nextDouble(), 60 * random.nextDouble())));
                }
            }
        }
        return rows;
    }

    @Test
    void sameInputInAnyOrderGivesIdenticalOutput() throws Exception {
        Random random = new Random(20261124L);
        List<MetricDataEntry> rows = randomRows(random, HOUR);

        List<String> expected = describe(aggregator.aggregateRaw(rows, HOUR, true, CREATED));
        assertEquals(6, expected.size());

        for (int i = 0; i < 10; i++) {
            List<MetricDataEntry> shuffled = new ArrayList<>(rows);
            Collections.shuffle(shuffled, random);
            assertEquals(expected, describe(aggregator.aggregateRaw(shuffled, HOUR, true, CREATED)));

            MetricAggregator otherNode = new MetricAggregator(METRIC_BASE_DN, "other-node", null);
            assertEquals(expected, describe(otherNode.aggregateRaw(shuffled, HOUR, true, new Date())));
        }

        List<MetricAggregationEntry> children = new ArrayList<>();
        for (PeriodBucket hour : PeriodMath.buckets(MetricAggregationType.HOURLY, DAY.getStart(), DAY.getEnd())) {
            children.addAll(aggregator.aggregateRaw(randomRows(random, hour), hour, true, CREATED));
        }
        List<String> expectedDaily = describe(aggregator.rollup(children, DAY, DAY.getEnd(), CREATED));
        assertEquals(6, expectedDaily.size());
        for (int i = 0; i < 10; i++) {
            List<MetricAggregationEntry> shuffled = new ArrayList<>(children);
            Collections.shuffle(shuffled, random);
            assertEquals(expectedDaily, describe(aggregator.rollup(shuffled, DAY, DAY.getEnd(), CREATED)));
        }
    }

    private static List<String> describe(List<MetricAggregationEntry> entries) {
        List<String> result = new ArrayList<>();
        for (MetricAggregationEntry entry : entries) {
            result.add(entry.getId() + " " + entry.getDn() + " " + entry.getData());
        }
        return result;
    }

    private List<MetricAggregationEntry> hourlyCounters(int skippedHour, int incompleteHour) throws Exception {
        List<MetricAggregationEntry> children = new ArrayList<>();
        List<PeriodBucket> hours = PeriodMath.buckets(MetricAggregationType.HOURLY, DAY.getStart(), DAY.getEnd());
        for (int h = 0; h < hours.size(); h++) {
            if (h == skippedHour) {
                continue;
            }
            PeriodBucket hour = hours.get(h);
            List<MetricDataEntry> rows = Arrays.asList(
                    raw("h1", COUNTER_TYPE, null, hour.getStart().plusSeconds(600), counter(1000, h + 1)),
                    raw("h2", COUNTER_TYPE, null, hour.getStart().plusSeconds(600), counter(1000, 1)));
            children.addAll(aggregator.aggregateRaw(rows, hour, h != incompleteHour, CREATED));
        }
        return children;
    }

    @Test
    void endDateIsPeriodEndMinusOneMillisecond() throws Exception {
        List<MetricAggregationEntry> hourly = hourlyCounters(-1, -1);
        assertEquals(24, hourly.size());
        for (MetricAggregationEntry entry : hourly) {
            assertEquals(entry.getStartDate().toInstant().plusSeconds(3600).minusMillis(1), entry.getEndDate().toInstant());
            assertTrue(DAY.contains(entry.getEndDate().toInstant()));
        }

        MetricAggregationEntry daily = only(aggregator.rollup(hourly, DAY, DAY.getEnd(), CREATED));
        assertEquals(Instant.parse("2026-11-23T00:00:00Z"), daily.getStartDate().toInstant());
        assertEquals(Instant.parse("2026-11-23T23:59:59.999Z"), daily.getEndDate().toInstant());
        assertTrue(data(daily).isComplete());
    }

    @Test
    void dailyRollupWithEmptyHourIsCompleteWhenWatermarkPassedDayEnd() throws Exception {
        List<MetricAggregationEntry> children = hourlyCounters(5, -1);
        assertEquals(23, children.size());

        MetricAggregationEntry daily = only(aggregator.rollup(children, DAY, DAY.getEnd().plusSeconds(3600), CREATED));
        AggregateData data = data(daily);

        int expected = 0;
        for (int h = 0; h < 24; h++) {
            expected += h == 5 ? 0 : h + 2;
        }
        assertEquals(expected, data.getCount());
        assertEquals(46, data.getSourceRows());
        assertEquals(2, data.getNodes());
        assertTrue(data.isComplete());
        assertFalse(data.isLegacyDelta());
        assertEquals(MetricAggregationType.DAILY, daily.getAggregationType());
        assertEquals(DAY.getStartDate(), daily.getStartDate());
        assertEquals(Date.from(DAY.getEnd().minusMillis(1)), daily.getEndDate());
        assertEquals(Date.from(CREATED.toInstant().plus(Duration.ofDays(730))), daily.getExpirationDate());

        assertTrue(data(only(aggregator.rollup(children, DAY, DAY.getEnd(), CREATED))).isComplete());
    }

    @Test
    void dailyRollupIsIncompleteWhenWatermarkInsideDay() throws Exception {
        List<MetricAggregationEntry> children = hourlyCounters(5, -1);

        assertFalse(data(only(aggregator.rollup(children, DAY, DAY.getEnd().minusSeconds(3600), CREATED))).isComplete());
        assertFalse(data(only(aggregator.rollup(children, DAY, null, CREATED))).isComplete());
    }

    @Test
    void unreadableChildPreventsOnlyItsKeyFromBeingComplete() throws Exception {
        List<MetricAggregationEntry> children = hourlyCounters(-1, -1);
        children.get(0).setData("{broken");
        MetricAggregationEntry otherKey = only(aggregator.aggregateRaw(Collections.singletonList(
                raw("h1", "other", null, inHour(10), counter(2, 2))), HOUR, true, CREATED));
        children.add(otherKey);
        List<MetricAggregationEntry> daily = aggregator.rollup(children, DAY, DAY.getEnd(), CREATED);
        assertEquals(2, daily.size());
        for (MetricAggregationEntry entry : daily) {
            assertEquals("other".equals(entry.getMetricType()), data(entry).isComplete());
        }
    }

    @Test
    void mixedKindsLeaveKeyIncomplete() throws Exception {
        List<MetricAggregationEntry> children = hourlyCounters(-1, -1);
        MetricAggregationEntry timerChild = only(aggregator.aggregateRaw(Collections.singletonList(raw("h1", COUNTER_TYPE,
                null, inHour(10), timer(1, 1L, 1, 1, 1, 1, 1, 1, 1, 1, 1))), HOUR, true, CREATED));
        children.set(10, timerChild);
        assertFalse(data(only(aggregator.rollup(children, DAY, DAY.getEnd(), CREATED))).isComplete());
    }

    @Test
    void retentionUsesUtcDaysAcrossDaylightSavingChange() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            Date creation = Date.from(at("2026-03-07T12:00:00Z"));
            MetricAggregationEntry entry = only(aggregator.aggregateRaw(Collections.singletonList(
                    raw("h1", COUNTER_TYPE, null, inHour(10), counter(2, 2))), HOUR, true, creation));
            assertEquals(creation.toInstant().plus(Duration.ofDays(90)), entry.getExpirationDate().toInstant());
            assertEquals(90 * 86400, entry.getTtl());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void dailyRollupIsIncompleteWhenAChildIsIncomplete() throws Exception {
        List<MetricAggregationEntry> children = hourlyCounters(-1, 23);

        assertFalse(data(only(aggregator.rollup(children, DAY, DAY.getEnd().plusSeconds(3600), CREATED))).isComplete());
    }

    @Test
    void rollupWithoutChildrenGivesNoRows() {
        assertTrue(aggregator.rollup(Collections.<MetricAggregationEntry>emptyList(), DAY, DAY.getEnd(), CREATED).isEmpty());
    }

    @Test
    void rollupRejectsWrongLevelOrPeriod() throws Exception {
        List<MetricAggregationEntry> hourly = hourlyCounters(-1, -1);
        PeriodBucket week = PeriodMath.bucketContaining(MetricAggregationType.WEEKLY, DAY.getStart());

        assertThrows(IllegalArgumentException.class, () -> aggregator.rollup(hourly, week, week.getEnd(), CREATED));
        assertThrows(IllegalArgumentException.class, () -> aggregator.rollup(hourly, PeriodMath.next(DAY), DAY.getEnd(), CREATED));
        assertThrows(IllegalArgumentException.class, () -> aggregator.rollup(hourly, HOUR, HOUR.getEnd(), CREATED));
    }

    @Test
    void timerRollupCombinesChildren() throws Exception {
        List<MetricAggregationEntry> children = new ArrayList<>();
        PeriodBucket first = PeriodMath.bucketContaining(MetricAggregationType.HOURLY, DAY.getStart());
        PeriodBucket second = PeriodMath.next(first);
        PeriodBucket third = PeriodMath.next(second);
        children.addAll(aggregator.aggregateRaw(Collections.singletonList(
                raw("h1", TIMER_TYPE, null, first.getStart().plusSeconds(60), timer(10, 10L, 1, 10, 4, 4, 4, 4, 4, 4, 4))), first, true, CREATED));
        children.addAll(aggregator.aggregateRaw(Collections.singletonList(
                raw("h2", TIMER_TYPE, null, second.getStart().plusSeconds(60), timer(30, 30L, 3, 50, 8, 8, 8, 8, 8, 8, 8))), second, true, CREATED));
        children.addAll(aggregator.aggregateRaw(Collections.singletonList(
                raw("h3", TIMER_TYPE, null, third.getStart().plusSeconds(60), timer(30, 0L, 0.5, 90, 70, 70, 70, 70, 70, 70, 70))), third, true, CREATED));

        TimerAggregateData data = (TimerAggregateData) data(only(aggregator.rollup(children, DAY, DAY.getEnd(), CREATED)));

        assertEquals(40, data.getCount());
        assertEquals(1.0, data.getMin());
        assertEquals(50.0, data.getMax());
        assertEquals(7.0, data.getMean());
        assertEquals(40d / 86400, data.getRate());
        assertEquals("milliseconds", data.getDurationUnit());
        assertEquals(3, data.getSourceRows());
        assertEquals(1, data.getNodes());
        assertTrue(data.isApproximate());
        assertTrue(data.isComplete());
    }

    @Test
    void weeklyAndMonthlyRollupFromDaily() throws Exception {
        PeriodBucket week = PeriodMath.bucketContaining(MetricAggregationType.WEEKLY, at("2026-11-23T00:00:00Z"));
        List<MetricAggregationEntry> daily = new ArrayList<>();
        for (PeriodBucket day : PeriodMath.buckets(MetricAggregationType.DAILY, week.getStart(), week.getEnd())) {
            List<MetricAggregationEntry> hourly = new ArrayList<>();
            for (PeriodBucket hour : PeriodMath.buckets(MetricAggregationType.HOURLY, day.getStart(), day.getEnd())) {
                hourly.addAll(aggregator.aggregateRaw(Collections.singletonList(
                        raw("h1", COUNTER_TYPE, null, hour.getStart().plusSeconds(60), legacyCounter(5))), hour, true, CREATED));
            }
            daily.addAll(aggregator.rollup(hourly, day, day.getEnd(), CREATED));
        }
        assertEquals(7, daily.size());

        AggregateData weekly = data(only(aggregator.rollup(daily, week, week.getEnd(), CREATED)));
        assertEquals(0, weekly.getCount());
        assertEquals(7 * 24, weekly.getSourceRows());
        assertTrue(weekly.isLegacyDelta());
        assertTrue(weekly.isComplete());

        PeriodBucket month = PeriodMath.bucketContaining(MetricAggregationType.MONTHLY, week.getStart());
        AggregateData monthly = data(only(aggregator.rollup(daily, month, week.getEnd(), CREATED)));
        assertFalse(monthly.isComplete());
        assertEquals(7 * 24, monthly.getSourceRows());
        assertTrue(data(only(aggregator.rollup(daily, month, month.getEnd(), CREATED))).isComplete());
    }

    @Test
    void monthlyRollupIsIncompleteWhenRetentionCutsThroughFirstDay() throws Exception {
        MetricAggregationConfiguration configuration = new MetricAggregationConfiguration();
        configuration.setKeepDailyDays(30);
        MetricAggregator shortRetention = new MetricAggregator(METRIC_BASE_DN, COMPUTING_NODE, configuration);
        List<MetricAggregationEntry> daily = aggregator.rollup(hourlyCounters(-1, -1), DAY, DAY.getEnd(), CREATED);
        PeriodBucket month = PeriodMath.bucketContaining(MetricAggregationType.MONTHLY, DAY.getStart());
        Instant boundary = month.getStart().plus(Duration.ofDays(30));

        assertTrue(data(only(shortRetention.rollup(daily, month, month.getEnd(), Date.from(boundary)))).isComplete());
        assertFalse(data(only(shortRetention.rollup(daily, month, month.getEnd(), Date.from(boundary.plusMillis(1)))))
                .isComplete());
        assertFalse(data(only(shortRetention.rollup(daily, month, month.getEnd(), Date.from(boundary.plus(Duration.ofDays(1))))))
                .isComplete());
    }

    @Test
    void sumOfDeltasEqualsTrueIncrementsForRandomSeriesWithRestarts() throws Exception {
        Random random = new Random(424242L);
        List<PeriodBucket> hours = PeriodMath.buckets(MetricAggregationType.HOURLY, DAY.getStart(), DAY.getEnd());

        for (int trial = 0; trial < 50; trial++) {
            List<MetricDataEntry> rows = new ArrayList<>();
            long trueTotal = 0;
            int hostCount = 1 + random.nextInt(3);
            for (int h = 0; h < hostCount; h++) {
                String host = "host-" + h;
                long count = random.nextInt(100000);
                rows.add(raw(host, COUNTER_TYPE, null, DAY.getStart().minusSeconds(1 + random.nextInt(3600)), legacyCounter(count)));

                Instant end = DAY.getStart();
                while (true) {
                    end = end.plusSeconds(1 + random.nextInt(900));
                    if (!end.isBefore(DAY.getEnd())) {
                        break;
                    }
                    long increment;
                    if (count > 0 && random.nextInt(10) == 0) {
                        count = random.nextInt((int) Math.min(count, Integer.MAX_VALUE));
                        increment = count;
                    } else {
                        increment = random.nextInt(25);
                        count += increment;
                    }
                    trueTotal += increment;
                    boolean legacy = random.nextInt(3) != 0;
                    rows.add(raw(host, COUNTER_TYPE, null, end, legacy ? legacyCounter(count) : counter(count, increment)));
                }
            }
            Collections.shuffle(rows, random);

            long aggregated = 0;
            for (PeriodBucket hour : hours) {
                for (MetricAggregationEntry entry : aggregator.aggregateRaw(rows, hour, true, CREATED)) {
                    aggregated += data(entry).getCount();
                }
            }
            assertEquals(trueTotal, aggregated, "trial " + trial);
        }
    }

}
