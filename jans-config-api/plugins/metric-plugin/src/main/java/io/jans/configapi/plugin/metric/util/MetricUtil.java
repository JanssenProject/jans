/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.util;

import static io.jans.configapi.core.rest.BaseResource.throwBadRequestException;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.util.ApiConstants;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Set;

/**
 * Query-parameter validation and configuration shared by {@code MetricResource} and
 * {@code MetricAggregationResource}, so the two endpoints can't drift apart on what counts as a
 * valid sortBy/date/pagination value.
 */
@ApplicationScoped
public class MetricUtil {

    // jansMetric/jansMetricAggregation attributes with an index that makes them safe to sort by; a
    // caller-supplied sortBy outside this set is rejected rather than passed through to the
    // persistence layer.
    public static final Set<String> SORTABLE_ATTRIBUTES = Set.of("jansStartDate", "jansEndDate", "creationDate");
    public static final String DEFAULT_SORT_BY = "jansStartDate";

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

    public void validateSortBy(String sortBy) {
        if (StringUtils.isNotBlank(sortBy) && !SORTABLE_ATTRIBUTES.contains(sortBy)) {
            throwBadRequestException("Invalid sortBy '" + sortBy + "'. Allowed values: " + SORTABLE_ATTRIBUTES,
                    "INVALID_SORT_BY");
        }
    }

    public String normalizeSortOrder(String sortOrder) {
        return ApiConstants.ASCENDING.equals(sortOrder) ? ApiConstants.ASCENDING : ApiConstants.DESCENDING;
    }

    /**
     * Rejects negative paging inputs before they reach the persistence layer, where a negative
     * startIndex/count would otherwise surface as an internal error instead of a 400. limit is also
     * clamped to the configured max record count rather than rejected, since an oversized value is a
     * request for "as much as allowed", not a malformed one.
     */
    public int validatePagination(int limit, int startIndex) {
        if (limit < 0 || startIndex < 0) {
            throwBadRequestException("limit and startIndex must not be negative.", "INVALID_PAGINATION");
        }
        int maxCount = getRecordMaxCount();
        return limit > maxCount ? maxCount : limit;
    }

    public Date parseDateParam(String value, String paramName) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            LocalDateTime parsed = MetricDateUtil.parseDateTime(value);
            return MetricDateUtil.toDate(parsed);
        } catch (DateTimeParseException dtpe) {
            throwBadRequestException(
                    "Invalid '" + paramName + "' value '" + value + "'. " + MetricDateUtil.ACCEPTED_FORMATS_MESSAGE,
                    "INVALID_DATE");
            return null;
        }
    }

    public void validateRange(Date start, Date end) {
        if (start != null && end != null && start.after(end)) {
            throwBadRequestException("start_date must be before or equal to end_date.", "INVALID_DATE_RANGE");
        }
    }

}
