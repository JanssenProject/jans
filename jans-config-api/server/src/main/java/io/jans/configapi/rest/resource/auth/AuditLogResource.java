package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.model.PagedResult;

import static io.jans.as.model.util.Util.escapeLog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.AUDIT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditLogResource extends ConfigBaseResource {

    public static final String AUDIT_FILE_PATH = "/opt/jans/jetty/jans-config-api/logs/";
    public static final String AUDIT_FILE_NAME = "configapi-audit.log";
    public static final String AUDIT_FILE_DATE_FORMAT = "dd-MM-yyyy";

    /** ISO-8601 date-time with optional offset (e.g. yyyy-MM-ddTHH:mm:ssZ or yyyy-MM-ddTHH:mm:ss.SSSZ). */
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    /** ISO-8601 local date-time (e.g. yyyy-MM-ddTHH:mm:ss). */
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    /** ISO-8601 date only (e.g. yyyy-MM-dd). */
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    static final String AUDIT = "/audit";

    private class LogPagedResult extends PagedResult<String> {
    };

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    /**
     * Searches and returns paginated audit log entries filtered by an optional pattern and date range.
     *
     * @param pattern   a substring or regex to filter log lines; blank returns all entries
     * @param startIndex the 1-based index of the first result to return
     * @param limit     maximum number of results to return
     * @param startDate optional start date (dd-MM-yyyy) to include entries on or after this date
     * @param endDate   optional end date (dd-MM-yyyy) to include entries on or before this date
     * @return          a HTTP 200 Response containing a LogPagedResult with the matching log lines and pagination metadata
     */
    @Operation(summary = "Get audit details.", description = "Get audit details.", operationId = "get-audit-data", tags = {
            "Logs" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.LOGGING_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.LOGGING_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.LOGGING_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LogPagedResult.class), examples = @ExampleObject(name = "Response example"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.LOGGING_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.LOGGING_WRITE_ACCESS }, superScopes = { ApiAccessConstants.LOGGING_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getLogsEnteries(
            @Parameter(description = "Search pattern") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(schema = @Schema(type = "string", description = "Start date/time for the log entries report. Accepted: `dd-MM-yyyy` or ISO-8601 date-time (e.g. `yyyy-MM-ddTHH:mm:ssZ`).")) @QueryParam(value = "start_date") String startDate,
            @Parameter(schema = @Schema(type = "string", description = "End date/time for the log entries. Accepted: `dd-MM-yyyy` or ISO-8601 date-time (e.g. `yyyy-MM-ddTHH:mm:ssZ`).")) @QueryParam(value = "end_date") String endDate)

    {
        if (log.isInfoEnabled()) {
            log.info("Search Attribute filters with pattern:{}, startIndex:{}, limit:{}, startDate:{}, endDate:{}",
                    escapeLog(pattern), escapeLog(startIndex), escapeLog(limit), escapeLog(startDate),
                    escapeLog(endDate));
        }

        return Response.ok(this.doSearch(getPattern(pattern), startIndex, limit, startDate, endDate)).build();
    }

    private LogPagedResult doSearch(String pattern, int startIndex, int limit, String startDate, String endDate) {
        if (log.isDebugEnabled()) {
            log.debug("Search Attribute filters with pattern:{}, startIndex:{}, limit:{}, startDate:{}, endDate:{}",
                    escapeLog(pattern), escapeLog(startIndex), escapeLog(limit), escapeLog(startDate),
                    escapeLog(endDate));
        }

        // Get data based on pattern and date filter
        List<String> logEntriesList = getLogEntries(getAuditLogFile(), pattern);
        log.debug("Log fetched  - logEntriesList:{}", logEntriesList);

        // Date filter
        logEntriesList = filterLogByDate(logEntriesList, startDate, endDate);
        log.debug("Log fetched  - logEntriesList:{}", logEntriesList);

        LogPagedResult logPagedResult = getLogPagedResult(logEntriesList, startIndex, limit);
        log.debug("Audit Log PagedResult:{}", logPagedResult);
        return logPagedResult;

    }

    private List<String> getLogEntries(String file, String pattern) {
        if (log.isDebugEnabled()) {
            log.debug("Fetch log file:{}, pattern:{}", file, pattern);
        }

        List<String> logEntries = new ArrayList<>();
        try (Stream<String> stream = Files.lines(java.nio.file.Path.of(file))) {
            logEntries = stream.filter(s -> s.matches(pattern)).collect(Collectors.toList());
        } catch (IOException ex) {
            throwInternalServerException(" Error while fetching logs", ex);
        }
        return logEntries;
    }

    private String getPattern(String pattern) {
        String searchPattern = ApiConstants.DEFAULT_SEARCH_PATTERN;
        if (StringUtils.isNotBlank(pattern)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("^.*");
            stringBuilder.append(pattern);
            stringBuilder.append(".*$");
            searchPattern = stringBuilder.toString();
        }
        return searchPattern;
    }

    private LogPagedResult getLogPagedResult(List<String> logEntriesList, int startIndex, int limit) {
        if (log.isDebugEnabled()) {
            log.debug("Audit logEntriesList:{}, startIndex:{}, limit:{}", logEntriesList, escapeLog(startIndex),
                    escapeLog(limit));
        }

        LogPagedResult logPagedResult = new LogPagedResult();
        if (logEntriesList != null && !logEntriesList.isEmpty()) {

            try {

                // verify start and limit index
                getStartIndex(logEntriesList, startIndex);
                int toIndex = (startIndex + limit <= logEntriesList.size()) ? startIndex + limit
                        : logEntriesList.size();
                log.info("Final startIndex:{}, limit:{}, toIndex:{}", startIndex, limit, toIndex);

                // Extract paginated data
                List<String> sublist = logEntriesList.subList(startIndex, toIndex);

                logPagedResult.setStart(startIndex);
                logPagedResult.setEntriesCount(limit);
                logPagedResult.setTotalEntriesCount(logEntriesList.size());
                logPagedResult.setEntries(sublist);

            } catch (IndexOutOfBoundsException ioe) {
                log.error("Error while getting log data is - ", ioe);
                throwBadRequestException("Index may be incorrect, total entries:{" + logEntriesList.size()
                        + "}, startIndex provided:{" + startIndex + "} , endtIndex provided:{" + limit + "} ");

            }
        }

        log.info("Audit logPagedResult:{}", logPagedResult);

        return logPagedResult;
    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private String getAuditDateFormat() {
        return this.authUtil.getAuditLogConf().getAuditLogDateFormat();
    }

    private String getAuditLogFile() {

        String filePath = (getAuditLogConf().getAuditLogFilePath() != null ? getAuditLogConf().getAuditLogFilePath()
                : AUDIT_FILE_PATH);
        String fileName = (getAuditLogConf().getAuditLogFileName() != null ? getAuditLogConf().getAuditLogFileName()
                : AUDIT_FILE_NAME);

        StringBuilder stringBuilder = new StringBuilder(filePath);
        stringBuilder.append(fileName);

        return stringBuilder.toString();
    }

    private int getStartIndex(List<String> logEntriesList, int startIndex) {
        if (log.isDebugEnabled()) {
            log.debug("Get startIndex logEntriesList:{}, startIndex:{}", logEntriesList, escapeLog(startIndex));
        }

        if (logEntriesList != null && !logEntriesList.isEmpty()) {

            try {
                logEntriesList.get(startIndex);
            } catch (IndexOutOfBoundsException ioe) {
                log.error("Error while getting data startIndex:{}", startIndex, ioe);
                throwBadRequestException("Page start index incorrect, total entries:{" + logEntriesList.size()
                        + "}, but provided:{" + startIndex + "} ");
            }
        }
        return startIndex;
    }

    private List<String> filterLogByDate(List<String> logEntries, String startDate, String endDate) {
        if (log.isDebugEnabled()) {
            log.debug(" logEntries:{}, startDate:{}, endDate:{} ", logEntries, escapeLog(startDate),
                    escapeLog(endDate));
        }

        if (logEntries == null || logEntries.isEmpty()
                || (StringUtils.isBlank(startDate) && StringUtils.isBlank(endDate))) {
            return logEntries;
        }
        List<String> filteredLogEntries = new ArrayList<>();
        try {
            String datePattern = (StringUtils.isNotBlank(getAuditDateFormat()) ? getAuditDateFormat()
                    : AUDIT_FILE_DATE_FORMAT);
            log.debug("datePattern:{}", datePattern);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern);

            for (int i = 0; i < logEntries.size(); i++) {
                String line = logEntries.get(i);
                if (StringUtils.isNotBlank(line) && line.length() > datePattern.length()) {
                    String timestampPart = line.substring(0, datePattern.length());
                    LocalDate logEntryLocalDate = getDate(timestampPart, formatter);
                    if (isValidlogEntry(formatter, startDate, endDate, logEntryLocalDate)) {
                        filteredLogEntries.add(line);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error while filtering log file with startDate:{} and endDate:{} is:{}", startDate, endDate, ex);
            return logEntries;
        }
        return filteredLogEntries;
    }

    private void validateDate(String startDate, String endDate, DateTimeFormatter formatter) {
        if (log.isDebugEnabled()) {
            log.debug(" Validate Date startDate:{}, endDate:{}, formatter:{}", escapeLog(startDate), escapeLog(endDate),
                    formatter);
        }

        StringBuilder sb = new StringBuilder();
        LocalDate startLocal = null;
        LocalDate endLocal = null;

        // validate startDate
        if (StringUtils.isNotBlank(startDate)) {
            try {
                startLocal = parseUserDate(startDate, formatter);
            } catch (DateTimeParseException dtpe) {
                sb.append("Start date is not valid. Use dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).");
            }
        }

        // validate endDate
        if (StringUtils.isNotBlank(endDate)) {
            try {
                endLocal = parseUserDate(endDate, formatter);
            } catch (DateTimeParseException dtpe) {
                sb.append("End date is not valid. Use dd-MM-yyyy or ISO-8601 date-time (e.g. yyyy-MM-ddTHH:mm:ssZ).");
            }
        }

        if (startLocal != null && endLocal != null && startLocal.isAfter(endLocal)) {
            sb.append("Start date must be before or equal to end date.");
        }

        if (sb.length() > 0) {
            throwBadRequestException(sb.toString(), "INVALID_DATE");
        }
    }

    private boolean isValidlogEntry(DateTimeFormatter formatter, String startDate, String endDate,
            LocalDate logEntryLocalDate) {
        if (log.isDebugEnabled()) {
            log.debug(" formatter:{}, startDate:{}, endDate:{}, logEntryLocalDate:{}", formatter, escapeLog(startDate),
                    escapeLog(endDate), logEntryLocalDate);
        }

        boolean isValid = false;

        // validate date
        validateDate(startDate, endDate, formatter);

        // startDate (supports dd-MM-yyyy and ISO-8601 date-time e.g. yyyy-MM-ddTHH:mm:ssZ)
        LocalDate startLocalDate = null;
        if (StringUtils.isNotBlank(startDate)) {
            startLocalDate = parseUserDate(startDate, formatter);
        }

        // endDate (supports dd-MM-yyyy and ISO-8601 date-time e.g. yyyy-MM-ddTHH:mm:ssZ)
        LocalDate endLocalDate = null;
        if (StringUtils.isNotBlank(endDate)) {
            endLocalDate = parseUserDate(endDate, formatter);
        }

        if (logEntryLocalDate != null) {
            if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)
                    && ((logEntryLocalDate.isEqual(startLocalDate) || logEntryLocalDate.isAfter(startLocalDate))
                            && (logEntryLocalDate.isEqual(endLocalDate) || logEntryLocalDate.isBefore(endLocalDate)))) {
                isValid = true;
            } else if ((StringUtils.isNotBlank(startDate) && StringUtils.isBlank(endDate)
                    && (logEntryLocalDate.isEqual(startLocalDate) || logEntryLocalDate.isAfter(startLocalDate)))

                    || (StringUtils.isBlank(startDate) && StringUtils.isNotBlank(endDate)
                            && (logEntryLocalDate.isEqual(endLocalDate) || logEntryLocalDate.isBefore(endLocalDate)))) {
                isValid = true;
            }
        }
        return isValid;
    }

    private LocalDate getDate(String strDate, DateTimeFormatter formatter) throws DateTimeParseException {
        log.debug(" Get Date strDate:{}, formatter:{}", strDate, formatter);
        LocalDate logDate = null;
        try {
            if (StringUtils.isNotBlank(strDate)) {
                logDate = parseDate(strDate, formatter);
            }
        } catch (DateTimeParseException ex) {
            log.error("Error while parsing logDate:{} is:{}", logDate, ex);
            return logDate;
        }
        return logDate;
    }

    private LocalDate parseDate(String date, DateTimeFormatter formatter) throws DateTimeParseException {
        log.debug(" Parse Date date:{}, formatter:{}", date, formatter);
        return LocalDate.parse(date, formatter);
    }

    /**
     * Parses a user-supplied date string in either ISO-8601 date-time format (e.g. yyyy-MM-ddTHH:mm:ssZ)
     * or legacy dd-MM-yyyy format. Returns the date part as LocalDate for range comparison.
     *
     * @param dateStr the date or date-time string from the API request
     * @param fallbackFormatter optional configured audit log date format (e.g. dd-MM-yyyy)
     * @return LocalDate for the given string, or null if dateStr is blank
     * @throws DateTimeParseException if the string cannot be parsed with any supported format
     */
    private LocalDate parseUserDate(String dateStr, DateTimeFormatter fallbackFormatter) throws DateTimeParseException {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        String trimmed = dateStr.trim();
        // Try ISO-8601 offset date-time (e.g. 2024-02-13T10:30:00Z, 2024-02-13T10:30:00+01:00)
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(trimmed, ISO_OFFSET_DATE_TIME);
            return zdt.toLocalDate();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try ISO local date-time (e.g. 2024-02-13T10:30:00)
        try {
            LocalDateTime ldt = LocalDateTime.parse(trimmed, ISO_LOCAL_DATE_TIME);
            return ldt.toLocalDate();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try ISO date only (e.g. 2024-02-13)
        try {
            return LocalDate.parse(trimmed, ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try configured audit format (e.g. dd-MM-yyyy)
        if (fallbackFormatter != null) {
            try {
                return LocalDate.parse(trimmed, fallbackFormatter);
            } catch (DateTimeParseException ignored) {
                // continue
            }
        }
        // Fallback to legacy dd-MM-yyyy
        DateTimeFormatter legacy = DateTimeFormatter.ofPattern(AUDIT_FILE_DATE_FORMAT);
        return LocalDate.parse(trimmed, legacy);
    }

}