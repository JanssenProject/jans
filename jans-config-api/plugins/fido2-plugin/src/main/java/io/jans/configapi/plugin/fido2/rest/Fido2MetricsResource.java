package io.jans.configapi.plugin.fido2.rest;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2MetricsService;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.util.exception.InvalidAttributeException;

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

import com.fasterxml.jackson.core.JsonProcessingException;

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

    /**
     * Retrieve a paged list of FIDO2 metrics entries matching the provided search
     * criteria.
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
    @Operation(summary = "Get a list of Fido2 Metrics Entry.", description = "Get a list of Fido2 Metrics Entry.", operationId = "search-metrics-data", tags = {
            "Fido2 - Metrics" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_METRICS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Fido2MetricsEntryPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/search-fido2-metrics-data.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    // @ProtectedApi(scopes = { Constants.FIDO2_METRICS_READ_ACCESS }, groupScopes =
    // {}, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
    // ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getFido2MetricsEntry(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Data whose value will be used to order the returned response") @DefaultValue(Constants.JANSID) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "mail=abc@mail.com,jansStatus=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair,
            @Parameter(description = "Start date/time for the log entries report. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "start_date") String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).", schema = @Schema(type = "string")) @QueryParam(value = "end_date") String endDate) {

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Fido2MetricsEntry search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}, startDate:{}, endDate:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair), escapeLog(startDate), escapeLog(endDate));
        }

        SearchRequest searchReq = createSearchRequest(fido2MetricsService.getBaseDnForFido2MetricsEntry(), pattern,
                null, null, startIndex, limit, null, null, Constants.DEFAULT_MAX_COUNT, fieldValuePair,
                Fido2MetricsEntry.class);

        return Response.ok(this.doSearch(searchReq, startDate, endDate)).build();
    }

    private Fido2MetricsEntryPagedResult doSearch(SearchRequest searchReq, String startDate, String endDate) {
        if (logger.isInfoEnabled()) {
            logger.info("Fido2 Metrics search params - searchReq:{}", escapeLog(searchReq));
        }
        Fido2MetricsEntryPagedResult pagedFido2MetricsEntry = null;
        try {

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

            PagedResult<Fido2MetricsEntry> list = fido2MetricsService.searchFido2MetricsEntries(searchReq, null,
                    startLocalDate, endLocalDate);
            PagedResult<Fido2MetricsEntry> pagedResult = null;
            if (logger.isDebugEnabled()) {
                logger.debug("Fido2RegistrationEntry  - pagedResult:{}", pagedResult);
            }

            pagedFido2MetricsEntry = new Fido2MetricsEntryPagedResult();
            if (pagedResult != null) {
                logger.debug("Users fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
                pagedFido2MetricsEntry.setStart(pagedResult.getStart());
                pagedFido2MetricsEntry.setEntriesCount(pagedResult.getEntriesCount());
                pagedFido2MetricsEntry.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
                pagedFido2MetricsEntry.setEntries(pagedResult.getEntries());
            }

            logger.info("Fido2MetricsEntry pagedFido2MetricsEntry:{}", pagedFido2MetricsEntry);

        } catch (Exception ex) {
            logger.error("Exception while updating user is - ", ex);
            throwInternalServerException(ex);
        }
        return pagedFido2MetricsEntry;
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

}