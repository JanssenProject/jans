/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@code jansData} of a timer aggregation row.
 *
 * <ul>
 * <li>{@code count}: number of timed events in the period.</li>
 * <li>{@code rate}: {@code count} divided by the period length in seconds; {@code rateUnit} is
 * {@code second}.</li>
 * <li>{@code min} and {@code max}: estimates from the smallest source reservoir minimum and the
 * largest source reservoir maximum. Reservoirs can include events from before the reporting interval,
 * so these are not exact period extrema. Child rollups preserve this limitation.</li>
 * <li>{@code mean}, {@code median} and the {@code value*Percentile} values: averages of the source
 * values weighted by the number of events of each source.</li>
 * <li>{@code approximate}: always {@code true} for generated aggregates and applies to all duration
 * statistics, including {@code min} and {@code max}. Interval-local duration samples are not available
 * in the source rows, and percentiles cannot be merged exactly.</li>
 * </ul>
 *
 * <p>Only sources with at least one event contribute to the duration statistics, because the reservoir
 * values of a source without events describe older events. When the period has no events, the duration
 * statistics are {@code null}. See {@link AggregateData} for the other properties.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "kind", "count", "rate", "rateUnit", "min", "max", "mean", "median", "value75thPercentile",
        "value95thPercentile", "value98thPercentile", "value99thPercentile", "value999thPercentile", "durationUnit",
        "approximate", "sourceRows", "nodes", "complete", "legacyDelta" })
public class TimerAggregateData extends AggregateData {

    private static final long serialVersionUID = -6230177019455380512L;

    public static final String KIND = "timer";

    public static final String RATE_UNIT_SECOND = "second";

    private double rate;
    private String rateUnit = RATE_UNIT_SECOND;

    private Double min;
    private Double max;
    private Double mean;
    private Double median;
    private Double value75thPercentile;
    private Double value95thPercentile;
    private Double value98thPercentile;
    private Double value99thPercentile;
    private Double value999thPercentile;
    private String durationUnit;

    private boolean approximate = true;

    @Override
    public String getKind() {
        return KIND;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getRateUnit() {
        return rateUnit;
    }

    public void setRateUnit(String rateUnit) {
        this.rateUnit = rateUnit;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public Double getValue75thPercentile() {
        return value75thPercentile;
    }

    public void setValue75thPercentile(Double value75thPercentile) {
        this.value75thPercentile = value75thPercentile;
    }

    public Double getValue95thPercentile() {
        return value95thPercentile;
    }

    public void setValue95thPercentile(Double value95thPercentile) {
        this.value95thPercentile = value95thPercentile;
    }

    public Double getValue98thPercentile() {
        return value98thPercentile;
    }

    public void setValue98thPercentile(Double value98thPercentile) {
        this.value98thPercentile = value98thPercentile;
    }

    public Double getValue99thPercentile() {
        return value99thPercentile;
    }

    public void setValue99thPercentile(Double value99thPercentile) {
        this.value99thPercentile = value99thPercentile;
    }

    public Double getValue999thPercentile() {
        return value999thPercentile;
    }

    public void setValue999thPercentile(Double value999thPercentile) {
        this.value999thPercentile = value999thPercentile;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    public boolean isApproximate() {
        return approximate;
    }

    public void setApproximate(boolean approximate) {
        this.approximate = approximate;
    }

}
