/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.metric.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.metric.model.MetricTypeInfo;
import io.jans.configapi.plugin.metric.service.MetricDataService;
import io.jans.configapi.plugin.metric.util.Constants;
import io.jans.configapi.plugin.metric.util.MetricUtil;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Read-only endpoints over jansMetric data: metric type discovery and raw entries for a period.
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MetricResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    MetricDataService metricDataService;

    @Inject
    MetricUtil metricUtil;

    private class MetricEntryPagedResult extends PagedResult<MetricDataEntry> {
    }

    @Operation(summary = "Get metric types reported by applications.", description = "Discovers the distinct metric types (jansMetricTyp), and their subtypes (jansMetricSubTyp) when present, found in jansMetric data - optionally filtered by application type (jansAppTyp) and narrowed to a date range.", operationId = "get-metric-types", tags = {
            "Metric" }, security = { @SecurityRequirement(name = "oauth2", scopes = { Constants.METRIC_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = MetricTypeInfo.class)))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.TYPES)
    @ProtectedApi(scopes = { Constants.METRIC_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getMetricTypes(
            @Parameter(description = "Application type (jansAppTyp) to narrow the search, for example jans_auth or fido2.") @QueryParam(Constants.APP_TYPE) String appType,
            @Parameter(description = "Start date/time to narrow the discovery scan. Accepted format ISO-8601 date-time (e.g. 2026-08-01T00:00:00Z), ISO-8601 date, or legacy dd-MM-yyyy.", schema = @Schema(type = "string")) @QueryParam(Constants.START_DATE) String startDate,
            @Parameter(description = "End date/time to narrow the discovery scan.", schema = @Schema(type = "string")) @QueryParam(Constants.END_DATE) String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info("Get metric types - appType:{}, startDate:{}, endDate:{}", escapeLog(appType),
                    escapeLog(startDate), escapeLog(endDate));
        }

        Date start = metricUtil.parseDateParam(startDate, "start_date");
        Date end = metricUtil.parseDateParam(endDate, "end_date");
        metricUtil.validateRange(start, end);

        List<MetricTypeInfo> result;
        try {
            result = metricDataService.findMetricTypes(appType, start, end, metricUtil.getMetricTypesScanLimit());
        } catch (Exception ex) {
            logger.error("Error while getting metric types", ex);
            throwInternalServerException(ex);
            return null;
        }
        return Response.ok(result).build();
    }

    @Operation(summary = "Get application types reporting metrics.", description = "Discovers the distinct application types (jansAppTyp) present in jansMetric data.", operationId = "get-metric-app-types", tags = {
            "Metric" }, security = { @SecurityRequirement(name = "oauth2", scopes = { Constants.METRIC_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.APP_TYPES)
    @ProtectedApi(scopes = { Constants.METRIC_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAppTypes() {
        if (logger.isInfoEnabled()) {
            logger.info("Get metric application types");
        }

        List<String> result;
        try {
            result = metricDataService.findAppTypes(metricUtil.getMetricTypesScanLimit());
        } catch (Exception ex) {
            logger.error("Error while getting metric application types", ex);
            throwInternalServerException(ex);
            return null;
        }
        return Response.ok(result).build();
    }

    @Operation(summary = "Get metric entries within a time range.", description = "Returns a page of raw jansMetric entries for a metric type (and, optionally, application type and subtype) within the given date range. When subType is omitted, both plain and per-subtype rows are returned.", operationId = "get-metric-entries", tags = {
            "Metric" }, security = { @SecurityRequirement(name = "oauth2", scopes = { Constants.METRIC_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MetricEntryPagedResult.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.ENTRIES)
    @ProtectedApi(scopes = { Constants.METRIC_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getMetricEntries(
            @Parameter(description = "Metric type (jansMetricTyp), for example user_authentication_success.") @QueryParam(Constants.METRIC_TYPE) @NotNull(message = "The attribute 'metricType' is required for this operation") String metricType,
            @Parameter(description = "Application type (jansAppTyp) to narrow the search, for example jans_auth or fido2.") @QueryParam(Constants.APP_TYPE) String appType,
            @Parameter(description = "Metric subtype (jansMetricSubTyp). When omitted, both plain and per-subtype rows are returned.") @QueryParam(Constants.SUB_TYPE) String subType,
            @Parameter(description = "Start date/time for entries. Accepted format ISO-8601 date-time (e.g. 2026-08-01T00:00:00Z), ISO-8601 date, or legacy dd-MM-yyyy.", schema = @Schema(type = "string")) @QueryParam(Constants.START_DATE) @NotNull(message = "The attribute 'start_date' is required for this operation") String startDate,
            @Parameter(description = "End date/time for entries.", schema = @Schema(type = "string")) @QueryParam(Constants.END_DATE) @NotNull(message = "The attribute 'end_date' is required for this operation") String endDate,
            @Parameter(description = "Maximum number of results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute to sort by. One of jansStartDate, jansEndDate, creationDate.") @DefaultValue(MetricUtil.DEFAULT_SORT_BY) @QueryParam(ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Sort order - ascending or descending.") @DefaultValue(ApiConstants.DESCENDING) @QueryParam(ApiConstants.SORT_ORDER) String sortOrder) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Get metric entries - metricType:{}, appType:{}, subType:{}, startDate:{}, endDate:{}, limit:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(metricType), escapeLog(appType), escapeLog(subType), escapeLog(startDate),
                    escapeLog(endDate), escapeLog(limit), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }

        metricUtil.validateSortBy(sortBy);
        int validatedLimit = metricUtil.validatePagination(limit, startIndex);
        Date start = metricUtil.parseDateParam(startDate, "start_date");
        Date end = metricUtil.parseDateParam(endDate, "end_date");
        metricUtil.validateRange(start, end);

        PagedResult<MetricDataEntry> pagedResult;
        try {
            pagedResult = metricDataService.findEntries(appType, metricType, subType, start, end, sortBy,
                    SortOrder.getByValue(metricUtil.normalizeSortOrder(sortOrder)), startIndex, validatedLimit,
                    metricUtil.getRecordMaxCount());
        } catch (Exception ex) {
            logger.error("Error while getting metric entries", ex);
            throwInternalServerException(ex);
            return null;
        }

        MetricEntryPagedResult result = new MetricEntryPagedResult();
        result.setStart(pagedResult.getStart());
        result.setEntriesCount(pagedResult.getEntriesCount());
        result.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
        result.setEntries(pagedResult.getEntries());
        return Response.ok(result).build();
    }

}
