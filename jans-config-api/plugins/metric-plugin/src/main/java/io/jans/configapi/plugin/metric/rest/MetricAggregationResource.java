package io.jans.configapi.plugin.metric.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.metric.service.MetricAggregationService;
import io.jans.configapi.plugin.metric.util.Constants;
import io.jans.configapi.plugin.metric.util.MetricDateUtil;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only endpoint over jansMetricAggregation data. The producer that writes aggregation rows
 * runs on application nodes and is implemented as a separate task, so this endpoint returns an
 * empty page until such a producer exists.
 */
@Path(Constants.AGGREGATIONS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MetricAggregationResource extends BaseResource {

    private static final Set<String> SORTABLE_ATTRIBUTES = Set.of("jansStartDate", "jansEndDate", "creationDate");
    private static final String DEFAULT_SORT_BY = "jansStartDate";
    // Must be a compile-time constant to appear in the @Parameter annotation below - keep this in
    // sync by hand with io.jans.model.metric.MetricAggregationType's values.
    private static final String ALLOWED_AGGREGATION_TYPES = "HOURLY, DAILY, WEEKLY, MONTHLY";

    @Inject
    Logger logger;

    @Inject
    MetricAggregationService metricAggregationService;

    @Inject
    MetricUtil metricUtil;

    private class MetricAggregationPagedResult extends PagedResult<MetricAggregationEntry> {
    }

    @Operation(summary = "Get aggregated metric entries within a time range.", description = "Returns a page of jansMetricAggregation entries for an aggregation period (HOURLY, DAILY, WEEKLY, MONTHLY) within the given date range. The aggregation producer runs on application nodes as a separate task; until it is deployed this endpoint returns an empty page.", operationId = "get-metric-aggregations", tags = {
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
            @Parameter(description = "Start date/time for entries. Accepted format ISO-8601 date-time (e.g. 2026-08-01T00:00:00Z), ISO-8601 date, or legacy dd-MM-yyyy.", schema = @Schema(type = "string")) @QueryParam(Constants.START_DATE) @NotNull(message = "The attribute 'start_date' is required for this operation") String startDate,
            @Parameter(description = "End date/time for entries.", schema = @Schema(type = "string")) @QueryParam(Constants.END_DATE) @NotNull(message = "The attribute 'end_date' is required for this operation") String endDate,
            @Parameter(description = "Maximum number of results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute to sort by. One of jansStartDate, jansEndDate, creationDate.") @DefaultValue(DEFAULT_SORT_BY) @QueryParam(ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Sort order - ascending or descending.") @DefaultValue(ApiConstants.DESCENDING) @QueryParam(ApiConstants.SORT_ORDER) String sortOrder) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Get metric aggregations - aggregationType:{}, appType:{}, metricType:{}, subType:{}, startDate:{}, endDate:{}, limit:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(aggregationType), escapeLog(appType), escapeLog(metricType), escapeLog(subType),
                    escapeLog(startDate), escapeLog(endDate), escapeLog(limit), escapeLog(startIndex),
                    escapeLog(sortBy), escapeLog(sortOrder));
        }

        MetricAggregationType type = validateAggregationType(aggregationType);
        validateSortBy(sortBy);
        Date start = parseDateParam(startDate, "start_date");
        Date end = parseDateParam(endDate, "end_date");
        validateRange(start, end);

        PagedResult<MetricAggregationEntry> pagedResult;
        try {
            pagedResult = metricAggregationService.findEntries(type, appType, metricType, subType, start, end,
                    sortBy, SortOrder.getByValue(normalizeSortOrder(sortOrder)), startIndex, limit,
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
        MetricAggregationType type = StringUtils.isBlank(aggregationType) ? null
                : MetricAggregationType.getByValue(aggregationType.trim().toUpperCase());
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

    private void validateSortBy(String sortBy) {
        if (StringUtils.isNotBlank(sortBy) && !SORTABLE_ATTRIBUTES.contains(sortBy)) {
            throwBadRequestException(
                    "Invalid sortBy '" + sortBy + "'. Allowed values: " + SORTABLE_ATTRIBUTES, "INVALID_SORT_BY");
        }
    }

    private String normalizeSortOrder(String sortOrder) {
        return ApiConstants.ASCENDING.equals(sortOrder) ? ApiConstants.ASCENDING : ApiConstants.DESCENDING;
    }

    private Date parseDateParam(String value, String paramName) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            LocalDateTime parsed = MetricDateUtil.parseDateTime(value);
            return MetricDateUtil.toDate(parsed);
        } catch (DateTimeParseException dtpe) {
            throwBadRequestException("Invalid '" + paramName + "' value '" + value + "'. "
                    + MetricDateUtil.ACCEPTED_FORMATS_MESSAGE, "INVALID_DATE");
            return null;
        }
    }

    private void validateRange(Date start, Date end) {
        if (start != null && end != null && start.after(end)) {
            throwBadRequestException("start_date must be before or equal to end_date.", "INVALID_DATE_RANGE");
        }
    }

}
