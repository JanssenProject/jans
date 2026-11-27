/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Configuration of the metric aggregation job that builds HOURLY, DAILY, WEEKLY and MONTHLY rows in
 * {@code jansMetricAggregation} from raw {@code jansMetric} rows.
 *
 * <p>Every property is optional. When a property is missing, or a numeric property is 0 or less, its
 * getter returns the default value, so a stored application configuration without this block needs
 * no migration. Aggregation is enabled by default.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricAggregationConfiguration implements Serializable {

    private static final long serialVersionUID = 6403146587224370731L;

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_RUN_INTERVAL_SECONDS = 300;
    public static final int DEFAULT_MAX_BUCKETS_PER_RUN = 48;
    public static final int DEFAULT_RECOMPUTE_HOURLY = 2;
    public static final int DEFAULT_RECOMPUTE_DAILY = 1;
    public static final int DEFAULT_RECOMPUTE_WEEKLY = 1;
    public static final int DEFAULT_RECOMPUTE_MONTHLY = 1;
    public static final int DEFAULT_KEEP_HOURLY_DAYS = 90;
    public static final int DEFAULT_KEEP_DAILY_DAYS = 730;
    public static final int DEFAULT_KEEP_WEEKLY_DAYS = 1825;
    public static final int DEFAULT_KEEP_MONTHLY_DAYS = 3650;
    public static final int DEFAULT_LEASE_SECONDS = 600;

    private Boolean enabled;
    private int runIntervalSeconds;
    private int maxBucketsPerRun;
    private int recomputeHourly;
    private int recomputeDaily;
    private int recomputeWeekly;
    private int recomputeMonthly;
    private int keepHourlyDays;
    private int keepDailyDays;
    private int keepWeeklyDays;
    private int keepMonthlyDays;
    private int leaseSeconds;

    /**
     * @return {@code false} only when the property is set to {@code false}; {@code true} when it is
     *         missing or {@code true}
     */
    public boolean isEnabled() {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return seconds between two runs of the aggregation job on each node
     */
    public int getRunIntervalSeconds() {
        return positiveOrDefault(runIntervalSeconds, DEFAULT_RUN_INTERVAL_SECONDS);
    }

    public void setRunIntervalSeconds(int runIntervalSeconds) {
        this.runIntervalSeconds = runIntervalSeconds;
    }

    /**
     * @return maximum number of buckets of one level that one run computes
     */
    public int getMaxBucketsPerRun() {
        return positiveOrDefault(maxBucketsPerRun, DEFAULT_MAX_BUCKETS_PER_RUN);
    }

    public void setMaxBucketsPerRun(int maxBucketsPerRun) {
        this.maxBucketsPerRun = maxBucketsPerRun;
    }

    /**
     * @return number of the latest closed HOURLY buckets that each run computes again
     */
    public int getRecomputeHourly() {
        return positiveOrDefault(recomputeHourly, DEFAULT_RECOMPUTE_HOURLY);
    }

    public void setRecomputeHourly(int recomputeHourly) {
        this.recomputeHourly = recomputeHourly;
    }

    /**
     * @return number of the latest closed DAILY buckets that each run computes again
     */
    public int getRecomputeDaily() {
        return positiveOrDefault(recomputeDaily, DEFAULT_RECOMPUTE_DAILY);
    }

    public void setRecomputeDaily(int recomputeDaily) {
        this.recomputeDaily = recomputeDaily;
    }

    /**
     * @return number of the latest closed WEEKLY buckets that each run computes again
     */
    public int getRecomputeWeekly() {
        return positiveOrDefault(recomputeWeekly, DEFAULT_RECOMPUTE_WEEKLY);
    }

    public void setRecomputeWeekly(int recomputeWeekly) {
        this.recomputeWeekly = recomputeWeekly;
    }

    /**
     * @return number of the latest closed MONTHLY buckets that each run computes again
     */
    public int getRecomputeMonthly() {
        return positiveOrDefault(recomputeMonthly, DEFAULT_RECOMPUTE_MONTHLY);
    }

    public void setRecomputeMonthly(int recomputeMonthly) {
        this.recomputeMonthly = recomputeMonthly;
    }

    /**
     * @return days that HOURLY aggregation rows are kept
     */
    public int getKeepHourlyDays() {
        return positiveOrDefault(keepHourlyDays, DEFAULT_KEEP_HOURLY_DAYS);
    }

    public void setKeepHourlyDays(int keepHourlyDays) {
        this.keepHourlyDays = keepHourlyDays;
    }

    /**
     * @return days that DAILY aggregation rows are kept
     */
    public int getKeepDailyDays() {
        return positiveOrDefault(keepDailyDays, DEFAULT_KEEP_DAILY_DAYS);
    }

    public void setKeepDailyDays(int keepDailyDays) {
        this.keepDailyDays = keepDailyDays;
    }

    /**
     * @return days that WEEKLY aggregation rows are kept
     */
    public int getKeepWeeklyDays() {
        return positiveOrDefault(keepWeeklyDays, DEFAULT_KEEP_WEEKLY_DAYS);
    }

    public void setKeepWeeklyDays(int keepWeeklyDays) {
        this.keepWeeklyDays = keepWeeklyDays;
    }

    /**
     * @return days that MONTHLY aggregation rows are kept
     */
    public int getKeepMonthlyDays() {
        return positiveOrDefault(keepMonthlyDays, DEFAULT_KEEP_MONTHLY_DAYS);
    }

    public void setKeepMonthlyDays(int keepMonthlyDays) {
        this.keepMonthlyDays = keepMonthlyDays;
    }

    /**
     * @return seconds after the last heartbeat for which a running aggregation claim is treated as alive
     */
    public int getLeaseSeconds() {
        return positiveOrDefault(leaseSeconds, DEFAULT_LEASE_SECONDS);
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    @Override
    public String toString() {
        return "MetricAggregationConfiguration [enabled=" + isEnabled() + ", runIntervalSeconds=" + getRunIntervalSeconds()
                + ", maxBucketsPerRun=" + getMaxBucketsPerRun() + ", recomputeHourly=" + getRecomputeHourly()
                + ", recomputeDaily=" + getRecomputeDaily() + ", recomputeWeekly=" + getRecomputeWeekly()
                + ", recomputeMonthly=" + getRecomputeMonthly() + ", keepHourlyDays=" + getKeepHourlyDays()
                + ", keepDailyDays=" + getKeepDailyDays() + ", keepWeeklyDays=" + getKeepWeeklyDays()
                + ", keepMonthlyDays=" + getKeepMonthlyDays() + ", leaseSeconds=" + getLeaseSeconds() + "]";
    }

}
