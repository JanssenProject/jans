/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.util;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.util.ApiConstants;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

@ApplicationScoped
public class MetricUtil {

    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    @ConfigProperty(name = "metric.types.scan.limit", defaultValue = "100000")
    int metricTypesScanLimit;

    public int getRecordMaxCount() {
        int maxCount = configurationFactory.getApiAppConfiguration().getMaxCount();
        logger.trace("Metric maxCount from ApiAppConfiguration:{}, DEFAULT_MAX_COUNT:{}", maxCount,
                ApiConstants.DEFAULT_MAX_COUNT);
        return maxCount > 0 ? maxCount : ApiConstants.DEFAULT_MAX_COUNT;
    }

    /**
     * Upper bound on how many jansMetric rows the /types and /app-types discovery scans read, since
     * the ORM has no server-side DISTINCT. Narrowing with appType/start_date/end_date keeps the scan
     * cheap; this limit only guards against an unbounded worst case.
     */
    public int getMetricTypesScanLimit() {
        return metricTypesScanLimit;
    }

}
