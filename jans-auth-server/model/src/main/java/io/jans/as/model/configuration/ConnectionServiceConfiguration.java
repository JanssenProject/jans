/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.as.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;

/**
 * Connection Service configuration
 *
 * @author Yuriy Movchan Date: 05/10/2025
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionServiceConfiguration {

    @DocProperty(description = "Set the maximum number of total open connections", defaultValue = "200")
    private Integer maxTotal;

    @DocProperty(description = "Set the maximum number of concurrent connections per route", defaultValue = "50")
    private Integer maxPerRoute;

    @DocProperty(description = "Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer", defaultValue = "2000")
	private Integer validateAfterInactivity;

	public Integer getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(Integer maxTotal) {
		this.maxTotal = maxTotal;
	}

	public Integer getMaxPerRoute() {
		return maxPerRoute;
	}

	public void setMaxPerRoute(Integer maxPerRoute) {
		this.maxPerRoute = maxPerRoute;
	}

	public Integer getValidateAfterInactivity() {
		return validateAfterInactivity;
	}

	public void setValidateAfterInactivity(Integer validateAfterInactivity) {
		this.validateAfterInactivity = validateAfterInactivity;
	}

}