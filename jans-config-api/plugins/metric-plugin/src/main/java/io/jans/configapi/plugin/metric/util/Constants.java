/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.util;

public class Constants {

    private Constants() {}

    public static final String TYPES = "/types";
    public static final String APP_TYPES = "/app-types";
    public static final String ENTRIES = "/entries";
    public static final String AGGREGATIONS = "/aggregations";

    public static final String APP_TYPE = "appType";
    public static final String METRIC_TYPE = "metricType";
    public static final String SUB_TYPE = "subType";
    public static final String AGGREGATION_TYPE = "aggregationType";
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";

    // Shared by @Parameter annotations, so these must stay compile-time constants.
    public static final String START_DATE_DESCRIPTION = "Start date/time. Accepted formats: ISO-8601 date-time "
            + "(e.g. 2026-08-01T00:00:00Z), ISO-8601 date (e.g. 2026-08-01) or legacy dd-MM-yyyy; a date means "
            + "00:00:00.000 UTC of that day. Only entries with jansStartDate on or after this instant match.";
    public static final String END_DATE_DESCRIPTION = "End date/time, inclusive. Same formats as start_date. A date-time "
            + "without a fractional second (e.g. 2026-08-01T23:59:59Z) is read as the end of that second "
            + "(23:59:59.999), and a date (e.g. 2026-08-01) as the end of that day (23:59:59.999 UTC), because "
            + "entries end 1 ms before a period boundary. A date-time with a non-zero fractional second is used as "
            + "given. Only entries with jansEndDate on or before this instant match.";
    public static final String CONTAINMENT_NOTE = " The date range is a containment filter: an entry matches only "
            + "when it lies completely inside the range (jansStartDate >= start_date AND jansEndDate <= end_date). "
            + "An entry that starts before start_date or ends after end_date is not returned, so a request for a "
            + "partial period does not return that period's entry.";

    public static final String METRIC_READ_ACCESS = "https://jans.io/oauth/config/metric.readonly";

}
