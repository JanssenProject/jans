/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.metric.timer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.gluu.model.metric.MetricData;

/**
 * Timer metric data class
 *
 * @author Yuriy Movchan Date: 07/30/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimerMetricData extends MetricData {

    private static final long serialVersionUID = -2322501012136295255L;

    private long count;

    private double meanRate;
    private double oneMinuteRate, fiveMinuteRate, fifteenMinuteRate;
    private String rateUnit;

    private double min, max, mean, stdDev, median;
    private double value75thPercentile, value95thPercentile, value98thPercentile, value99thPercentile, value999thPercentile;
    private String durationUnit;

    public TimerMetricData() {
    }

    public TimerMetricData(long count, double meanRate, double oneMinuteRate, double fiveMinuteRate, double fifteenMinuteRate, String rateUnit,
            double min, double max, double mean, double stdDev, double median, double value75thPercentile, double value95thPercentile,
            double value98thPercentile, double value99thPercentile, double value999thPercentile, String durationUnit) {
        this.count = count;
        this.meanRate = meanRate;
        this.oneMinuteRate = oneMinuteRate;
        this.fiveMinuteRate = fiveMinuteRate;
        this.fifteenMinuteRate = fifteenMinuteRate;
        this.rateUnit = rateUnit;
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stdDev = stdDev;
        this.median = median;
        this.value75thPercentile = value75thPercentile;
        this.value95thPercentile = value95thPercentile;
        this.value98thPercentile = value98thPercentile;
        this.value99thPercentile = value99thPercentile;
        this.value999thPercentile = value999thPercentile;
        this.durationUnit = durationUnit;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getMeanRate() {
        return meanRate;
    }

    public void setMeanRate(double meanRate) {
        this.meanRate = meanRate;
    }

    public double getOneMinuteRate() {
        return oneMinuteRate;
    }

    public void setOneMinuteRate(double oneMinuteRate) {
        this.oneMinuteRate = oneMinuteRate;
    }

    public double getFiveMinuteRate() {
        return fiveMinuteRate;
    }

    public void setFiveMinuteRate(double fiveMinuteRate) {
        this.fiveMinuteRate = fiveMinuteRate;
    }

    public double getFifteenMinuteRate() {
        return fifteenMinuteRate;
    }

    public void setFifteenMinuteRate(double fifteenMinuteRate) {
        this.fifteenMinuteRate = fifteenMinuteRate;
    }

    public String getRateUnit() {
        return rateUnit;
    }

    public void setRateUnit(String rateUnit) {
        this.rateUnit = rateUnit;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public void setStdDev(double stdDev) {
        this.stdDev = stdDev;
    }

    public double getMedian() {
        return median;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public double getValue75thPercentile() {
        return value75thPercentile;
    }

    public void setValue75thPercentile(double value75thPercentile) {
        this.value75thPercentile = value75thPercentile;
    }

    public double getValue95thPercentile() {
        return value95thPercentile;
    }

    public void setValue95thPercentile(double value95thPercentile) {
        this.value95thPercentile = value95thPercentile;
    }

    public double getValue98thPercentile() {
        return value98thPercentile;
    }

    public void setValue98thPercentile(double value98thPercentile) {
        this.value98thPercentile = value98thPercentile;
    }

    public double getValue99thPercentile() {
        return value99thPercentile;
    }

    public void setValue99thPercentile(double value99thPercentile) {
        this.value99thPercentile = value99thPercentile;
    }

    public double getValue999thPercentile() {
        return value999thPercentile;
    }

    public void setValue999thPercentile(double value999thPercentile) {
        this.value999thPercentile = value999thPercentile;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TimerMetricData [count=").append(count).append(", meanRate=").append(meanRate).append(", oneMinuteRate=")
                .append(oneMinuteRate).append(", fiveMinuteRate=").append(fiveMinuteRate).append(", fifteenMinuteRate=").append(fifteenMinuteRate)
                .append(", rateUnit=").append(rateUnit).append(", min=").append(min).append(", max=").append(max).append(", mean=").append(mean)
                .append(", stdDev=").append(stdDev).append(", median=").append(median).append(", value75thPercentile=").append(value75thPercentile)
                .append(", value95thPercentile=").append(value95thPercentile).append(", value98thPercentile=").append(value98thPercentile)
                .append(", value99thPercentile=").append(value99thPercentile).append(", value999thPercentile=").append(value999thPercentile)
                .append(", durationUnit=").append(durationUnit).append("]");
        return builder.toString();
    }

}
