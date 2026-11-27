/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.metric.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.metric.service.MetricAggregationService;
import io.jans.configapi.plugin.metric.util.Constants;
import io.jans.configapi.plugin.metric.util.MetricUtil;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Read-only endpoint over jansMetricAggregation data: HOURLY, DAILY, WEEKLY and MONTHLY aggregates
 * that the application nodes compute from jansMetric rows. The response carries each aggregate's
 * jansData as a JSON object in the {@code data} field.
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@Path(Constants.AGGREGATIONS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MetricAggregationResource extends BaseResource {

    // Must be a compile-time constant to appear in the @Parameter annotation below - keep this in
    // sync by hand with io.jans.model.metric.MetricAggregationType's values.
    private static final String ALLOWED_AGGREGATION_TYPES = "HOURLY, DAILY, WEEKLY, MONTHLY";

    private static final String AGGREGATIONS_DESCRIPTION = "Returns a page of jansMetricAggregation entries for an "
            + "aggregation period (HOURLY, DAILY, WEEKLY, MONTHLY) within the given date range. Entries are computed "
            + "by the application nodes from the raw jansMetric rows, in UTC (weeks are ISO weeks starting Monday). "
            + "Aggregates are cluster-wide totals per application type, metric type and metric subtype. An entry "
            + "covers a whole period: jansStartDate is the first millisecond of the period and jansEndDate is the "
            + "last one (period end minus 1 ms, for example 2026-11-24T23:59:59.999Z for a DAILY entry)."
            + Constants.CONTAINMENT_NOTE
            + " The 'data' field of each entry is a JSON object. Counter: {\"kind\":\"counter\",\"count\":13,"
            + "\"sourceRows\":2,\"nodes\":2,\"complete\":true,\"legacyDelta\":false}. Timer: "
            + "{\"kind\":\"timer\",\"count\":40,\"rate\":0.0111,\"rateUnit\":\"second\",\"min\":1.0,"
            + "\"max\":40.0,\"mean\":8.0,\"median\":7.0,\"value75thPercentile\":9.0,\"value95thPercentile\":17.5,"
            + "\"value98thPercentile\":21.75,\"value99thPercentile\":26.25,\"value999thPercentile\":33.25,"
            + "\"durationUnit\":\"milliseconds\",\"approximate\":true,\"sourceRows\":2,\"nodes\":2,"
            + "\"complete\":false,\"legacyDelta\":false}. Fields: count is the number of events in the period "
            + "(for a timer, the number of timed calls), summed from per-interval deltas; sourceRows is the number "
            + "of raw rows the aggregate covers; nodes is the number of nodes that reported (in WEEKLY, MONTHLY and "
            + "DAILY entries it is a lower bound: the largest value among the child entries); complete is true when "
            + "the entry was computed after its period closed and its source data was complete, so false means the "
            + "period is still open or source data is incomplete, unreadable, expired or may still arrive; legacyDelta is true when some counts came from raw rows "
            + "written by an older version without a per-interval count, whose first row of a series counts as 0. "
            + "Timer fields: rate is count divided by the period length in seconds; min and max are estimates from "
            + "source reservoir extrema and may include older events; mean, median and the percentiles are weighted averages of the "
            + "per-row values. All duration statistics are approximate (approximate is always true); durationUnit is "
            + "milliseconds for min, max, mean, median and the percentiles. A timer with count 0 has null for min, max, mean, median and "
            + "the percentiles. Rows that arrive after the recompute window (by default the last 2 closed HOURLY periods "
            + "and the last closed DAILY, WEEKLY and MONTHLY period) are not counted. If 'data' does not hold "
            + "valid JSON, it is returned as a string.";

    @Inject
    Logger logger;

    @Inject
    MetricAggregationService metricAggregationService;

    @Inject
    MetricUtil metricUtil;

    private class MetricAggregationPagedResult extends PagedResult<MetricAggregationEntry> {
    }

    @Operation(summary = "Get aggregated metric entries within a time range.", description = AGGREGATIONS_DESCRIPTION, operationId = "get-metric-aggregations", tags = {
            "Metric" }, security = { @SecurityRequirement(name = "oauth2", scopes = { Constants.METRIC_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MetricAggregationPagedResult.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.METRIC_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getMetricAggregations(
            @Parameter(description = "Aggregation period. One of " + ALLOWED_AGGREGATION_TYPES + ".") @QueryParam(Constants.AGGREGATION_TYPE) @NotNull(message = "The attribute 'aggregationType' is required for this operation") String aggregationType,
            @Parameter(description = "Application type (jansAppTyp) to narrow the search, for example jans_auth or fido2.") @QueryParam(Constants.APP_TYPE) String appType,
            @Parameter(description = "Metric type (jansMetricTyp) to narrow the search.") @QueryParam(Constants.METRIC_TYPE) String metricType,
            @Parameter(description = "Metric subtype (jansMetricSubTyp) to narrow the search.") @QueryParam(Constants.SUB_TYPE) String subType,
            @Parameter(description = Constants.START_DATE_DESCRIPTION, schema = @Schema(type = "string")) @QueryParam(Constants.START_DATE) @NotNull(message = "The attribute 'start_date' is required for this operation") String startDate,
            @Parameter(description = Constants.END_DATE_DESCRIPTION, schema = @Schema(type = "string")) @QueryParam(Constants.END_DATE) @NotNull(message = "The attribute 'end_date' is required for this operation") String endDate,
            @Parameter(description = "Maximum number of results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute to sort by. One of jansStartDate, jansEndDate, creationDate.") @DefaultValue(MetricUtil.DEFAULT_SORT_BY) @QueryParam(ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Sort order - ascending or descending.") @DefaultValue(ApiConstants.DESCENDING) @QueryParam(ApiConstants.SORT_ORDER) String sortOrder) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Get metric aggregations - aggregationType:{}, appType:{}, metricType:{}, subType:{}, startDate:{}, endDate:{}, limit:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(aggregationType), escapeLog(appType), escapeLog(metricType), escapeLog(subType),
                    escapeLog(startDate), escapeLog(endDate), escapeLog(limit), escapeLog(startIndex),
                    escapeLog(sortBy), escapeLog(sortOrder));
        }

        MetricAggregationType type = validateAggregationType(aggregationType);
        metricUtil.validateSortBy(sortBy);
        int validatedLimit = metricUtil.validatePagination(limit, startIndex);
        Date start = metricUtil.parseDateParam(startDate, "start_date");
        Date end = metricUtil.parseEndDateParam(endDate, "end_date");
        metricUtil.validateRange(start, end);

        PagedResult<MetricAggregationEntry> pagedResult;
        try {
            pagedResult = metricAggregationService.findEntries(type, appType, metricType, subType, start, end,
                    sortBy, SortOrder.getByValue(metricUtil.normalizeSortOrder(sortOrder)), startIndex, validatedLimit,
                    metricUtil.getRecordMaxCount());
        } catch (Exception ex) {
            logger.error("Error while getting metric aggregations", ex);
            throwInternalServerException(ex);
            return null;
        }

        MetricAggregationPagedResult result = new MetricAggregationPagedResult();
        result.setStart(pagedResult.getStart());
        result.setEntriesCount(pagedResult.getEntriesCount());
        result.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
        result.setEntries(pagedResult.getEntries());
        return Response.ok(result).build();
    }

    private MetricAggregationType validateAggregationType(String aggregationType) {
        // Locale.ROOT avoids the Turkish-locale "i" problem, where "daily".toUpperCase() produces
        // "DAİLI" (dotted capital I) instead of "DAILY", which would fail the lookup below.
        MetricAggregationType type = StringUtils.isBlank(aggregationType) ? null
                : MetricAggregationType.getByValue(aggregationType.trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            // Computed from the enum (rather than the ALLOWED_AGGREGATION_TYPES constant above) so
            // this message can't drift out of sync if a new aggregation period is ever added.
            String allowedValues = Arrays.stream(MetricAggregationType.values()).map(MetricAggregationType::getValue)
                    .collect(Collectors.joining(", "));
            throwBadRequestException(
                    "Invalid aggregationType '" + aggregationType + "'. Allowed values: " + allowedValues,
                    "INVALID_AGGREGATION_TYPE");
        }
        return type;
    }

}
