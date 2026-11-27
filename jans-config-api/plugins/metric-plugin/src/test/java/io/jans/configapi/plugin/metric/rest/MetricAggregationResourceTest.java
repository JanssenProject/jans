/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.rest;

import static org.testng.Assert.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import io.jans.configapi.plugin.metric.service.MetricAggregationService;
import io.jans.configapi.plugin.metric.util.MetricUtil;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import jakarta.ws.rs.core.Response;

public class MetricAggregationResourceTest {

    @Test
    public void dateOnlyEndReachesServiceAsLastMillisecondOfDay() {
        AtomicReference<Date> captured = new AtomicReference<>();
        MetricAggregationResource resource = new MetricAggregationResource();
        resource.logger = LoggerFactory.getLogger(getClass());
        resource.metricUtil = new MetricUtil() {
            @Override
            public int getRecordMaxCount() {
                return 100;
            }
        };
        resource.metricAggregationService = new MetricAggregationService() {
            @Override
            public PagedResult<MetricAggregationEntry> findEntries(MetricAggregationType type, String app, String metric,
                    String subtype, Date start, Date end, String sortBy, SortOrder order, int offset, int count, int maxCount) {
                captured.set(end);
                return new PagedResult<>();
            }
        };
        try (Response response = resource.getMetricAggregations("DAILY", null, null, null, "2026-02-01", "2026-02-20",
                10, 0, "jansStartDate", "ascending")) {
            assertEquals(response.getStatus(), 200);
            assertEquals(captured.get(), Date.from(Instant.parse("2026-02-20T23:59:59.999Z")));
        }
    }
}
