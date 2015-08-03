/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.metric.timer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.xdi.model.metric.MetricData;

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

	private double min, max;
	private String durationUnit;

	public TimerMetricData() {
	}

	public TimerMetricData(long count, double meanRate, double oneMinuteRate, double fiveMinuteRate, double fifteenMinuteRate, String rateUnit, double min, double max, String durationUnit) {
		this.count = count;

		this.meanRate = meanRate;
		this.oneMinuteRate = oneMinuteRate;
		this.fiveMinuteRate = fiveMinuteRate;
		this.fifteenMinuteRate = fifteenMinuteRate;
		this.rateUnit = rateUnit;

		this.min = min;
		this.max = max;
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

	public String getDurationUnit() {
		return durationUnit;
	}

	public void setDurationUnit(String durationUnit) {
		this.durationUnit = durationUnit;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimerMetricData [count=").append(count).append(", meanRate=").append(meanRate).append(", oneMinuteRate=").append(oneMinuteRate)
				.append(", fiveMinuteRate=").append(fiveMinuteRate).append(", fifteenMinuteRate=").append(fifteenMinuteRate).append(", rateUnit=")
				.append(rateUnit).append(", min=").append(min).append(", max=").append(max).append(", durationUnit=").append(durationUnit).append("]");
		return builder.toString();
	}

}
