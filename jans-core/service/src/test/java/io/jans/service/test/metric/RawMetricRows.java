/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.metric;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.model.metric.ldap.OrderedRawEntry;

/**
 * Builds raw {@code jansMetric} rows the way {@code PersistenceEntryReporter} writes them: the end date is
 * the report time minus 1 ms.
 */
public final class RawMetricRows {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneOffset.UTC);

    private RawMetricRows() {
    }

    /**
     * @param reportTime report time; the row covers {@code [reportTime - interval, reportTime - 1 ms]}
     */
    public static MetricDataEntry row(String metricBaseDn, String applicationType, String host, String metricType, String subType,
            Instant reportTime, Duration interval, String data, Date expirationDate) {
        String id = UUID.nameUUIDFromBytes((applicationType + "|" + host + "|" + metricType + "|" + subType + "|" + reportTime.toEpochMilli())
                .getBytes(StandardCharsets.UTF_8)).toString();
        MetricDataEntry entry = new OrderedRawEntry();
        entry.setId(id);
        entry.setDn("uniqueIdentifier=" + id + ",ou=" + PERIOD.format(reportTime) + ",ou=" + applicationType + "," + metricBaseDn);
        entry.setApplicationType(applicationType);
        entry.setMetricType(metricType);
        entry.setMetricSubType(subType);
        entry.setNodeIdentifier(host);
        entry.setStartDate(Date.from(reportTime.minus(interval)));
        entry.setEndDate(Date.from(reportTime.minusMillis(1)));
        entry.setCreationDate(Date.from(reportTime));
        entry.setData(data);
        entry.setExpirationDate(expirationDate);
        entry.setDeletable(true);
        return entry;
    }

    public static String counter(long count, long intervalCount) {
        return "{\"count\":" + count + ",\"intervalCount\":" + intervalCount + "}";
    }

    public static String legacyCounter(long count) {
        return "{\"count\":" + count + "}";
    }

    public static String timer(long count, long intervalCount, double min, double max, double mean) {
        return timer(count, ",\"intervalCount\":" + intervalCount, min, max, mean);
    }

    public static String legacyTimer(long count, double min, double max, double mean) {
        return timer(count, "", min, max, mean);
    }

    private static String timer(long count, String intervalCount, double min, double max, double mean) {
        return "{\"count\":" + count + intervalCount + ",\"meanRate\":0.1,\"oneMinuteRate\":0.1,\"fiveMinuteRate\":0.1,"
                + "\"fifteenMinuteRate\":0.1,\"rateUnit\":\"second\",\"min\":" + min + ",\"max\":" + max + ",\"mean\":" + mean
                + ",\"stdDev\":0.0,\"median\":" + mean + ",\"value75thPercentile\":" + mean + ",\"value95thPercentile\":" + max
                + ",\"value98thPercentile\":" + max + ",\"value99thPercentile\":" + max + ",\"value999thPercentile\":" + max
                + ",\"durationUnit\":\"milliseconds\"}";
    }

}
