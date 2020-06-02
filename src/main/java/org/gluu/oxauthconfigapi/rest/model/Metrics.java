package org.gluu.oxauthconfigapi.rest.model;


import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

/**
 * @author Puja Sharma
 *
 */
public class Metrics implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Boolean value specifying whether to enable Metric Reporter
	 */
	private Boolean metricReporterEnabled;
	
	/**
	 * The days to keep metric reported data
	 */
	@Positive
	@Min(value=1)
	private Integer metricReporterKeepDataDays;
	
	/**
	 * The interval for metric reporter in seconds
	 */
	@Positive
	@Min(value=1)
	private Integer metricReporterInterval;
	
	
	public Boolean getMetricReporterEnabled() {
		return metricReporterEnabled;
	}
	
	public void setMetricReporterEnabled(Boolean metricReporterEnabled) {
		this.metricReporterEnabled = metricReporterEnabled;
	}
	
	public Integer getMetricReporterKeepDataDays() {
		return metricReporterKeepDataDays;
	}
	
	public void setMetricReporterKeepDataDays(Integer metricReporterKeepDataDays) {
		this.metricReporterKeepDataDays = metricReporterKeepDataDays;
	}
	
	public Integer getMetricReporterInterval() {
		return metricReporterInterval;
	}
	
	public void setMetricReporterInterval(Integer metricReporterInterval) {
		this.metricReporterInterval = metricReporterInterval;
	}
	
}
