package io.jans.configapi.plugin.fido2.rest;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.fido2.model.metric.Fido2MetricsAggregation;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2MetricsService;
import io.jans.configapi.plugin.fido2.util.Constants;

import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.PagedResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import java.util.function.*;
import java.util.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Path(Constants.METRICS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2MetricsResource extends BaseResource {

    private static final String METRICS_DATE_FORMAT = "dd-MM-yyyy";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(METRICS_DATE_FORMAT);
    private static final String ALL_PARAM = "Search param - limit:{}, startIndex:{}, startDate:{}, endDate:{}";
    private static final String DATE_PARAM = "Search param - startDate:{}, endDate:{}";
    private static final String ERR_MSG = "Exception while getting metric data is - ";

    @Inject
    Logger logger;

    @Inject
    Fido2MetricsService fido2MetricsService;

    private class Fido2MetricsEntryPagedResult extends PagedResult<Fido2MetricsEntry> {
    };

    private class Fido2MetricsAggregationPagedResult extends PagedResult<Fido2MetricsAggregation> {
    };

    /**
     * Get metrics entries within a time range.
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date values are "ascending" and "descending"
     *
     * @return a Response containing a Fido2MetricsEntryPagedResult with the
     *         matching entries
     * 
     */
    @Operation(summary = "Get a list of Fido2 Metrics by time range.", description = "Get a list of Fido2 Metrics by time range.", operationId = "get-fido2-metrics", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/entries")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getFido2MetricsEntry(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Start date/time for entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(ALL_PARAM, escapeLog(limit), escapeLog(startIndex), escapeLog(startDate), escapeLog(endDate));
        }

        PagedResult<Fido2MetricsEntry> pagedResult = null;
        try {
            pagedResult = builPagedResult(startDate, endDate, formatter,

                    // 🔹 supplier: fetch data
                    (start, end) -> {
                        PagedResult<Fido2MetricsEntry> result = null;
                        try {
                            result = fido2MetricsService.getFido2MetricsEntries(null, start, end);
                        } catch (JsonProcessingException jex) {
                            throwInternalServerException(jex);
                        }
                        return result;
                    });

            logger.debug(" getFido2MetricsEntry() - pagedResult:{}", pagedResult);

        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(getFido2MetricsEntryPagedResult(pagedResult, limit, startIndex)).build();
    }

    /**
     * Get metrics entries for a specific user.
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param userId     User ID
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date values are "ascending" and "descending"
     *
     * @return a Response containing a Fido2MetricsEntryPagedResult with the
     *         matching entries
     * 
     */
    @Operation(summary = "Get a list of Fido2 metrics for a specific user by time range.", description = "Get a list of Fido2 metrics for a specific user by time range.", operationId = "get-fido2-metrics-by-user", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-by-user.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/entries/user/{userId}")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getFido2UserMetricsEntries(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "user Id") @PathParam("userId") @NotNull String userId,
            @Parameter(description = "Start date/time for entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info("Fido2MetricsEntries for user search param - limit:{}, startIndex:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startIndex), escapeLog(startDate), escapeLog(endDate));
        }
        PagedResult<Fido2MetricsEntry> pagedResult = null;
        try {

            // validate Date
            validateDate(startDate, endDate, formatter);

            pagedResult = fido2MetricsService.getFido2UserMetricsEntries(null, userId, parseDate(startDate),
                    parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2UserMetricsEntries  - pagedResult:{}", pagedResult);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }

        return Response.ok(getFido2MetricsEntryPagedResult(pagedResult, limit, startIndex)).build();
    }

    /**
     * Get metrics entries by operation type.
     *
     * @param limit         maximum number of results to return
     * @param startIndex    0-based index of the first result to return
     * @param operationType Operation type
     * @param startDate     optional start date (dd-MM-yyyy) to include entries on
     *                      or after this date
     * @param endDate       optional end date (dd-MM-yyyy) to include entries on or
     *                      before this date values are "ascending" and "descending"
     *
     * @return a Response containing a Fido2MetricsEntryPagedResult with the
     *         matching entries
     * 
     */
    @Operation(summary = "Get a list of Fido2 metrics for a operation type by time range.", description = "Get a list of Fido2 metrics for a operation type by time range.", operationId = "get-fido2-metrics-by-operation", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-by-operation-type.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/entries/operation/{operationType}")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getMetricsEntriesByOperation(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Operation Type") @PathParam("operationType") @NotNull String operationType,
            @Parameter(description = "Start date/time for entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry by operation type search param - limit:{}, startIndex:{}, operationType:{} startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startIndex), escapeLog(operationType), escapeLog(startDate),
                    escapeLog(endDate));
        }

        PagedResult<Fido2MetricsEntry> pagedResult = null;
        try {
            // validate Date
            validateDate(startDate, endDate, formatter);

            pagedResult = fido2MetricsService.getMetricsEntriesByOperation(null, operationType, parseDate(startDate),
                    parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2MetricsEntriesByOperation - pagedResult:{}", pagedResult);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(getFido2MetricsEntryPagedResult(pagedResult, limit, startIndex)).build();
    }

    // aggregation endpoints
    /**
     * Get aggregated metrics data.
     *
     * @param limit           maximum number of results to return
     * @param startIndex      0-based index of the first result to return
     * @param aggregationType Aggregation type
     * @param startDate       optional start date (dd-MM-yyyy) to include entries on
     *                        or after this date
     * @param endDate         optional end date (dd-MM-yyyy) to include entries on
     *                        or before this date values are "ascending" and
     *                        "descending"
     *
     * @return a Response containing a Fido2MetricsAggregationPagedResult with the
     *         matching entries
     * 
     */
    @Operation(summary = "Get a list of Fido2 aggregated metrics by time range.", description = "Get a list of Fido2 aggregated metrics by time range.", operationId = "get-fido2-metrics-aggregated", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsAggregationPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-aggregations-type.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/aggregations/{aggregationType}")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getFido2MetricsAggregation(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 0-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = " Aggregation Type", schema = @Schema(type = "string")) @DefaultValue("") @PathParam("aggregationType") @NotNull String aggregationType,
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry aggregation search param - limit:{}, startIndex:{}, aggregationType:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startIndex), escapeLog(aggregationType), escapeLog(startDate),
                    escapeLog(endDate));
        }

        PagedResult<Fido2MetricsAggregation> pagedResult = null;
        try {

            // validate Date
            validateDate(startDate, endDate, formatter);

            pagedResult = fido2MetricsService.getFido2MetricsAggregation(null, aggregationType, parseDate(startDate),
                    parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2MetricsAggregation  - pagedResult:{}", pagedResult);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }

        return Response.ok(this.getFido2MetricsAggregationPagedResult(pagedResult, limit, startIndex)).build();
    }

    // aggregation summary
    /**
     * Get aggregation summary statistics.
     *
     * @param limit           maximum number of results to return
     * @param startIndex      0-based index of the first result to return
     * @param aggregationType Aggregation type
     * @param startDate       optional start date (dd-MM-yyyy) to include entries on
     *                        or after this date
     * @param endDate         optional end date (dd-MM-yyyy) to include entries on
     *                        or before this date
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 aggregation summary by time range.", description = "Get Fido2 aggregation summary by time range.", operationId = "get-fido2-aggregation-summary-metrics", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-aggregation-summary.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/aggregations/{aggregationType}/summary")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getFido2MetricsAggregationSummary(
            @Parameter(description = "Metrics Aggregation Type") @DefaultValue("") @PathParam("aggregationType") @NotNull String aggregationType,
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry aggregation summary search param - aggregationType:{}, startDate:{}, endDate:{}",
                    escapeLog(aggregationType), escapeLog(startDate), escapeLog(endDate));
        }
        JsonNode jsonNode = null;
        try {
            // validate Date
            validateDate(startDate, endDate, formatter);

            jsonNode = fido2MetricsService.getFido2MetricsAggregationSummary(null, aggregationType,
                    parseDate(startDate), parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2MetricsAggregationSummary  - jsonNode:{}", jsonNode);
            }

        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    /**
     * Get adoption metrics
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 adoption metrics by time range.", description = "Get Fido2 adoption metrics by time range.", operationId = "get-fido2-adoption-metrics", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-adoption.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/analytics/adoption")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getAdoptionMetrics(
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(DATE_PARAM, escapeLog(startDate), escapeLog(endDate));
        }

        JsonNode jsonNode = null;
        try {
            // validate Date
            validateDate(startDate, endDate, formatter);

            jsonNode = fido2MetricsService.getAdoptionMetrics(null, parseDate(startDate), parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2AdoptionMetrics  - jsonNode:{}", jsonNode);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    /**
     * Get performance metrics
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 analytics performance metrics by time range.", description = "Get Fido2 analytics performance metrics by time range.", operationId = "get-fido2-analytics-performance-metrics", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-analytics-performance-metrics.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/analytics/performance")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getPerformanceMetrics(
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(DATE_PARAM, escapeLog(startDate), escapeLog(endDate));
        }
        JsonNode jsonNode = null;
        try {

            // validate Date
            validateDate(startDate, endDate, formatter);

            jsonNode = fido2MetricsService.getPerformanceMetrics(null, parseDate(startDate), parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2 analytics performance metrics  - jsonNode:{}", jsonNode);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    /**
     * Get device analytics
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 devices analytics metrics by time range.", description = "Get Fido2 devices analytics metrics by time range.", operationId = "get-fido2-metrics-analytics-devices", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-analytics-devices.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/analytics/devices")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getDeviceAnalytics(
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(DATE_PARAM, escapeLog(startDate), escapeLog(endDate));
        }
        JsonNode jsonNode = null;
        try {
            // validate Date
            validateDate(startDate, endDate, formatter);

            jsonNode = fido2MetricsService.getDeviceAnalytics(null, parseDate(startDate), parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2DeviceAnalytics  - jsonNode:{}", jsonNode);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }

        return Response.ok(jsonNode).build();
    }

    /**
     * Get error analysis
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 error analysis metrics by time range.", description = "Get Fido2 error analysis metrics by time range.", operationId = "get-fido2-metrics-analytics-errors", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-analytics-errors.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/analytics/errors")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getErrorAnalysis(
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(DATE_PARAM, escapeLog(startDate), escapeLog(endDate));
        }
        JsonNode jsonNode = null;
        try {

            // validate Date
            validateDate(startDate, endDate, formatter);

            jsonNode = fido2MetricsService.getErrorAnalysis(null, parseDate(startDate), parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2ErrorAnalysis  - jsonNode:{}", jsonNode);
            }
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    /**
     * Get trend analysis for metrics over time
     *
     * @param limit      maximum number of results to return
     * @param startIndex 0-based index of the first result to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 analytics trends by aggregationType for metrics over time.", description = "Get Fido2 analytics trends by aggregationType for metrics over time.", operationId = "get-fido2-analytics-trends-aggregationType", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-analytics-trends-aggregation-type.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/analytics/trends/{aggregationType}")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getTrendAnalysis(
            @Parameter(description = "Aggregation Type") @DefaultValue("") @PathParam("aggregationType") @NotNull String aggregationType,
            @Parameter(description = "Start date/time for the log entries report. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted format dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info("ErrorAnalysis search param -  aggregationType:{}, startDate:{}, endDate:{}",
                    escapeLog(aggregationType), escapeLog(startDate), escapeLog(endDate));
        }

        JsonNode jsonNode = null;
        try {
            // validate Date
            validateDate(startDate, endDate, formatter);

            jsonNode = fido2MetricsService.getTrendAnalysis(null, aggregationType, parseDate(startDate),
                    parseDate(endDate));

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2TrendAnalysis - jsonNode:{}", jsonNode);
            }

        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }

        return Response.ok(jsonNode).build();
    }

    /**
     * Get period-over-period comparison
     *
     * @param aggregationType Aggregation type for comparison
     * @param periods         Number of periods to compare (default: 2)
     * 
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 period-over-period comparison.", description = "Get Fido2 period-over-period comparison.", operationId = "get-fido2-period-over-period-comparison", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-period-over-period-comparison.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/analytics/comparison/{aggregationType}")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getPeriodOverPeriodComparison(
            @Parameter(description = " Aggregation Type") @DefaultValue("") @PathParam("aggregationType") @NotNull String aggregationType,
            @Parameter(description = "periods") @DefaultValue("2") @QueryParam(value = "periods") int periods) {

        if (logger.isInfoEnabled()) {
            logger.info("Fido2 analytics comparison search param - aggregationType:{}, periods:{}",
                    escapeLog(aggregationType), escapeLog(periods));
        }

        JsonNode jsonNode = null;

        try {
            jsonNode = fido2MetricsService.getPeriodOverPeriodComparison(null, aggregationType, periods);

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2 analytics comparison - jsonNode:{}", jsonNode);
            }
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }

        return Response.ok(jsonNode).build();
    }

    /**
     * Get metrics configuration and status
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 metrics configuration.", description = "Get Fido2 metrics configuration.", operationId = "get-fido2-metrics-configuration", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-configuration.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/config")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getMetricsConfig() {

        if (logger.isInfoEnabled()) {
            logger.info("Fido2 metrics configuration");
        }

        JsonNode jsonNode = null;

        try {
            jsonNode = fido2MetricsService.getMetricsConfig(null);

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2 metrics configuration - jsonNode:{}", jsonNode);
            }
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    /**
     * Health check endpoint for metrics service Verifies that the metrics service
     * is functional and can connect to the database
     *
     * @return a Response containing a JsonNode with the matching entries
     * 
     */
    @Operation(summary = "Get Fido2 metrics health check endpoint.", description = "Get Fido2 metrics health check endpoint.", operationId = "get-fido2-metrics-health-check", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/metrics/fido2-metrics-health-check.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/health")
    @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getMetricsHealth() {

        if (logger.isInfoEnabled()) {
            logger.info("Fido2 metrics health");
        }
        JsonNode jsonNode = null;
        try {
            jsonNode = fido2MetricsService.getMetricsHealth(null);

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2 metrics health- jsonNode:{}", jsonNode);
            }

        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    // helper methods
    private void validateDate(String startDate, String endDate, DateTimeFormatter formatter) {
        if (logger.isDebugEnabled()) {
            logger.debug(" Validate Date startDate:{}, endDate:{}, formatter:{}", escapeLog(startDate),
                    escapeLog(endDate), formatter);
        }

        StringBuilder sb = new StringBuilder();
        LocalDateTime startLocalDateTime = null;
        LocalDateTime endLocalDateTime = null;

        // validate startDate
        if (StringUtils.isNotBlank(startDate)) {
            try {
                startLocalDateTime = parseDate(startDate);
            } catch (DateTimeParseException dtpe) {
                sb.append(
                        "Start date is not valid. Use dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.");
            }
        }

        // validate endDate
        if (StringUtils.isNotBlank(endDate)) {
            try {
                endLocalDateTime = parseDate(endDate);
            } catch (DateTimeParseException dtpe) {
                sb.append(
                        "End date is not valid. Use dd-MM-yyyy or ISO-8601 date-time like yyyy-MM-ddTHH:mm:ssZ, for example, 31-12-2025 and 2025-12-31T23:59:59Z.");
            }
        }

        if (startLocalDateTime != null && endLocalDateTime != null && startLocalDateTime.isAfter(endLocalDateTime)) {
            sb.append("Start date must be before or equal to end date.");
        }

        if (sb.length() > 0) {
            throwBadRequestException(sb.toString(), "INVALID_DATE");
        }
    }

    /**
     * Parses a user-supplied date string in either ISO-8601 date-time format (e.g.
     * yyyy-MM-ddTHH:mm:ssZ) or legacy dd-MM-yyyy format. Returns the date part as
     * LocalDate for range comparison.
     *
     * @param dateStr the date or date-time string from the API request
     *
     * @return LocalDate for the given string, or null if dateStr is blank
     * @throws DateTimeParseException if the string cannot be parsed with any
     *                                supported format
     */
    private LocalDateTime parseDate(String dateStr) throws DateTimeParseException {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        String trimmed = dateStr.trim();
        // Try ISO-8601 offset date-time (e.g. 2024-02-13T10:30:00Z,
        // 2024-02-13T10:30:00+01:00)
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return zdt.toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try ISO local date-time (e.g. 2024-02-13T10:30:00)
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try ISO date only (e.g. 2024-02-13)
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // continue
        }

        // Fallback to legacy dd-MM-yyyy
        return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern(METRICS_DATE_FORMAT));
    }

    private Fido2MetricsEntryPagedResult getFido2MetricsEntryPagedResult(PagedResult<Fido2MetricsEntry> pagedResult,
            int limit, int startIndex) {
        Fido2MetricsEntryPagedResult fido2MetricsEntryPagedResult = null;

        if (pagedResult != null) {

            List<Fido2MetricsEntry> identityProviderList = pagedResult.getEntries();
            int listSize = (identityProviderList == null) ? 0 : identityProviderList.size();

            validateStartIndex(identityProviderList, startIndex);
            int toIndex = (startIndex + limit <= listSize) ? startIndex + limit : listSize;
            if (logger.isInfoEnabled()) {
                logger.info("Final startIndex:{}, limit:{}, toIndex:{}", escapeLog(startIndex), escapeLog(limit),
                        toIndex);
            }

            // Extract paginated data
            List<Fido2MetricsEntry> sublist = (identityProviderList == null) ? null
                    : identityProviderList.subList(startIndex, toIndex);

            int entriesCount = (sublist == null) ? 0 : sublist.size();

            fido2MetricsEntryPagedResult = new Fido2MetricsEntryPagedResult();
            fido2MetricsEntryPagedResult.setStart(startIndex);
            fido2MetricsEntryPagedResult.setEntriesCount(entriesCount);
            fido2MetricsEntryPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            fido2MetricsEntryPagedResult.setEntries(sublist);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Fido2MetricsEntry fido2MetricsEntryPagedResult:{}", fido2MetricsEntryPagedResult);
        }
        return fido2MetricsEntryPagedResult;
    }

    private Fido2MetricsAggregationPagedResult getFido2MetricsAggregationPagedResult(
            PagedResult<Fido2MetricsAggregation> pagedResult, int limit, int startIndex) {
        Fido2MetricsAggregationPagedResult fido2MetricsAggregationPagedResult = null;

        if (pagedResult != null) {

            List<Fido2MetricsAggregation> identityProviderList = pagedResult.getEntries();
            int listSize = (identityProviderList == null) ? 0 : identityProviderList.size();

            validateStartIndex(identityProviderList, startIndex);
            int toIndex = (startIndex + limit <= listSize) ? startIndex + limit : listSize;
            if (logger.isInfoEnabled()) {
                logger.info("Final listSize:{}, startIndex:{}, limit:{}, toIndex:{}", listSize, escapeLog(startIndex),
                        escapeLog(limit), toIndex);
            }

            // Extract paginated data
            List<Fido2MetricsAggregation> sublist = (identityProviderList == null) ? null
                    : identityProviderList.subList(startIndex, toIndex);

            int entriesCount = (sublist == null) ? 0 : sublist.size();

            fido2MetricsAggregationPagedResult = new Fido2MetricsAggregationPagedResult();
            fido2MetricsAggregationPagedResult.setStart(startIndex);
            fido2MetricsAggregationPagedResult.setEntriesCount(entriesCount);
            fido2MetricsAggregationPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            fido2MetricsAggregationPagedResult.setEntries(sublist);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Fido2MetricsAggregation fido2MetricsAggregationPagedResult:{}",
                    fido2MetricsAggregationPagedResult);
        }
        return fido2MetricsAggregationPagedResult;
    }

    private void validateStartIndex(List<?> entriesList, int startIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Validate startIndex entriesList:{}, startIndex:{}", entriesList, escapeLog(startIndex));
        }

        if (entriesList == null || entriesList.isEmpty() || startIndex < 0) {
            return;
        }

        logger.debug("Validate startIndex  entriesList.size():{}", entriesList.size());
        try {
            entriesList.get(startIndex);
        } catch (IndexOutOfBoundsException ioe) {
            logger.error("Error while getting data startIndex:{}", startIndex, ioe);
            throwBadRequestException("Page start index incorrect, total entries:{" + entriesList.size()
                    + "}, but provided:{" + startIndex + "} ");
        }

    }

    private <T> PagedResult<T> builPagedResult(String startDate, String endDate, DateTimeFormatter fmt,
            BiFunction<LocalDateTime, LocalDateTime, PagedResult<T>> supplier) throws Exception {
        validateDate(startDate, endDate, fmt);
        LocalDateTime start = parseDate(startDate);
        LocalDateTime end = parseDate(endDate);
        PagedResult<T> result = supplier.apply(start, end);
        logger.debug("PagedResult  result :{}", result);
        return result;
    }

}