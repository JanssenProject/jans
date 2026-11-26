/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.aggregation.AggregateData;
import io.jans.model.metric.aggregation.CounterAggregateData;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.model.metric.aggregation.TimerAggregateData;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.model.metric.ldap.MetricDataEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Pure, deterministic aggregation of metric rows into {@code jansMetricAggregation} entries. It does no
 * I/O: the caller reads the source rows and writes the returned entries.
 *
 * <p>Rules:</p>
 * <ul>
 * <li>Aggregates are cluster-wide per (application type, metric type, metric subtype). A missing or
 * empty subtype is one key; each subtype value is another key.</li>
 * <li>A raw row belongs to the period that contains its {@code jansEndDate}.</li>
 * <li>The count of a raw row is its {@code intervalCount} when the row has one. Otherwise it is derived
 * from the cumulative {@code count} of the series (node, application type, metric type, subtype) ordered
 * by {@code jansEndDate}: the difference to the previous row, or the count itself when it is lower than
 * the previous count (process restart). The first row of a series without a previous row is a baseline
 * with count 0. Rows before the period only provide previous counts.</li>
 * <li>Inputs are sorted by a fixed key before any floating-point sum, so the same input in any order
 * gives byte-identical {@code jansData} and the same IDs.</li>
 * <li>Timer duration statistics from raw rows and child aggregates are normalized to milliseconds
 * before they are combined; output {@code durationUnit} is always {@code milliseconds}.</li>
 * <li>Periods without rows produce no entry. A period with rows whose counts are all 0 produces an entry
 * with count 0.</li>
 * <li>An entry has {@code jansStartDate} = period start and {@code jansEndDate} = period end minus 1 ms,
 * the same convention as raw {@code jansMetric} rows, so a containment filter
 * ({@code jansStartDate >= start AND jansEndDate <= end}) with an inclusive end such as
 * {@code 23:59:59} returns the period. Select rollup children by {@code jansStartDate} in
 * {@code [start, end)}.</li>
 * </ul>
 *
 * <p>To carry previous counts from one period to the next within a run, pass the last rows of each
 * series of the previous period together with the rows of the next period; rows before the period are
 * used only as previous counts.</p>
 */
