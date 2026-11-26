/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.model.cluster.ClusterLock;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.model.metric.ldap.OrderedRawEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.MetricAggregationEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.cluster.ClusterLockService;
import io.jans.service.metric.MetricService;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Builds the HOURLY, DAILY, WEEKLY and MONTHLY rows of {@code jansMetricAggregation} from the raw
 * {@code jansMetric} rows of one application type. Every node of the application runs it; for each run
 * slot, the node whose cluster claim wins does the work.
 *
 * <p>Run cycle ({@link #runOnce()}):</p>
 * <ol>
 * <li>Return when gating is off ({@link #isEnabled()}): nothing is read or written.</li>
 * <li>Claim slot {@code floor(now / runIntervalSeconds)} of task {@code metric-aggregation_<appType>}.
 * Return when another node holds it, or when the claim cannot be inserted (for example a missing
 * {@code jansClusterLock} table, or a backend without insert-or-fail).</li>
 * <li>When another claim of the task is still active, release the new claim as {@code skipped}.</li>
 * <li>Read the watermarks from the task state row. A watermark is the end of the last processed bucket of
 * a level. A missing state row starts each level at the raw-data horizon ({@code now -
 * metricReporterKeepDataDays}, aligned down to the level). Plan the closed buckets of the recompute
 * window plus the closed buckets from the watermark forward, at most {@code maxBucketsPerRun} per level.
 * A rollup level only plans buckets that end at or before the watermark of its source level.</li>
 * <li>HOURLY buckets: one paged query on {@code jansMetric} per bucket. Previous cumulative counts of
 * legacy rows (rows without {@code intervalCount}) are carried from bucket to bucket within a run; only
 * when a bucket has a legacy series without a carried row does one extra query read up to
 * {@link #getLegacyLookback()} before the bucket. Then DAILY from HOURLY rows, WEEKLY and MONTHLY from
 * DAILY rows.</li>
 * <li>Each aggregate is written as an upsert. Buckets that an earlier run already wrote (recompute
 * window) use {@code find}, then {@code merge}, or {@code persist} when missing; new buckets use
 * {@code persist}. A duplicate on {@code persist} falls back to {@code find} plus {@code merge}. Aggregates
 * have fixed IDs, so a re-run overwrites the same rows.</li>
 * <li>The claim heartbeat is renewed after each bucket; when it fails, the run stops. A level stops at its
 * first failed bucket, and its watermark does not move past it. After each level the watermarks are saved
 * (merged with the stored ones, so they only move forward).</li>
 * <li>The claim is released as {@code done} with a summary, and expired claims of the task are purged.</li>
 * </ol>
 *
 * <p>Raw rows that arrive after their bucket left the recompute window are not counted.</p>
 *
 * <p>Subclasses are {@code @ApplicationScoped} beans of an application. They supply the hooks and call
 * {@link #initTimer()} after the metric service timer has started.</p>
 */
public abstract class MetricAggregationManager {

    private static final Logger LOG = LoggerFactory.getLogger(MetricAggregationManager.class);

    /** Prefix of the cluster claim task; the application type follows it. */
    public static final String TASK_PREFIX = "metric-aggregation_";

    /** Longest period between two timer events; a run starts on the first event of each new slot. */
    public static final int TIMER_INTERVAL_SECONDS = 60;

    public static final int TIMER_DELAY_SECONDS = 60;

    /** Expired claims of the task removed at the end of a run, at most. */
    public static final int PURGE_LIMIT = 100;

    public static final int QUERY_CHUNK_SIZE = 1000;

    public static final int DEFAULT_MAX_ROWS_PER_QUERY = 500_000;

    public static final Duration DEFAULT_LEGACY_LOOKBACK = Duration.ofDays(1);

    /** Used when the application reports no positive {@code metricReporterKeepDataDays}. */
    public static final int DEFAULT_KEEP_DATA_DAYS = 15;

    /** Used when the application reports no positive {@code metricReporterInterval}. */
    public static final int DEFAULT_REPORTER_INTERVAL_SECONDS = 60;

    public static final Duration WARNING_INTERVAL = Duration.ofHours(1);

    static final String SUMMARY_SLOT = "slot";
    static final String SUMMARY_ACTIVE_SLOT = "activeSlot";
    static final String SUMMARY_ROWS = "rows";
    static final String SUMMARY_FAILED = "failed";
    static final String SUMMARY_DURATION = "ms";
    static final String SUMMARY_ERROR = "error";

    private static final List<MetricAggregationType> LEVELS = Collections.unmodifiableList(Arrays.asList(MetricAggregationType.HOURLY,
            MetricAggregationType.DAILY, MetricAggregationType.WEEKLY, MetricAggregationType.MONTHLY));

    private static final String SORT_BY = "uniqueIdentifier";
    private static final String SERIES_SEPARATOR = "\u0000";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Inject
    private Event<TimerEvent> timerEvent;

    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final AtomicLong lastScheduledSlot = new AtomicLong(Long.MIN_VALUE);
    private final ConcurrentMap<String, Instant> lastWarnings = new ConcurrentHashMap<>();

    private volatile Clock clock;
    private volatile boolean aggregationBranchReady;

    /**
     * Starts the timer. The timer fires every {@value #TIMER_INTERVAL_SECONDS} seconds (or every
     * {@code runIntervalSeconds}, when shorter); a run starts on the first event of each new slot.
     */
    public void initTimer() {
        int runIntervalSeconds = getConfiguration().getRunIntervalSeconds();
        int intervalSeconds = Math.max(1, Math.min(TIMER_INTERVAL_SECONDS, runIntervalSeconds));
        LOG.debug("Initializing metric aggregation timer, interval {} s", intervalSeconds);

        timerEvent.fire(new TimerEvent(new TimerSchedule(TIMER_DELAY_SECONDS, intervalSeconds), new MetricAggregationEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void processMetricAggregationEvent(@Observes @Scheduled MetricAggregationEvent event) {
        if (!isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            runOnSchedule();
        } catch (Throwable ex) {
            LOG.error("Metric aggregation run failed", ex);
        } finally {
            isActive.set(false);
        }
    }

    /**
     * Runs {@link #runOnce()} when the current slot differs from the slot of the previous scheduled call.
     *
     * @return the run result, or {@code null} when the slot was already handled by this instance
     */
    public MetricAggregationRunResult runOnSchedule() {
        Instant now = getClock().instant();
        MetricAggregationConfiguration configuration = snapshotConfiguration();
        long slot = slotOf(now, configuration.getRunIntervalSeconds());
        long previous = lastScheduledSlot.getAndSet(slot);
        if (previous == slot) {
            return null;
        }

        return runOnce(now, configuration);
    }

    /**
     * Runs one aggregation cycle for the current slot. Never throws for persistence errors.
     */
    public MetricAggregationRunResult runOnce() {
        return runOnce(getClock().instant(), snapshotConfiguration());
    }

    private MetricAggregationRunResult runOnce(Instant now, MetricAggregationConfiguration configuration) {
        long startNanos = System.nanoTime();
        if (!isEnabled()) {
            LOG.trace("Metric aggregation is disabled");
            return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.DISABLED, -1);
        }

        ClusterLock claim = null;
        ClusterLockService lockService = null;
        long slot = -1;
        try {
            MetricService metricService = getMetricService();
            lockService = getClusterLockService();
            String applicationType = metricService.getString();
            String task = taskName(applicationType);
            slot = slotOf(now, configuration.getRunIntervalSeconds());
            String slotKey = Long.toString(slot);

            Optional<ClusterLock> claimed = lockService.tryClaim(task, slotKey, Duration.ofSeconds(configuration.getLeaseSeconds()));
            if (!claimed.isPresent()) {
                LOG.debug("Metric aggregation for '{}' skips slot {}: not claimed by this node", applicationType, slot);
                return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.NOT_CLAIMED, slot);
            }
            claim = claimed.get();

            Optional<ClusterLock> active = lockService.findActive(task, slotKey, getClock().instant());
            if (active.isPresent()) {
                Object activeSlot = activeSlotKey(active.get());
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put(SUMMARY_SLOT, slot);
                summary.put(SUMMARY_ACTIVE_SLOT, activeSlot);
                if (!releaseClaim(lockService, claim, ClusterLock.STATUS_SKIPPED, toJson(summary))) {
                    return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.RELEASE_UNCONFIRMED, slot);
                }
                LOG.info("Metric aggregation for '{}' skips slot {}: the run of slot {} is still active", applicationType, slot, activeSlot);
                return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.SKIPPED, slot);
            }

            Map<MetricAggregationType, Instant> stored;
            String storedJson;
            try {
                // Register before reading any sources. A later registration fences this run at commit.
                if (!lockService.saveState(claim, current -> current)) {
                    throw new IllegalStateException("Failed to register the task writer");
                }
                storedJson = lockService.loadState(task).orElse(null);
                stored = parseState(storedJson);
            } catch (RuntimeException ex) {
                if (shouldWarn("state-read")) {
                    LOG.warn("Metric aggregation for '{}' skips slot {}: failed to read the task state ({}: {})", applicationType, slot,
                            ex.getClass().getName(), ex.getMessage());
                }
                LOG.debug("Failed to read the state of task '{}'", task, ex);
                if (!releaseClaim(lockService, claim, ClusterLock.STATUS_FAILED, errorSummary(slot, "state"))) {
                    return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.RELEASE_UNCONFIRMED, slot);
                }
                return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.STATE_ERROR, slot);
            }

            RunContext context = new RunContext(metricService, lockService, configuration, claim, task, applicationType, now);
            context.coverageStarts.putAll(parseCoverageStarts(storedJson));
            context.savedCoverageStarts.putAll(context.coverageStarts);
            initWatermarks(context, stored);
            prepareAggregationBranch(context);

            for (MetricAggregationType level : LEVELS) {
                processLevel(context, level);
                if (context.lost) {
                    break;
                }
                saveWatermarks(context);
            }

            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            boolean stateError = !context.saved.equals(context.watermarks)
                    || !context.savedCoverageStarts.equals(context.coverageStarts);
            MetricAggregationRunResult result = new MetricAggregationRunResult(
                    context.lost ? MetricAggregationRunResult.Status.LOST : stateError ? MetricAggregationRunResult.Status.STATE_ERROR : MetricAggregationRunResult.Status.DONE, slot, context.buckets,
                    context.failedBuckets, context.rowsWritten, durationMillis);

            if (context.lost) {
                LOG.info("Metric aggregation for '{}' slot {} stopped: claim lost; buckets {}, rows written {}, {} ms", applicationType, slot,
                        context.buckets, context.rowsWritten, durationMillis);
                return result;
            }

            if (!releaseClaim(lockService, claim, stateError ? ClusterLock.STATUS_FAILED : ClusterLock.STATUS_DONE,
                    stateError ? stateSaveFailureSummary(result) : doneSummary(result))) {
                return new MetricAggregationRunResult(MetricAggregationRunResult.Status.RELEASE_UNCONFIRMED, slot, context.buckets,
                        context.failedBuckets, context.rowsWritten, durationMillis);
            }

            try {
				lockService.purgeExpired(task, PURGE_LIMIT);
			} catch (RuntimeException purgeEx) {
				LOG.debug("Failed to purge expired claims of task '{}'", task, purgeEx);
			}
			logRun(applicationType, context, result);

            return result;
        } catch (RuntimeException ex) {
            if (shouldWarn("run-failure:" + ex.getClass().getName())) {
                LOG.warn("Metric aggregation run of slot {} failed ({}: {})", slot, ex.getClass().getName(), ex.getMessage());
            }
            LOG.debug("Metric aggregation run of slot {} failed", slot, ex);
            if (claim != null) {
                if (!releaseClaim(lockService, claim, ClusterLock.STATUS_FAILED, errorSummary(slot, ex.getClass().getSimpleName()))) {
                    return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.RELEASE_UNCONFIRMED, slot);
                }
            }
            return MetricAggregationRunResult.of(MetricAggregationRunResult.Status.FAILED, slot);
        }
    }

    /**
     * Gating: the metric reporter of the application is enabled and the aggregation configuration is
     * enabled. Any error while reading these settings counts as disabled.
     */
    public boolean isEnabled() {
        try {
            MetricService metricService = getMetricService();
            return (metricService != null) && metricService.isMetricReporterEnabled() && getConfiguration().isEnabled();
        } catch (RuntimeException ex) {
            LOG.debug("Failed to evaluate metric aggregation gating", ex);
            return false;
        }
    }

    /**
     * @return the cluster claim task of an application type: {@code metric-aggregation_<applicationType>}
     */
    public static String taskName(String applicationType) {
        if (StringUtils.isBlank(applicationType)) {
            throw new IllegalArgumentException("Application type is blank");
        }

        return TASK_PREFIX + applicationType;
    }

    /**
     * @return {@code floor(now / runIntervalSeconds)}
     */
    public static long slotOf(Instant now, int runIntervalSeconds) {
        return Math.floorDiv(now.getEpochSecond(), (long) Math.max(1, runIntervalSeconds));
    }

    /**
     * @return the clock set with {@link #setClock(Clock)}, otherwise the clock of the cluster lock
     *         service
     */
    public Clock getClock() {
        Clock result = clock;
        if (result != null) {
            return result;
        }

        ClusterLockService lockService = getClusterLockService();
        return lockService != null ? lockService.getClock() : Clock.systemUTC();
    }

    /**
     * Replaces the clock, for tests.
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return the metric service of the application: application type, metric base DN, entry manager of
     *         the metric rows, node identifier and the reporter switch
     */
    protected abstract MetricService getMetricService();

    /**
     * @return the cluster lock service of the application, which holds the claims and the task state row
     */
    protected abstract ClusterLockService getClusterLockService();

    /**
     * @return the aggregation configuration of the application; {@code null} means all defaults
     */
    protected abstract MetricAggregationConfiguration getAggregationConfiguration();

    /**
     * @return {@code metricReporterInterval} of the application, in seconds
     */
    protected abstract int getMetricReporterIntervalSeconds();

    /**
     * @return {@code metricReporterKeepDataDays} of the application
     */
    protected abstract int getMetricReporterKeepDataDays();

    /**
     * @return how far before the first bucket of a run legacy rows are read for previous counts
     */
    protected Duration getLegacyLookback() {
        return DEFAULT_LEGACY_LOOKBACK;
    }

    /**
     * @return the maximum raw page size and rollup query size; raw buckets may span many pages
     */
    protected int getMaxRowsPerQuery() {
        return DEFAULT_MAX_ROWS_PER_QUERY;
    }

    protected MetricAggregationConfiguration getConfiguration() {
        MetricAggregationConfiguration configuration = getAggregationConfiguration();
        return configuration != null ? configuration : new MetricAggregationConfiguration();
    }

    private int getRawKeepDays() {
        int days = getMetricReporterKeepDataDays();
        return days > 0 ? days : DEFAULT_KEEP_DATA_DAYS;
    }

    private int getReporterIntervalSeconds() {
        int seconds = getMetricReporterIntervalSeconds();
        return seconds > 0 ? seconds : DEFAULT_REPORTER_INTERVAL_SECONDS;
    }

    private static Object activeSlotKey(ClusterLock claim) {
        String key = claim.getId().substring(claim.getTask().length() + 1);
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException nonNumericSlot) {
            return key;
        }
    }

    private void initWatermarks(RunContext context, Map<MetricAggregationType, Instant> stored) {
        Instant horizon = context.now.minus(Duration.ofDays(getRawKeepDays()));
        for (MetricAggregationType level : LEVELS) {
            Instant watermark = stored.get(level);
            if (watermark == null) {
                watermark = PeriodMath.bucketStart(level, horizon);
            }
            context.watermarks.put(level, watermark);
            Instant sourceStart = level == MetricAggregationType.HOURLY ? horizon
                    : context.coverageStarts.get(MetricAggregator.sourceLevel(level));
            Instant start = level == MetricAggregationType.HOURLY && context.coverageStarts.containsKey(level)
                    ? context.coverageStarts.get(level) : firstWholeBucket(level, sourceStart);
            context.coverageStarts.merge(level, start, (saved, required) -> saved.isAfter(required) ? saved : required);
        }
        context.saved.putAll(stored);
    }

    private void prepareAggregationBranch(RunContext context) {
        if (aggregationBranchReady) {
            return;
        }

        PersistenceEntryManager entryManager = context.entryManager;
        String branchDn = context.aggregator.getAggregationBaseDn();
        if (entryManager.hasBranchesSupport(branchDn) && !entryManager.contains(branchDn, SimpleBranch.class)) {
            try {
                entryManager.persist(new SimpleBranch(branchDn, MetricAggregator.AGGREGATION_OU));
                LOG.info("Created branch '{}'", branchDn);
            } catch (RuntimeException ex) {
                if (!entryManager.contains(branchDn, SimpleBranch.class)) {
                    throw ex;
                }
            }
        }
        aggregationBranchReady = true;
    }

    private void processLevel(RunContext context, MetricAggregationType level) {
        MetricAggregationConfiguration configuration = context.configuration;
        Duration sourceLifetime = Duration.ofDays(getSourceKeepDays(level, configuration));
        Instant retentionFloor = context.now.minus(sourceLifetime);
        Instant floor = firstWholeBucket(level, retentionFloor);
        Instant coverageStart = context.coverageStarts.get(level);
        if (level != MetricAggregationType.HOURLY) {
            Instant sourceStart = firstWholeBucket(level, context.coverageStarts.get(MetricAggregator.sourceLevel(level)));
            if (sourceStart.isAfter(coverageStart)) {
                coverageStart = sourceStart;
            }
        }
        if (coverageStart.isAfter(floor)) {
            floor = coverageStart;
        }
        Instant watermark = context.watermarks.get(level);
        if (watermark.isBefore(floor)) {
            watermark = floor;
            coverageStart = floor;
        }
        context.coverageStarts.put(level, coverageStart);
        retentionFloor = floor;

        Instant limit = level == MetricAggregationType.HOURLY ? null : context.watermarks.get(MetricAggregator.sourceLevel(level));
        List<PeriodBucket> plan = plan(level, watermark, context.now, context.grace, getRecompute(level, configuration),
                configuration.getMaxBucketsPerRun(), limit, retentionFloor);
        LOG.debug("Metric aggregation for '{}': {} plan {} (watermark {})", context.applicationType, level, plan, watermark);

        // Buckets that end at or before the watermark were written by an earlier run
        Instant processedUntil = watermark;

        PeriodBucket previous = null;
        for (PeriodBucket bucket : plan) {
            // Retention can advance while earlier levels or buckets are being processed.
            if (bucket.getStart().isBefore(getClock().instant().minus(sourceLifetime))) {
                continue;
            }
            try {
                boolean findFirst = !bucket.getEnd().isAfter(processedUntil);
                if (level == MetricAggregationType.HOURLY) {
                    processRawBucket(context, bucket, previous, findFirst);
                } else {
                    processRollupBucket(context, bucket, findFirst);
                }
                context.buckets.merge(level, 1, Integer::sum);
                if (!bucket.getStart().isAfter(watermark) && bucket.getEnd().isAfter(watermark)) {
                    watermark = bucket.getEnd();
                }
            } catch (RuntimeException ex) {
                context.failedBuckets.merge(level, 1, Integer::sum);
                context.carryValid = false;
                if (context.firstFailure == null) {
                    context.firstFailure = bucket + ": " + ex.getClass().getName() + ": " + ex.getMessage();
                }
                LOG.debug("Metric aggregation for '{}' failed in {}", context.applicationType, bucket, ex);
                if (!context.lockService.heartbeat(context.claim)) {
                    context.lost = true;
                }
                break;
            }

            previous = bucket;
            if (!context.lockService.heartbeat(context.claim)) {
                context.lost = true;
                break;
            }
        }

        context.watermarks.put(level, watermark);
    }

    /**
     * Lists the buckets to process at one level, oldest first: the last {@code recompute} closed
     * buckets, plus the closed buckets from the watermark forward, at most {@code maxBuckets} in total.
     * A bucket is closed when {@code now >= end + grace}; when {@code limit} is given, a bucket must also
     * end at or before it. With a cap of one, a ready forward bucket takes precedence over recomputation.
     */
    static List<PeriodBucket> plan(MetricAggregationType level, Instant watermark, Instant now, Duration grace, int recompute,
            int maxBuckets, Instant limit) {
        return plan(level, watermark, now, grace, recompute, maxBuckets, limit, null);
    }

    static List<PeriodBucket> plan(MetricAggregationType level, Instant watermark, Instant now, Duration grace, int recompute,
            int maxBuckets, Instant limit, Instant retentionFloor) {
        Instant upper = now.minus(grace);
        if ((limit != null) && limit.isBefore(upper)) {
            upper = limit;
        }

        PeriodBucket last = PeriodMath.bucketContaining(level, upper.minusMillis(1));
        if (last.getEnd().isAfter(upper)) {
            last = PeriodMath.previous(last);
        }

        TreeSet<PeriodBucket> buckets = new TreeSet<>();
        PeriodBucket bucket = last;
        for (int i = 0; i < recompute; i++) {
            if (retentionFloor != null && bucket.getStart().isBefore(retentionFloor)) {
                break;
            }
            buckets.add(bucket);
            bucket = PeriodMath.previous(bucket);
        }

        bucket = PeriodMath.bucketContaining(level, watermark);
        if (retentionFloor != null && bucket.getStart().isBefore(retentionFloor)) {
            bucket = PeriodMath.bucketContaining(level, retentionFloor);
            if (bucket.getStart().isBefore(retentionFloor)) {
                bucket = PeriodMath.next(bucket);
            }
        }
        for (int i = 0; (i < maxBuckets) && !bucket.getEnd().isAfter(upper); i++) {
            buckets.add(bucket);
            bucket = PeriodMath.next(bucket);
        }

        if (maxBuckets == 1 && !bucketForForward(level, watermark, retentionFloor).getEnd().isAfter(upper)) {
            return new ArrayList<>(Collections.singletonList(bucketForForward(level, watermark, retentionFloor)));
        }

        List<PeriodBucket> result = new ArrayList<>(Math.min(buckets.size(), maxBuckets));
        Iterator<PeriodBucket> iterator = buckets.iterator();
        while (iterator.hasNext() && (result.size() < maxBuckets)) {
            result.add(iterator.next());
        }

        return result;
    }

    private static Instant firstWholeBucket(MetricAggregationType level, Instant from) {
        Instant start = PeriodMath.bucketStart(level, from);
        return start.isBefore(from) ? PeriodMath.bucketEnd(level, start) : start;
    }

    private static PeriodBucket bucketForForward(MetricAggregationType level, Instant watermark, Instant retentionFloor) {
        Instant start = PeriodMath.bucketStart(level, watermark);
        if (retentionFloor != null && start.isBefore(retentionFloor)) {
            start = firstWholeBucket(level, retentionFloor);
        }
        return PeriodMath.bucketContaining(level, start);
    }

    private int processRawBucket(RunContext context, PeriodBucket bucket, PeriodBucket previous, boolean findFirst) {
        if ((previous == null) || !context.carryValid || !previous.getEnd().equals(bucket.getStart())) {
            context.carry.clear();
        }
        context.carryValid = false;

        MetricAggregator.RawAggregation aggregation = context.aggregator.newRawAggregation(bucket,
                PeriodMath.isClosed(bucket, context.now, context.grace), context.creationDate);
        aggregation.addRows(context.carry.values());
        Map<String, MetricDataEntry> baselineIndex = new HashMap<>();
        boolean[] lookbackRead = { false };
        readRawPages(context, bucket.getStart(), bucket.getEnd(), rows -> {
            Set<String> missing = new LinkedHashSet<>();
            for (MetricDataEntry row : rows) {
                JsonNode data = readObject(row.getData());
                if (data != null && !data.hasNonNull("intervalCount") && MetricAggregator.isReadableRaw(row)
                        && !context.carry.containsKey(seriesKey(row))) {
                    missing.add(seriesKey(row));
                }
            }
            if (!missing.isEmpty()) {
                if (!lookbackRead[0]) {
                    readRawPages(context, bucket.getStart().minus(getLegacyLookback()), bucket.getStart(),
                            older -> remember(baselineIndex, older));
                    lookbackRead[0] = true;
                }
                List<MetricDataEntry> baselines = new ArrayList<>();
                for (String series : missing) {
                    MetricDataEntry baseline = baselineIndex.get(series);
                    if (baseline != null) {
                        baselines.add(baseline);
                    }
                }
                aggregation.addRows(baselines);
                remember(context.carry, baselines);
            }
            aggregation.addRows(rows);
            remember(context.carry, rows);
        });
        context.carryValid = true;
        return write(context, bucket, aggregation.finish(), findFirst);
    }

    private int processRollupBucket(RunContext context, PeriodBucket bucket, boolean findFirst) {
        MetricAggregationType sourceLevel = MetricAggregator.sourceLevel(bucket.getType());
        List<MetricAggregationEntry> children = readAggregates(context, sourceLevel, bucket.getStart(), bucket.getEnd());
        List<MetricAggregationEntry> entries = context.aggregator.rollup(children, bucket, context.watermarks.get(sourceLevel),
                context.coverageStarts.get(sourceLevel), Date.from(getClock().instant()));

        return write(context, bucket, entries, findFirst);
    }

    /** Keyset paging in the projection's composite SQL order. */
    private void readRawPages(RunContext context, Instant from, Instant to, Consumer<List<MetricDataEntry>> consume) {
        PersistenceEntryManager manager = context.entryManager;
        String baseDn = context.metricBaseDn;
        int pageSize = Math.max(1, Math.min(QUERY_CHUNK_SIZE, getMaxRowsPerQuery()));
        Filter range = Filter.createANDFilter(Filter.createEqualityFilter("jansAppTyp", context.applicationType),
                Filter.createGreaterOrEqualFilter("jansEndDate", manager.encodeTime(baseDn, Date.from(from))),
                Filter.createLessOrEqualFilter("jansEndDate", manager.encodeTime(baseDn, Date.from(to.minusMillis(1)))));
        Filter pageFilter = range;
        MetricDataEntry cursor = null;
        while (true) {
            List<OrderedRawEntry> page = manager.findEntries(baseDn, OrderedRawEntry.class, pageFilter, (String[]) null,
                    null, SortOrder.ASCENDING, 0, pageSize, pageSize);
            if (page == null) {
                throw new IllegalStateException("Raw metric page was not returned");
            }
            if (page.isEmpty()) {
                return;
            }
            MetricDataEntry last = page.get(page.size() - 1);
            if (last.getEndDate() == null || last.getId() == null || (cursor != null && !isAfter(last, cursor))) {
                throw new IllegalStateException("Raw metric page did not advance");
            }
            consume.accept(new ArrayList<>(page));
            if (page.size() < pageSize) {
                return;
            }
            cursor = last;
            String end = manager.encodeTime(baseDn, cursor.getEndDate());
            pageFilter = Filter.createANDFilter(range, Filter.createORFilter(
                    Filter.createNOTFilter(Filter.createLessOrEqualFilter("jansEndDate", end)),
                    Filter.createANDFilter(Filter.createEqualityFilter("jansEndDate", end),
                            Filter.createNOTFilter(Filter.createLessOrEqualFilter("uniqueIdentifier", cursor.getId())))));
        }
    }

    private List<MetricAggregationEntry> readAggregates(RunContext context, MetricAggregationType type, Instant from, Instant to) {
        PersistenceEntryManager entryManager = context.entryManager;
        String baseDn = context.aggregator.getAggregationBaseDn();
        Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansAppTyp", context.applicationType),
                Filter.createEqualityFilter("jansAggregationType", type.getValue()),
                Filter.createGreaterOrEqualFilter("jansStartDate", entryManager.encodeTime(baseDn, Date.from(from))),
                Filter.createLessOrEqualFilter("jansStartDate", entryManager.encodeTime(baseDn, Date.from(to.minusMillis(1)))));

        return search(entryManager, baseDn, MetricAggregationEntry.class, filter, MetricAggregationEntry::getDn);
    }

    /**
     * One paged search, sorted by the unique {@code uniqueIdentifier} so that SQL offset paging neither
     * repeats nor skips rows. The count is explicit because LDAP returns nothing for a count of 0 on this
     * call.
     */
    private <T> List<T> search(PersistenceEntryManager entryManager, String baseDn, Class<T> entryClass, Filter filter,
            Function<T, String> dnGetter) {
        int maxRows = getMaxRowsPerQuery();
        List<T> entries = entryManager.findEntries(baseDn, entryClass, filter, (String[]) null, SORT_BY, SortOrder.ASCENDING, 0, maxRows,
                QUERY_CHUNK_SIZE);
        if (entries == null) {
            return new ArrayList<>();
        }
        if (entries.size() >= maxRows) {
            throw new IllegalStateException(String.format("Query of %s returned %d rows or more", entryClass.getSimpleName(), maxRows));
        }

        Set<String> dns = new LinkedHashSet<>();
        List<T> result = new ArrayList<>(entries.size());
        for (T entry : entries) {
            String dn = dnGetter.apply(entry);
            if ((dn == null) || dns.add(dn)) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Writes the aggregates of one bucket (D14). A bucket that an earlier run already wrote (inside the
     * recompute window) is written find-first: {@code find}, then {@code merge}, or {@code persist} when the
     * row is missing. A new bucket is written persist-first. Either way, a duplicate on {@code persist} (a
     * race with another node) falls back to {@code find} plus {@code merge}. Find-first avoids a failed
     * insert, and with it a database server error log line, for every recomputed row.
     */
    private int write(RunContext context, PeriodBucket bucket, List<MetricAggregationEntry> entries, boolean findFirst) {
        Consumer<Runnable> transaction = work -> context.lockService.withActiveClaim(context.claim, context.entryManager,
                context.aggregator.getAggregationBaseDn(), () -> {
                    ClusterLock writer = context.entryManager.findForUpdate(ClusterLock.class,
                            context.lockService.buildStateDn(context.task));
                    if (writer == null || !context.claim.getLockKey().equals(writer.getLockKey())) {
                        throw new IllegalStateException("Task writer changed before aggregate commit");
                    }
                    checkRetention(context, bucket);
                    work.run();
                    checkRetention(context, bucket);
                    return null;
                });
        try {
            transaction.accept(() -> {
                for (MetricAggregationEntry entry : entries) {
                    upsertInTransaction(context.entryManager, entry, findFirst);
                }
            });
        } catch (RuntimeException ex) {
            if (!ClusterLockService.isDuplicate(ex)) {
                throw ex;
            }
            // PostgreSQL aborts a transaction on a duplicate. Retry the entire bucket only after
            // rollback, in a fresh fenced transaction which reads existing rows before inserting.
            transaction.accept(() -> {
                for (MetricAggregationEntry entry : entries) {
                    upsertInTransaction(context.entryManager, entry, true);
                }
            });
        }
        context.rowsWritten += entries.size();
        return entries.size();
    }

    private void checkRetention(RunContext context, PeriodBucket bucket) {
        if (bucket.getStart().isBefore(getClock().instant().minus(Duration.ofDays(
                getSourceKeepDays(bucket.getType(), context.configuration))))) {
            throw new IllegalStateException("Retention no longer covers the complete bucket");
        }
    }

    private static void upsertInTransaction(PersistenceEntryManager manager, MetricAggregationEntry entry, boolean findFirst) {
        if (findFirst) {
            MetricAggregationEntry existing = findExisting(manager, entry.getDn());
            if (existing != null) {
                mergeInto(manager, existing, entry);
                return;
            }
        }
        manager.persist(entry);
    }

    static void upsert(PersistenceEntryManager entryManager, MetricAggregationEntry entry, boolean findFirst,
            Consumer<Runnable> transaction) {
        try {
            transaction.accept(() -> {
                if (findFirst) {
                    MetricAggregationEntry existing = findExisting(entryManager, entry.getDn());
                    if (existing != null) {
                        mergeInto(entryManager, existing, entry);
                        return;
                    }
                }
                entryManager.persist(entry);
            });
        } catch (RuntimeException ex) {
            if (!ClusterLockService.isDuplicate(ex)) {
                throw ex;
            }
            transaction.accept(() -> {
                MetricAggregationEntry existing = findExisting(entryManager, entry.getDn());
                if (existing == null) {
                    throw ex;
                }
                mergeInto(entryManager, existing, entry);
            });
        }
    }

    /**
     * @return the stored aggregate, or {@code null} when it does not exist
     * @throws EntryPersistenceException the read failed for another reason
     */
    private static MetricAggregationEntry findExisting(PersistenceEntryManager entryManager, String dn) {
        try {
            return entryManager.find(MetricAggregationEntry.class, dn);
        } catch (EntryPersistenceException ex) {
            if (isMissingEntry(ex)) {
                return null;
            }
            throw ex;
        }
    }

    /**
     * SQL and LDAP report a missing entry either as a {@code null} result or as
     * {@link EntryPersistenceException} caused by a {@link SearchException} that has no cause of its own (or
     * with no cause at all). A failed query has a deeper cause.
     */
    static boolean isMissingEntry(EntryPersistenceException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return false;
        }

        return (cause instanceof SearchException) && (cause.getCause() == null);
    }

    private static void mergeInto(PersistenceEntryManager entryManager, MetricAggregationEntry existing, MetricAggregationEntry entry) {
        existing.setAggregationType(entry.getAggregationType());
        existing.setStartDate(entry.getStartDate());
        existing.setEndDate(entry.getEndDate());
        existing.setApplicationType(entry.getApplicationType());
        existing.setMetricType(entry.getMetricType());
        existing.setMetricSubType(entry.getMetricSubType());
        existing.setData(entry.getData());
        existing.setCreationDate(entry.getCreationDate());
        existing.setNodeIdentifier(entry.getNodeIdentifier());
        existing.setExpirationDate(entry.getExpirationDate());
        existing.setTtl(entry.getTtl());
        existing.setDeletable(entry.isDeletable());
        entryManager.merge(existing);
    }

    private void saveWatermarks(RunContext context) {
        if (context.saved.equals(context.watermarks) && context.savedCoverageStarts.equals(context.coverageStarts)) {
            return;
        }

        Map<MetricAggregationType, Instant> merged = new EnumMap<>(context.watermarks);
        Map<MetricAggregationType, Instant> starts = new EnumMap<>(context.coverageStarts);
        // The state is read under a row lock in the same transaction as the claim guard and write.
        // A read/parse failure aborts the transaction; a concurrent saver cannot regress watermarks.
        if (context.lockService.saveState(context.claim, json -> {
            Map<MetricAggregationType, Instant> current = parseState(json);
            for (Map.Entry<MetricAggregationType, Instant> entry : current.entrySet()) {
                Instant mine = merged.get(entry.getKey());
                if (mine == null || entry.getValue().isAfter(mine)) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
            parseCoverageStarts(json).forEach((level, start) ->
                    starts.merge(level, start, (mine, stored) -> mine.isAfter(stored) ? mine : stored));
            Map<String, Object> state = new LinkedHashMap<>();
            for (MetricAggregationType level : LEVELS) {
                if (merged.containsKey(level)) {
                    state.put(level.getValue(), merged.get(level).toString());
                }
            }
            state.put("coverageStarts", readObject(toStateJson(starts)));
            return toJson(state);
        }, true)) {
            context.watermarks.putAll(merged);
            context.saved.clear();
            context.saved.putAll(merged);
            context.coverageStarts.putAll(starts);
            context.savedCoverageStarts.clear();
            context.savedCoverageStarts.putAll(starts);
        } else {
            LOG.debug("Failed to save the state of task '{}'", context.task);
        }
    }

    private boolean releaseClaim(ClusterLockService lockService, ClusterLock claim, String status, String summary) {
        for (int attempt = 0; attempt < 2; attempt++) {
            if (lockService.release(claim, status, summary) || lockService.isReleaseConfirmed(claim, status, summary)) {
                return true;
            }
            if (attempt == 0 && !lockService.heartbeat(claim)) {
                break;
            }
        }
        return false;
    }

    private MetricAggregationConfiguration snapshotConfiguration() {
        MetricAggregationConfiguration source = getConfiguration();
        MetricAggregationConfiguration result = new MetricAggregationConfiguration();
        result.setEnabled(source.isEnabled());
        result.setRunIntervalSeconds(source.getRunIntervalSeconds());
        result.setMaxBucketsPerRun(source.getMaxBucketsPerRun());
        result.setRecomputeHourly(source.getRecomputeHourly());
        result.setRecomputeDaily(source.getRecomputeDaily());
        result.setRecomputeWeekly(source.getRecomputeWeekly());
        result.setRecomputeMonthly(source.getRecomputeMonthly());
        result.setKeepHourlyDays(source.getKeepHourlyDays());
        result.setKeepDailyDays(source.getKeepDailyDays());
        result.setKeepWeeklyDays(source.getKeepWeeklyDays());
        result.setKeepMonthlyDays(source.getKeepMonthlyDays());
        result.setLeaseSeconds(source.getLeaseSeconds());
        return result;
    }

    private void logRun(String applicationType, RunContext context, MetricAggregationRunResult result) {
        int failed = result.getTotalFailedBuckets();
        LOG.info("Metric aggregation for '{}' slot {}: buckets HOURLY={} DAILY={} WEEKLY={} MONTHLY={}, rows written {}, failed buckets {}, {} ms",
                applicationType, result.getSlot(), result.getBuckets(MetricAggregationType.HOURLY), result.getBuckets(MetricAggregationType.DAILY),
                result.getBuckets(MetricAggregationType.WEEKLY), result.getBuckets(MetricAggregationType.MONTHLY), result.getRowsWritten(),
                failed, result.getDurationMillis());
        if ((failed > 0) && shouldWarn("bucket-failure:" + applicationType)) {
            LOG.warn("Metric aggregation for '{}' has failed buckets; they are retried on the next run. First failure: {}", applicationType,
                    context.firstFailure);
        }
    }

    private static int getRecompute(MetricAggregationType level, MetricAggregationConfiguration configuration) {
        switch (level) {
        case HOURLY:
            return configuration.getRecomputeHourly();
        case DAILY:
            return configuration.getRecomputeDaily();
        case WEEKLY:
            return configuration.getRecomputeWeekly();
        case MONTHLY:
            return configuration.getRecomputeMonthly();
        default:
            throw new IllegalArgumentException("Unsupported aggregation type: " + level);
        }
    }

    /**
     * @return days that the source rows of the level are kept: raw rows for HOURLY, HOURLY rows for DAILY,
     *         DAILY rows for WEEKLY and MONTHLY. Buckets older than that are not planned.
     */
    private int getSourceKeepDays(MetricAggregationType level, MetricAggregationConfiguration configuration) {
        switch (level) {
        case HOURLY:
            return getRawKeepDays();
        case DAILY:
            return configuration.getKeepHourlyDays();
        case WEEKLY:
        case MONTHLY:
            return configuration.getKeepDailyDays();
        default:
            throw new IllegalArgumentException("Unsupported aggregation type: " + level);
        }
    }

    /**
     * Keeps the last row (by {@code jansEndDate}, then ID) of each series.
     */
    private static void remember(Map<String, MetricDataEntry> carry, List<MetricDataEntry> rows) {
        for (MetricDataEntry row : rows) {
            if (!MetricAggregator.isReadableRaw(row)) {
                continue;
            }
            carry.merge(seriesKey(row), row, (current, candidate) -> isAfter(candidate, current) ? candidate : current);
        }
    }

    private static boolean isAfter(MetricDataEntry candidate, MetricDataEntry current) {
        int result = candidate.getEndDate().compareTo(current.getEndDate());
        if (result == 0) {
            result = StringUtils.compare(candidate.getId(), current.getId());
        }
        return result > 0;
    }

    private static String seriesKey(MetricDataEntry row) {
        return StringUtils.defaultString(row.getNodeIdentifier()) + SERIES_SEPARATOR + StringUtils.defaultString(row.getApplicationType())
                + SERIES_SEPARATOR + StringUtils.defaultString(row.getMetricType()) + SERIES_SEPARATOR
                + StringUtils.defaultString(row.getMetricSubType());
    }

    private static JsonNode readObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            return (node != null) && node.isObject() ? node : null;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /**
     * Reads the watermarks of the task state: {@code {"HOURLY":"<ISO instant>", ...}}. Unknown keys and
     * unreadable values are ignored.
     */
    static Map<MetricAggregationType, Instant> parseState(String json) {
        Map<MetricAggregationType, Instant> result = new EnumMap<>(MetricAggregationType.class);
        JsonNode node = readObject(json);
        if (node == null) {
            if (StringUtils.isNotBlank(json)) {
                LOG.debug("Ignoring unreadable metric aggregation state");
            }
            return result;
        }

        for (MetricAggregationType level : LEVELS) {
            JsonNode value = node.get(level.getValue());
            if ((value == null) || !value.isTextual()) {
                continue;
            }
            try {
                result.put(level, Instant.parse(value.asText()));
            } catch (DateTimeParseException ex) {
                LOG.debug("Ignoring unreadable {} watermark in metric aggregation state", level);
            }
        }

        return result;
    }

    private static Map<MetricAggregationType, Instant> parseCoverageStarts(String json) {
        JsonNode state = readObject(json);
        JsonNode starts = state == null ? null : state.get("coverageStarts");
        return parseState(starts == null ? null : starts.toString());
    }

    static String toStateJson(Map<MetricAggregationType, Instant> watermarks) {
        Map<String, Object> state = new LinkedHashMap<>();
        for (MetricAggregationType level : LEVELS) {
            Instant watermark = watermarks.get(level);
            if (watermark != null) {
                state.put(level.getValue(), watermark.toString());
            }
        }

        return toJson(state);
    }

    private static String stateSaveFailureSummary(MetricAggregationRunResult result) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(SUMMARY_SLOT, result.getSlot());
        summary.put(SUMMARY_ERROR, "state-save");
        summary.put(SUMMARY_ROWS, result.getRowsWritten());
        return toJson(summary);
    }

    private static String doneSummary(MetricAggregationRunResult result) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(SUMMARY_SLOT, result.getSlot());
        for (MetricAggregationType level : LEVELS) {
            summary.put(level.getValue(), result.getBuckets(level));
        }
        summary.put(SUMMARY_ROWS, result.getRowsWritten());
        summary.put(SUMMARY_FAILED, result.getTotalFailedBuckets());
        summary.put(SUMMARY_DURATION, result.getDurationMillis());

        return toJson(summary);
    }

    private static String errorSummary(long slot, String error) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(SUMMARY_SLOT, slot);
        summary.put(SUMMARY_ERROR, error);

        return toJson(summary);
    }

    private static String toJson(Map<String, Object> value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize metric aggregation data", ex);
        }
    }

    private boolean shouldWarn(String key) {
        Instant now = Instant.now();
        boolean[] warn = new boolean[1];
        lastWarnings.compute(key, (k, last) -> {
            if ((last == null) || !now.isBefore(last.plus(WARNING_INTERVAL))) {
                warn[0] = true;
                return now;
            }
            return last;
        });

        return warn[0];
    }

    /**
     * State of one run.
     */
    private final class RunContext {

        private final ClusterLockService lockService;
        private final MetricAggregationConfiguration configuration;
        private final ClusterLock claim;
        private final String task;
        private final String applicationType;
        private final Instant now;
        private final Date creationDate;
        private final Duration grace;
        private final PersistenceEntryManager entryManager;
        private final String metricBaseDn;
        private final MetricAggregator aggregator;

        private final Map<MetricAggregationType, Instant> watermarks = new EnumMap<>(MetricAggregationType.class);
        private final Map<MetricAggregationType, Instant> saved = new EnumMap<>(MetricAggregationType.class);
        private final Map<MetricAggregationType, Instant> coverageStarts = new EnumMap<>(MetricAggregationType.class);
        private final Map<MetricAggregationType, Instant> savedCoverageStarts = new EnumMap<>(MetricAggregationType.class);
        private final Map<MetricAggregationType, Integer> buckets = new EnumMap<>(MetricAggregationType.class);
        private final Map<MetricAggregationType, Integer> failedBuckets = new EnumMap<>(MetricAggregationType.class);
        private final Map<String, MetricDataEntry> carry = new HashMap<>();

        private int rowsWritten;
        private boolean carryValid;
        private boolean lost;
        private String firstFailure;

        private RunContext(MetricService metricService, ClusterLockService lockService, MetricAggregationConfiguration configuration,
                ClusterLock claim, String task, String applicationType, Instant now) {
            this.lockService = lockService;
            this.configuration = configuration;
            this.claim = claim;
            this.task = task;
            this.applicationType = applicationType;
            this.now = now;
            this.creationDate = Date.from(now);
            this.grace = PeriodMath.grace(getReporterIntervalSeconds());
            this.entryManager = metricService.getEntryManager();
            this.metricBaseDn = metricService.baseDn();
            this.aggregator = new MetricAggregator(metricBaseDn, metricService.getNodeIdentifier(), configuration);
        }
    }

}
