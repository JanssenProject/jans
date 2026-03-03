package io.jans.configapi.plugin.fido2.rest;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.databind.JsonNode;

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

    public static final String METRICS_DATE_FORMAT = "dd-MM-yyyy";

    @Inject
    Logger logger;

    @Inject
    Fido2MetricsService fido2MetricsService;

    private class Fido2MetricsEntryPagedResult extends PagedResult<Fido2MetricsEntry> {
    };

    private class Fido2MetricsAggregationPagedResult extends PagedResult<Fido2MetricsAggregation> {
    };

    /**
     * Retrieve a paged list of FIDO2 metrics entries by time range.
     *
     * @param limit          maximum number of results to return
     * @param pattern        search pattern to match entry attributes
     * @param startIndex     1-based index of the first result to return
     * @param sortBy         attribute used to order results
     * @param sortOrder      order direction applied to {@code sortBy}; allowed
     *                       values are "ascending" and "descending"
     * @param fieldValuePair comma-separated field=value pairs to further filter
     *                       results (e.g.
     *                       {@code mail=abc@mail.com,jansStatus=true})
     * @return a Response containing a Fido2MetricsEntryPagedResult with the
     *         matching entries
     */
    @Operation(summary = "Get a list of Fido2 Metrics by time range.", description = "Get a list of Fido2 Metrics by time range.", operationId = "search-fido2-metrics", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-metrics-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/entries")
    // @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes =
    // {}, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
    // ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFido2MetricsEntry(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Start date/time for the log entries report. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate)
            throws Exception {

        if (logger.isInfoEnabled()) {
            logger.info("Fido2MetricsEntry search param - limit:{}, startIndex:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startIndex), escapeLog(startDate), escapeLog(endDate));
        }

        // validate Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(METRICS_DATE_FORMAT);
        validateDate(startDate, endDate, formatter);

        // Fetch Fido2 Metrics Entries

        // startDate (supports dd-MM-yyyy and ISO-8601 date-time e.g.
        // yyyy-MM-ddTHH:mm:ssZ)
        LocalDateTime startLocalDate = null;
        if (StringUtils.isNotBlank(startDate)) {
            startLocalDate = parseDate(startDate, formatter);
        }

        // endDate (supports dd-MM-yyyy and ISO-8601 date-time e.g.
        // yyyy-MM-ddTHH:mm:ssZ)
        LocalDateTime endLocalDate = null;
        if (StringUtils.isNotBlank(endDate)) {
            endLocalDate = parseDate(endDate, formatter);
        }

        PagedResult<Fido2MetricsEntry> pagedResult = fido2MetricsService.searchFido2MetricsEntries(null, startLocalDate,
                endLocalDate);

        if (logger.isDebugEnabled()) {
            logger.debug("Fido2RegistrationEntry  - pagedResult:{}", pagedResult);
        }

        return Response.ok(getFido2MetricsEntryPagedResult(pagedResult)).build();
    }

    // aggregation endpoints
    @Operation(summary = "Get a list of Fido2 aggregations by time range.", description = "Get a list of Fido2 aggregations by time range.", operationId = "search-fido2-metrics-aggregations", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsAggregationPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-metrics-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/aggregations/{aggregationType}")
    // @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes =
    // {}, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
    // ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFido2MetricsAggregation(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = " Metrics Aggregation Type") @DefaultValue("") @PathParam("aggregationType") @NotNull String aggregationType,
            @Parameter(description = "Start date/time for the log entries report. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate)
            throws Exception {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry search param - limit:{}, startIndex:{}, aggregationType:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startIndex), escapeLog(aggregationType), escapeLog(startDate),
                    escapeLog(endDate));
        }

        // validate Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(METRICS_DATE_FORMAT);
        validateDate(startDate, endDate, formatter);

        // startDate (supports dd-MM-yyyy and ISO-8601 date-time e.g.
        // yyyy-MM-ddTHH:mm:ssZ)
        LocalDateTime startLocalDate = null;
        if (StringUtils.isNotBlank(startDate)) {
            startLocalDate = parseDate(startDate, formatter);
        }

        // endDate (supports dd-MM-yyyy and ISO-8601 date-time e.g.
        // yyyy-MM-ddTHH:mm:ssZ)
        LocalDateTime endLocalDate = null;
        if (StringUtils.isNotBlank(endDate)) {
            endLocalDate = parseDate(endDate, formatter);
        }

        PagedResult<Fido2MetricsAggregation> pagedResult = fido2MetricsService.searchFido2MetricsAggregation(null,
                aggregationType, startLocalDate, endLocalDate);

        if (logger.isDebugEnabled()) {
            logger.debug("Fido2RegistrationEntry  - pagedResult:{}", pagedResult);
        }

        return Response.ok(this.getFido2MetricsAggregationPagedResult(pagedResult)).build();
    }

    // aggregation summary
    @Operation(summary = "Get a list of Fido2 aggregations by time range.", description = "Get a list of Fido2 aggregations by time range.", operationId = "search-fido2-metrics-aggregations", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-metrics-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/aggregations/{aggregationType}/summary")
    // @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes =
    // {}, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
    // ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFido2MetricsAggregationSummary(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = " Metrics Aggregation Type") @DefaultValue("") @PathParam("aggregationType") @NotNull String aggregationType,
            @Parameter(description = "Start date/time for the log entries report. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "start_date") @NotNull String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "end_date") @NotNull String endDate)
            throws Exception {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry search param - limit:{}, startIndex:{}, aggregationType:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(startIndex), escapeLog(aggregationType), escapeLog(startDate),
                    escapeLog(endDate));
        }

        // validate Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(METRICS_DATE_FORMAT);
        validateDate(startDate, endDate, formatter);

        // startDate (supports dd-MM-yyyy and ISO-8601 date-time e.g.
        // yyyy-MM-ddTHH:mm:ssZ)
        LocalDateTime startLocalDate = null;
        if (StringUtils.isNotBlank(startDate)) {
            startLocalDate = parseDate(startDate, formatter);
        }

        // endDate (supports dd-MM-yyyy and ISO-8601 date-time e.g.
        // yyyy-MM-ddTHH:mm:ssZ)
        LocalDateTime endLocalDate = null;
        if (StringUtils.isNotBlank(endDate)) {
            endLocalDate = parseDate(endDate, formatter);
        }

        JsonNode jsonNode = fido2MetricsService.searchFido2MetricsAggregationSummary(null, aggregationType,
                startLocalDate, endLocalDate);

        if (logger.isDebugEnabled()) {
            logger.debug("Fido2RegistrationEntry  - jsonNode:{}", jsonNode);
        }

        return Response.ok(jsonNode).build();
    }

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
                startLocalDateTime = parseDate(startDate, formatter);
            } catch (DateTimeParseException dtpe) {
                sb.append("Start date is not valid. Use dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).");
            }
        }

        // validate endDate
        if (StringUtils.isNotBlank(endDate)) {
            try {
                endLocalDateTime = parseDate(endDate, formatter);
            } catch (DateTimeParseException dtpe) {
                sb.append("End date is not valid. Use dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).");
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
     * @param dateStr           the date or date-time string from the API request
     * @param fallbackFormatter optional configured audit log date format (e.g.
     *                          dd-MM-yyyy)
     * @return LocalDate for the given string, or null if dateStr is blank
     * @throws DateTimeParseException if the string cannot be parsed with any
     *                                supported format
     */
    private LocalDateTime parseDate(String dateStr, DateTimeFormatter fallbackFormatter) throws DateTimeParseException {
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

    private Fido2MetricsEntryPagedResult getFido2MetricsEntryPagedResult(PagedResult<Fido2MetricsEntry> pagedResult) {
        Fido2MetricsEntryPagedResult fido2MetricsEntryPagedResult = null;

        if (pagedResult != null) {
            List<Fido2MetricsEntry> identityProviderList = pagedResult.getEntries();
            fido2MetricsEntryPagedResult = new Fido2MetricsEntryPagedResult();
            fido2MetricsEntryPagedResult.setStart(pagedResult.getStart());
            fido2MetricsEntryPagedResult.setEntriesCount(pagedResult.getEntriesCount());
            fido2MetricsEntryPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            fido2MetricsEntryPagedResult.setEntries(identityProviderList);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Fido2MetricsEntry fido2MetricsEntryPagedResult:{}", fido2MetricsEntryPagedResult);
        }
        return fido2MetricsEntryPagedResult;
    }

    private Fido2MetricsAggregationPagedResult getFido2MetricsAggregationPagedResult(
            PagedResult<Fido2MetricsAggregation> pagedResult) {
        Fido2MetricsAggregationPagedResult fido2MetricsAggregationPagedResult = null;

        if (pagedResult != null) {
            List<Fido2MetricsAggregation> identityProviderList = pagedResult.getEntries();
            fido2MetricsAggregationPagedResult = new Fido2MetricsAggregationPagedResult();
            fido2MetricsAggregationPagedResult.setStart(pagedResult.getStart());
            fido2MetricsAggregationPagedResult.setEntriesCount(pagedResult.getEntriesCount());
            fido2MetricsAggregationPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            fido2MetricsAggregationPagedResult.setEntries(identityProviderList);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Fido2MetricsAggregation fido2MetricsAggregationPagedResult:{}",
                    fido2MetricsAggregationPagedResult);
        }
        return fido2MetricsAggregationPagedResult;
    }

}