public class MetricAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(MetricAggregator.class);

    public static final String AGGREGATION_OU = "aggregation";
    public static final int MAX_DN_LENGTH = 128;

    private static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private static final Comparator<String> NULLS_FIRST = Comparator.nullsFirst(Comparator.<String>naturalOrder());
    private static final Comparator<Date> DATES_NULLS_FIRST = Comparator.nullsFirst(Comparator.<Date>naturalOrder());

    private static final Comparator<RawRow> RAW_ROW_ORDER = Comparator
            .comparing((RawRow r) -> r.host, NULLS_FIRST)
            .thenComparing(r -> r.key.applicationType, NULLS_FIRST)
            .thenComparing(r -> r.key.metricType, NULLS_FIRST)
            .thenComparing(r -> r.key.metricSubType, NULLS_FIRST)
            .thenComparing(r -> r.entry.getEndDate(), DATES_NULLS_FIRST)
            .thenComparing(r -> r.entry.getId(), NULLS_FIRST)
            .thenComparing(r -> r.entry.getData(), NULLS_FIRST);

    private static final Comparator<ChildRow> CHILD_ROW_ORDER = Comparator
            .comparing((ChildRow r) -> r.key)
            .thenComparing(r -> r.entry.getStartDate(), DATES_NULLS_FIRST)
            .thenComparing(r -> r.entry.getId(), NULLS_FIRST)
            .thenComparing(r -> r.entry.getData(), NULLS_FIRST);

    private final String metricBaseDn;
    private final String nodeIdentifier;
    private final MetricAggregationConfiguration configuration;

    /**
     * @param metricBaseDn   base DN of the raw metric rows, for example {@code ou=statistic,o=metric}
     * @param nodeIdentifier identifier of the node that computes the aggregates; stored in {@code jansHost}
     * @param configuration  aggregation configuration; {@code null} means all defaults
     */
    public MetricAggregator(String metricBaseDn, String nodeIdentifier, MetricAggregationConfiguration configuration) {
        if (StringUtils.isBlank(metricBaseDn)) {
            throw new IllegalArgumentException("Metric base DN is required");
        }
        this.metricBaseDn = metricBaseDn;
        this.nodeIdentifier = nodeIdentifier;
        this.configuration = configuration == null ? new MetricAggregationConfiguration() : configuration;
    }

    /**
     * @return {@code ou=aggregation,<metricBaseDn>}, the same base DN the config-api metric plugin reads
     */
    public static String aggregationBaseDn(String metricBaseDn) {
        return "ou=" + AGGREGATION_OU + "," + metricBaseDn;
    }

    public String getAggregationBaseDn() {
        return aggregationBaseDn(metricBaseDn);
    }

    /**
     * Builds the DN {@code uniqueIdentifier=<id>,ou=aggregation,<metricBaseDn>}.
     *
     * @throws IllegalArgumentException if the DN is longer than {@value #MAX_DN_LENGTH} characters, the
     *                                  size of the SQL {@code dn} column
     */
    public String buildDn(String id) {
        String dn = "uniqueIdentifier=" + id + "," + getAggregationBaseDn();
        if (dn.length() > MAX_DN_LENGTH) {
            throw new IllegalArgumentException("Aggregation DN is longer than " + MAX_DN_LENGTH + " characters: " + dn);
        }
        return dn;
    }

    /**
     * Fixed ID of an aggregate: a name-based UUID of the aggregation type, application type, metric
     * type, subtype (null treated as empty), and period start epoch milliseconds. Each field is prefixed
     * with its character length and a colon, then the name is encoded in UTF-8. Computing the same
     * period again gives the same ID, so a re-run overwrites the same row.
     */
    public static String aggregateId(MetricAggregationType aggregationType, String applicationType, String metricType,
            String metricSubType, Instant periodStart) {
        StringBuilder name = new StringBuilder();
        for (String field : new String[] { aggregationType.getValue(), StringUtils.defaultString(applicationType),
                StringUtils.defaultString(metricType), StringUtils.defaultString(metricSubType),
                Long.toString(periodStart.toEpochMilli()) }) {
            name.append(field.length()).append(':').append(field);
        }
        return UUID.nameUUIDFromBytes(name.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * @return the level that a rollup of {@code target} reads: HOURLY for DAILY, DAILY for WEEKLY and
     *         MONTHLY
     * @throws IllegalArgumentException for HOURLY, which is built from raw rows
     */
    public static MetricAggregationType sourceLevel(MetricAggregationType target) {
        switch (target) {
        case DAILY:
            return MetricAggregationType.HOURLY;
        case WEEKLY:
        case MONTHLY:
            return MetricAggregationType.DAILY;
        default:
            throw new IllegalArgumentException("Aggregation type " + target + " is built from raw rows, not by rollup");
        }
    }

    /**
     * @return days that rows of {@code type} are kept, from the configuration
     */
    public int keepDays(MetricAggregationType type) {
        switch (type) {
        case HOURLY:
            return configuration.getKeepHourlyDays();
        case DAILY:
            return configuration.getKeepDailyDays();
        case WEEKLY:
            return configuration.getKeepWeeklyDays();
        case MONTHLY:
            return configuration.getKeepMonthlyDays();
        default:
            throw new IllegalArgumentException("Unsupported aggregation type: " + type);
        }
    }

    /**
     * Detects the kind of a raw {@code jansMetric} row from the shape of its {@code jansData}: timer rows
     * have {@code durationUnit}, counter rows have {@code count}.
     *
     * @return {@link TimerAggregateData#KIND}, {@link CounterAggregateData#KIND}, or {@code null} when the
     *         data is missing, not a JSON object, or of an unknown shape
     */
    public static String detectRawKind(String jansData) {
        return detectRawKind(readObject(jansData));
    }

    private static String detectRawKind(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.has("durationUnit")) {
            try {
                TimeUnit.valueOf(node.path("durationUnit").asText("").toUpperCase(Locale.ROOT));
                return TimerAggregateData.KIND;
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        if (node.has("count")) {
            return CounterAggregateData.KIND;
        }
        return null;
    }

    /**
     * Serializes aggregate data with the fixed mapper used for {@code jansData} of aggregation rows.
     */
    public static String toJson(AggregateData data) {
        try {
            return JSON_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize aggregate data", e);
        }
    }

    /**
     * Reads the {@code jansData} of an aggregation row.
     *
     * @return {@link CounterAggregateData} or {@link TimerAggregateData}, or {@code null} when the data is
     *         missing, not valid JSON, or of an unknown kind
     */
    public static AggregateData readAggregateData(String jansData) {
        JsonNode node = readObject(jansData);
        if (node == null) {
            return null;
        }
        String kind = node.path("kind").asText(null);
        try {
            if (CounterAggregateData.KIND.equals(kind)) {
                return JSON_MAPPER.treeToValue(node, CounterAggregateData.class);
            }
            if (TimerAggregateData.KIND.equals(kind)) {
                return JSON_MAPPER.treeToValue(node, TimerAggregateData.class);
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            LOG.debug("Unreadable aggregate data", e);
        }
        return null;
    }

    private static JsonNode readObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            return node != null && node.isObject() ? node : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Builds HOURLY (or any level) aggregates of raw rows for one period.
     *
     * @param rows         raw rows with {@code jansEndDate} inside the period, plus any earlier rows that
     *                     provide previous cumulative counts (lookback). Rows at or after the period end
     *                     are ignored. Rows with unreadable data are skipped.
     * @param bucket       the period
     * @param complete     value of {@code complete}: {@code true} when the period is closed, see
     *                     {@link PeriodMath#isClosed}
     * @param creationDate creation date of the entries; {@code exp} is this date plus the retention days
     *                     of the level
     * @return one entry per key with at least one row in the period, sorted by key
     */
    public List<MetricAggregationEntry> aggregateRaw(Collection<MetricDataEntry> rows, PeriodBucket bucket, boolean complete,
            Date creationDate) {
        Objects.requireNonNull(bucket, "bucket");
        Objects.requireNonNull(creationDate, "creationDate");

        List<RawRow> rawRows = new ArrayList<>();
        if (rows != null) {
            for (MetricDataEntry entry : rows) {
                RawRow rawRow = RawRow.of(entry);
                if (rawRow == null) {
                    LOG.debug("Skipping metric row with unknown data: {}", entry == null ? null : entry.getDn());
                } else if (rawRow.entry.getEndDate().toInstant().isBefore(bucket.getEnd())) {
                    rawRows.add(rawRow);
                }
            }
        }
        rawRows.sort(RAW_ROW_ORDER);

        RawAggregation aggregation = newRawAggregation(bucket, complete, creationDate);
        for (RawRow row : rawRows) {
            aggregation.addRow(row);
        }
        return aggregation.finish();
    }

    /** Starts a bounded-memory aggregation. Pages must be ordered by end date, then row ID per series. */
    public RawAggregation newRawAggregation(PeriodBucket bucket, boolean complete, Date creationDate) {
        return new RawAggregation(bucket, complete, creationDate);
    }

    public final class RawAggregation {
        private final PeriodBucket bucket;
        private final boolean complete;
        private final Date creationDate;
        private final Map<SeriesKey, Long> previousCounts = new HashMap<>();
        private final Set<SeriesKey> startedSeries = new HashSet<>();
        private final Map<AggregateKey, Accumulator> accumulators = new TreeMap<>();

        private RawAggregation(PeriodBucket bucket, boolean complete, Date creationDate) {
            this.bucket = Objects.requireNonNull(bucket, "bucket");
            this.complete = complete;
            this.creationDate = Objects.requireNonNull(creationDate, "creationDate");
        }

        public void addRows(Collection<MetricDataEntry> rows) {
            for (MetricDataEntry entry : rows) {
                RawRow row = RawRow.of(entry);
                if (row != null && row.entry.getEndDate().toInstant().isBefore(bucket.getEnd())) {
                    addRow(row);
                }
            }
        }

        private void addRow(RawRow row) {
            SeriesKey seriesKey = new SeriesKey(row.host, row.key);
            if (row.entry.getEndDate().toInstant().isBefore(bucket.getStart()) && startedSeries.contains(seriesKey)) {
                return;
            }
            Long previousCount = previousCounts.put(seriesKey, row.count);

            if (!bucket.contains(row.entry.getEndDate().toInstant())) {
                return;
            }

            startedSeries.add(seriesKey);
            long delta;
            boolean legacy;
            if (row.intervalCount != null) {
                delta = row.intervalCount;
                legacy = false;
            } else {
                delta = legacyDelta(row.count, previousCount);
                legacy = true;
            }

            Accumulator accumulator = accumulators.computeIfAbsent(row.key, k -> new Accumulator(row.kind));
            if (!accumulator.kind.equals(row.kind)) {
                LOG.debug("Skipping metric row of kind {} for key {} of kind {}", row.kind, row.key, accumulator.kind);
                return;
            }
            accumulator.addRaw(row, delta, legacy);
        }

        public List<MetricAggregationEntry> finish() {
            List<MetricAggregationEntry> result = new ArrayList<>();
            for (Map.Entry<AggregateKey, Accumulator> item : accumulators.entrySet()) {
                Accumulator accumulator = item.getValue();
                AggregateData data = accumulator.build(bucket, complete, accumulator.hosts.size());
                result.add(buildEntry(item.getKey(), bucket, data, creationDate));
            }

            return result;
        }
    }

    /**
     * Combines child aggregates of the source level into one period of a higher level: DAILY from
     * HOURLY, WEEKLY and MONTHLY from DAILY. Counter counts are summed; timers are combined as described
     * in {@link TimerAggregateData}.
     *
     * <p>{@code complete} is {@code true} when {@code childWatermark} is at or after the end of the period
     * (the source level has been processed for the whole period; periods without children are empty) and
     * every child is complete. Weekly and monthly periods must also lie wholly within daily source retention;
     * otherwise missing expired children cannot be distinguished from empty days.</p>
     *
     * @param children       aggregates of {@link #sourceLevel} whose {@code jansStartDate} lies in the period
     * @param bucket         the period; its type is the target level
     * @param childWatermark end of the last fully processed period of the source level, or {@code null}
     * @param creationDate   creation date of the entries
     * @return one entry per key with at least one child, sorted by key
     * @throws IllegalArgumentException if a child has another level or starts outside the period
     */
    public List<MetricAggregationEntry> rollup(Collection<MetricAggregationEntry> children, PeriodBucket bucket,
            Instant childWatermark, Date creationDate) {
        return rollup(children, bucket, childWatermark, null, creationDate);
    }

    /** Includes the first processed source period, so a watermark alone cannot imply earlier coverage. */
    public List<MetricAggregationEntry> rollup(Collection<MetricAggregationEntry> children, PeriodBucket bucket,
            Instant childWatermark, Instant sourceStart, Date creationDate) {
        Objects.requireNonNull(bucket, "bucket");
        Objects.requireNonNull(creationDate, "creationDate");
        MetricAggregationType sourceLevel = sourceLevel(bucket.getType());

        List<ChildRow> childRows = new ArrayList<>();
        Set<AggregateKey> unreadableKeys = new HashSet<>();
        if (children != null) {
            for (MetricAggregationEntry child : children) {
                if (child.getAggregationType() != sourceLevel) {
                    throw new IllegalArgumentException("Expected " + sourceLevel + " aggregate for " + bucket + ", got "
                            + child.getAggregationType() + ": " + child.getDn());
                }
                if (child.getStartDate() == null || !bucket.contains(child.getStartDate().toInstant())) {
                    throw new IllegalArgumentException("Aggregate " + child.getDn() + " does not start in " + bucket);
                }
                AggregateData data = readAggregateData(child.getData());
                if (data == null) {
                    unreadableKeys.add(new AggregateKey(child.getApplicationType(), child.getMetricType(), child.getMetricSubType()));
                    LOG.debug("Skipping aggregate with unreadable data: {}", child.getDn());
                    continue;
                }
                childRows.add(new ChildRow(child, data));
            }
        }
        childRows.sort(CHILD_ROW_ORDER);

        boolean sourceProcessed = childWatermark != null && !childWatermark.isBefore(bucket.getEnd())
                && (sourceStart == null || !bucket.getStart().isBefore(sourceStart))
                && (sourceLevel == MetricAggregationType.DAILY ? hasRetainedDailySource(bucket, creationDate.toInstant())
                        : !bucket.getStart().isBefore(creationDate.toInstant().minus(Duration.ofDays(keepDays(sourceLevel)))));

        Map<AggregateKey, Accumulator> accumulators = new TreeMap<>();
        for (ChildRow row : childRows) {
            Accumulator accumulator = accumulators.computeIfAbsent(row.key, k -> new Accumulator(row.data.getKind()));
            if (!accumulator.kind.equals(row.data.getKind())) {
                unreadableKeys.add(row.key);
                LOG.debug("Skipping aggregate of kind {} for key {} of kind {}", row.data.getKind(), row.key, accumulator.kind);
                continue;
            }
            accumulator.addChild(row.data);
        }

        List<MetricAggregationEntry> result = new ArrayList<>();
        for (Map.Entry<AggregateKey, Accumulator> item : accumulators.entrySet()) {
            Accumulator accumulator = item.getValue();
            boolean complete = sourceProcessed && accumulator.allChildrenComplete && !unreadableKeys.contains(item.getKey());
            AggregateData data = accumulator.build(bucket, complete, accumulator.maxChildNodes);
            result.add(buildEntry(item.getKey(), bucket, data, creationDate));
        }

        return result;
    }

    /**
     * Whether all daily children of a period are still within retention at read time. Keep the exact
     * cutoff: rounding it down to a day would admit a first day whose rows may already have expired.
     */
    boolean hasRetainedDailySource(PeriodBucket bucket, Instant readTime) {
        return !bucket.getStart().isBefore(readTime.minus(Duration.ofDays(configuration.getKeepDailyDays())));
    }

    static long legacyDelta(long count, Long previousCount) {
        if (previousCount == null) {
            return 0;
        }
        if (count < previousCount) {
            return count;
        }
        return count - previousCount;
    }

    private MetricAggregationEntry buildEntry(AggregateKey key, PeriodBucket bucket, AggregateData data, Date creationDate) {
        MetricAggregationType type = bucket.getType();
        String id = aggregateId(type, key.applicationType, key.metricType, key.metricSubType, bucket.getStart());

        MetricAggregationEntry entry = new MetricAggregationEntry(buildDn(id), id, creationDate);
        entry.setAggregationType(type);
        entry.setStartDate(bucket.getStartDate());
        entry.setEndDate(Date.from(bucket.getEnd().minusMillis(1)));
        entry.setApplicationType(key.applicationType);
        entry.setMetricType(key.metricType);
        entry.setMetricSubType(key.metricSubType);
        entry.setData(toJson(data));
        entry.setNodeIdentifier(nodeIdentifier);

        Instant expiration = creationDate.toInstant().plus(Duration.ofDays(keepDays(type)));
        entry.setExpirationDate(Date.from(expiration));
        entry.setTtl(Math.toIntExact(Duration.between(creationDate.toInstant(), expiration).getSeconds()));
        entry.setDeletable(true);

        return entry;
    }

    private static final class AggregateKey implements Comparable<AggregateKey> {

        private static final Comparator<AggregateKey> ORDER = Comparator
                .comparing((AggregateKey k) -> k.applicationType, NULLS_FIRST)
                .thenComparing(k -> k.metricType, NULLS_FIRST)
                .thenComparing(k -> k.metricSubType, NULLS_FIRST);

        private final String applicationType;
        private final String metricType;
        private final String metricSubType;

        private AggregateKey(String applicationType, String metricType, String metricSubType) {
            this.applicationType = applicationType;
            this.metricType = metricType;
            this.metricSubType = StringUtils.isEmpty(metricSubType) ? null : metricSubType;
        }

        @Override
        public int compareTo(AggregateKey other) {
            return ORDER.compare(this, other);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AggregateKey)) {
                return false;
            }
            AggregateKey other = (AggregateKey) obj;
            return Objects.equals(applicationType, other.applicationType) && Objects.equals(metricType, other.metricType)
                    && Objects.equals(metricSubType, other.metricSubType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(applicationType, metricType, metricSubType);
        }

        @Override
        public String toString() {
            return applicationType + "/" + metricType + "/" + metricSubType;
        }
    }

    private static final class SeriesKey {

        private final String host;
        private final AggregateKey key;

        private SeriesKey(String host, AggregateKey key) {
            this.host = host;
            this.key = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SeriesKey)) {
                return false;
            }
            SeriesKey other = (SeriesKey) obj;
            return Objects.equals(host, other.host) && key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, key);
        }
    }

    /** Whether a raw row can safely contribute statistics or a cumulative-count baseline. */
    public static boolean isReadableRaw(MetricDataEntry entry) {
        return RawRow.of(entry) != null;
    }

    private static final class RawRow {

        private final MetricDataEntry entry;
        private final JsonNode data;
        private final String kind;
        private final String host;
        private final AggregateKey key;
        private final long count;
        private final Long intervalCount;

        private RawRow(MetricDataEntry entry, JsonNode data, String kind) {
            this.entry = entry;
            this.data = data;
            this.kind = kind;
            this.host = entry.getNodeIdentifier();
            this.key = new AggregateKey(entry.getApplicationType(), entry.getMetricType(), entry.getMetricSubType());
            this.count = data.path("count").asLong();
            this.intervalCount = data.has("intervalCount") ? data.get("intervalCount").asLong() : null;
        }

        private static RawRow of(MetricDataEntry entry) {
            if (entry == null || entry.getEndDate() == null || entry.getMetricType() == null) {
                return null;
            }
            JsonNode data = readObject(entry.getData());
            String kind = detectRawKind(data);
            if (TimerAggregateData.KIND.equals(kind)) {
                String unit = data.path("durationUnit").asText(null);
                if (unit == null) {
                    return null;
                }
                try {
                    TimeUnit.valueOf(unit.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException unreadableUnit) {
                    return null;
                }
            }
            return kind == null ? null : new RawRow(entry, data, kind);
        }

        private double stat(String name) {
            return data.path(name).asDouble();
        }
    }

    private static final class ChildRow {

        private final MetricAggregationEntry entry;
        private final AggregateData data;
        private final AggregateKey key;

        private ChildRow(MetricAggregationEntry entry, AggregateData data) {
            this.entry = entry;
            this.data = data;
            this.key = new AggregateKey(entry.getApplicationType(), entry.getMetricType(), entry.getMetricSubType());
        }
    }

    /**
     * Sums for one key. Values are added in the order of the sorted input only, so the floating-point
     * results do not depend on the input order.
     */
    private static final class Accumulator {

        private final String kind;

        private long count;
        private long sourceRows;
        private boolean legacyDelta;
        private final Set<String> hosts = new TreeSet<>();
        private int maxChildNodes;
        private boolean allChildrenComplete = true;

        private long weight;
        private Double min;
        private Double max;
        private double weightedMean;
        private double weightedMedian;
        private double weighted75th;
        private double weighted95th;
        private double weighted98th;
        private double weighted99th;
        private double weighted999th;

        private Accumulator(String kind) {
            this.kind = kind;
        }

        private boolean isTimer() {
            return TimerAggregateData.KIND.equals(kind);
        }

        private void addRaw(RawRow row, long delta, boolean legacy) {
            count += delta;
            sourceRows++;
            legacyDelta |= legacy;
            hosts.add(StringUtils.defaultString(row.host));

            if (isTimer()) {
                if (delta > 0) {
                    // intervalCount supplies the weight, not interval-local duration samples.
                    // These reservoir extrema remain estimates under TimerAggregateData.approximate.
                    addTimerStats(delta, row.data.path("durationUnit").asText(null),
                            row.stat("min"), row.stat("max"), row.stat("mean"), row.stat("median"),
                            row.stat("value75thPercentile"), row.stat("value95thPercentile"), row.stat("value98thPercentile"),
                            row.stat("value99thPercentile"), row.stat("value999thPercentile"));
                }
            }
        }

        private void addChild(AggregateData data) {
            count += data.getCount();
            sourceRows += data.getSourceRows();
            legacyDelta |= data.isLegacyDelta();
            maxChildNodes = Math.max(maxChildNodes, data.getNodes());
            allChildrenComplete &= data.isComplete();

            if (isTimer()) {
                TimerAggregateData timer = (TimerAggregateData) data;
                if (timer.getCount() > 0 && timer.getMin() != null) {
                    addTimerStats(timer.getCount(), timer.getDurationUnit(), timer.getMin(), value(timer.getMax()), value(timer.getMean()),
                            value(timer.getMedian()), value(timer.getValue75thPercentile()),
                            value(timer.getValue95thPercentile()), value(timer.getValue98thPercentile()),
                            value(timer.getValue99thPercentile()), value(timer.getValue999thPercentile()));
                }
            }
        }

        private void addTimerStats(long w, String sourceUnit, double rowMin, double rowMax, double mean, double median, double p75, double p95,
                double p98, double p99, double p999) {
            if (sourceUnit == null) {
                throw new IllegalArgumentException("Timer duration unit is required");
            }
            // Reporter duration units are lowercase TimeUnit names. Convert floating-point values
            // before combining them; integer TimeUnit conversion would truncate sub-millisecond values.
            double scale = TimeUnit.valueOf(sourceUnit.toUpperCase(Locale.ROOT)).toNanos(1)
                    / (double) TimeUnit.MILLISECONDS.toNanos(1);
            rowMin *= scale;
            rowMax *= scale;
            mean *= scale;
            median *= scale;
            p75 *= scale;
            p95 *= scale;
            p98 *= scale;
            p99 *= scale;
            p999 *= scale;
            weight += w;
            min = min == null ? rowMin : Math.min(min, rowMin);
            max = max == null ? rowMax : Math.max(max, rowMax);
            weightedMean += w * mean;
            weightedMedian += w * median;
            weighted75th += w * p75;
            weighted95th += w * p95;
            weighted98th += w * p98;
            weighted99th += w * p99;
            weighted999th += w * p999;
        }

        private static double value(Double value) {
            return value == null ? 0d : value;
        }

        private AggregateData build(PeriodBucket bucket, boolean complete, int nodes) {
            AggregateData data;
            if (isTimer()) {
                TimerAggregateData timer = new TimerAggregateData();
                long seconds = bucket.getLengthSeconds();
                timer.setRate(seconds > 0 ? (double) count / seconds : 0d);
                timer.setDurationUnit("milliseconds");
                if (weight > 0) {
                    timer.setMin(min);
                    timer.setMax(max);
                    timer.setMean(weightedMean / weight);
                    timer.setMedian(weightedMedian / weight);
                    timer.setValue75thPercentile(weighted75th / weight);
                    timer.setValue95thPercentile(weighted95th / weight);
                    timer.setValue98thPercentile(weighted98th / weight);
                    timer.setValue99thPercentile(weighted99th / weight);
                    timer.setValue999thPercentile(weighted999th / weight);
                }
                data = timer;
            } else {
                data = new CounterAggregateData();
            }

            data.setCount(count);
            data.setSourceRows(sourceRows);
            data.setNodes(nodes);
            data.setComplete(complete);
            data.setLegacyDelta(legacyDelta);
            return data;
        }
    }

}
