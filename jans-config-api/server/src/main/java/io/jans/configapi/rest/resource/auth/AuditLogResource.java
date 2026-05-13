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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.AUDIT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditLogResource extends ConfigBaseResource {

    public static final String AUDIT_FILE_PATH = "/opt/jans/jetty/jans-config-api/logs/";
    public static final String AUDIT_FILE_NAME = "configapi-audit.log";
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
    public static final String AUDIT_FILE_ISO_OFFSET_DATE_TIME = "dd-MM-yyyy HH:mm:ss.SSS";
    public static final String PARAM_DATE_ISO_OFFSET_DATE_TIME = "dd-MM-yyyy'T'HH:mm:ss.SSS'Z'";

    static final String AUDIT = "/audit";

    private class LogPagedResult extends PagedResult<String> {
    };

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    /**
     * Searches and returns paginated audit log entries filtered by an optional
     * pattern and date range.
     *
     * @param pattern    a substring to filter log lines; blank returns all entries
     * @param startIndex the 1-based index of the first result to return
     * @param limit      maximum number of results to return
     * @param startDate  optional start date (dd-MM-yyyy) to include entries on or
     *                   after this date
     * @param endDate    optional end date (dd-MM-yyyy) to include entries on or
     *                   before this date
     * @return a HTTP 200 Response containing a LogPagedResult with the matching log
     *         lines and pagination metadata
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
            @Parameter(description = "Start date/time for the log entries report. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. dd-MM-yyyy'T'HH:mm:ss.SSS'Z').", schema = @Schema(type = "string")) @QueryParam(value = "start_date") String startDate,
            @Parameter(description = "End date/time for the log entries. Accepted: dd-MM-yyyy or ISO-8601 date-time (e.g. dd-MM-yyyy'T'HH:mm:ss.SSS'Z').", schema = @Schema(type = "string")) @QueryParam(value = "end_date") String endDate)

    {
        if (log.isInfoEnabled()) {
            log.info("Search Attribute filters with pattern:{}, startIndex:{}, limit:{}, startDate:{}, endDate:{}",
                    escapeLog(pattern), escapeLog(startIndex), escapeLog(limit), escapeLog(startDate),
                    escapeLog(endDate));
        }

        return Response.ok(this.doSearch(getPattern(pattern), startIndex, limit, startDate, endDate)).build();
    }

    /* Helper methods */

    private List<String> getLogEntries(String file) {

        if (log.isDebugEnabled()) {
            log.debug("Fetch log file:{}", file);
        }

        List<String> logEntries = new ArrayList<>();

        try (Stream<String> stream = Files.lines(java.nio.file.Path.of(file))) {
            logEntries = stream.collect(Collectors.toList());
        } catch (IOException ex) {
            throwInternalServerException(" Error while fetching logs", ex);
        }
        return logEntries;

    }

    private LogPagedResult doSearch(String pattern, int startIndex, int limit, String startDate, String endDate) {
        if (log.isDebugEnabled()) {
            log.debug("Search Attribute filters with pattern:{}, startIndex:{}, limit:{}, startDate:{}, endDate:{}",
                    escapeLog(pattern), escapeLog(startIndex), escapeLog(limit), escapeLog(startDate),
                    escapeLog(endDate));
        }

        // Get data based on pattern and date filter
        List<String> logEntriesList = getLogEntries(getAuditLogFile());
        log.trace("Log fetched - logEntriesList:{}", logEntriesList);

        // pattern filter
        logEntriesList = filterLogByPattern(logEntriesList, pattern);
        log.trace("Log fetched - logEntriesList:{}", logEntriesList);

        // Date filter
        logEntriesList = filterLogByDate(logEntriesList, startDate, endDate);

        LogPagedResult logPagedResult = getLogPagedResult(logEntriesList, startIndex, limit);
        log.debug("Audit Log PagedResult:{}", logPagedResult);
        return logPagedResult;

    }

    private List<String> filterLogByPattern(List<String> logEntriesList, String strPattern) {

        if (log.isDebugEnabled()) {
            log.debug("Fetch log logEntriesList:{}, strPattern:{}", logEntriesList, escapeLog(strPattern));
        }

        if (StringUtils.isBlank(strPattern)) {
            return logEntriesList;
        }

        Pattern pattern = null;
        try {
            pattern = Pattern.compile(strPattern);
        } catch (PatternSyntaxException pse) {
            log.error("Invalid pattern: {}", escapeLog(strPattern), pse);
            throwBadRequestException("Invalid search pattern syntax");
            return logEntriesList;
        }
        if (log.isDebugEnabled()) {
            log.debug(" strPattern:{}, pattern:{}", escapeLog(strPattern), pattern);
        }

        List<String> logEntriesfilterByPattern = logEntriesList.stream().filter(pattern.asPredicate())
                .collect(Collectors.toList());
        log.debug(" logEntriesfilterByPattern:{}", logEntriesfilterByPattern);
        return logEntriesfilterByPattern;
    }

    private String getPattern(String strPattern) {
        String searchPattern = ApiConstants.DEFAULT_SEARCH_PATTERN;
        if (StringUtils.isNotBlank(strPattern)) {

            // Limit input length first
            if (strPattern.length() > 100) {
                throwBadRequestException(strPattern);
            }
            searchPattern = Pattern.quote(strPattern);
        }

        searchPattern = "^.*" + searchPattern + ".*$";
        if (log.isDebugEnabled()) {
            log.debug("Audit searchPattern:{}", searchPattern);
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
                if (log.isDebugEnabled()) {
                    log.debug("Final startIndex:{}, limit:{}, toIndex:{}", escapeLog(startIndex), escapeLog(limit),
                            escapeLog(toIndex));
                }

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

        // Validate & parse
        validateDate(startDate, endDate);
        LocalDateTime startLocal = StringUtils.isNotBlank(startDate) ? parseDate(startDate) : null;
        LocalDateTime endLocal = StringUtils.isNotBlank(endDate) ? parseDate(endDate) : null;
        if (StringUtils.isNotBlank(endDate) && !endDate.contains("T") && !endDate.contains(" ")) {
            endLocal = endLocal.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        }

        List<String> filteredLogEntries = new ArrayList<>();
        try {
            String datePattern = (StringUtils.isNotBlank(getAuditDateFormat()) ? getAuditDateFormat()
                    : AUDIT_FILE_ISO_OFFSET_DATE_TIME);
            log.debug("datePattern:{}", datePattern);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern);

            for (int i = 0; i < logEntries.size(); i++) {
                String line = logEntries.get(i);
                if (StringUtils.isNotBlank(line) && line.length() > datePattern.length()) {
                    String timestampPart = line.substring(0, datePattern.length());
                    log.debug("{}, timestampPart:{} , {}", "\n\n", timestampPart, "\n\n");
                    LocalDateTime logEntryLocalDate = getDate(timestampPart, formatter);
                    log.debug("{}, timestampPart:{}, logEntryLocalDate:{}, {}", "\n\n", timestampPart,
                            logEntryLocalDate, "\n\n");
                    if (isValidlogEntry(startLocal, endLocal, logEntryLocalDate)) {
                        filteredLogEntries.add(line);
                    }
                }
            }
        } catch (DateTimeParseException ex) {
            log.error("Error while filtering log file with startDate:{} and endDate:{} is:{}", escapeLog(startDate),
                    escapeLog(endDate), ex);
            return logEntries;
        }
        return filteredLogEntries;
    }

    private void validateDate(String startDate, String endDate) {
        if (log.isDebugEnabled()) {
            log.debug(" Validate Date startDate:{}, endDate:{}", escapeLog(startDate), escapeLog(endDate));
        }

        StringBuilder sb = new StringBuilder();
        LocalDateTime startLocal = null;
        LocalDateTime endLocal = null;

        // validate startDate
        if (StringUtils.isNotBlank(startDate)) {
            try {
                startLocal = parseDate(startDate);
                if (log.isDebugEnabled()) {
                    log.debug(" startLocal:{} {}", escapeLog(startLocal), "\n\n");
                }
            } catch (DateTimeParseException dtpe) {
                sb.append(
                        "Start date is not valid. Use dd-MM-yyyy or ISO-8601 date-time (e.g. dd-MM-yyyy'T'HH:mm:ss.SSS'Z').");
            }
        }

        // validate endDate
        if (StringUtils.isNotBlank(endDate)) {
            try {
                endLocal = parseDate(endDate);
                if (log.isDebugEnabled()) {
                    log.debug(" endLocal:{} {}", escapeLog(endLocal), "\n\n");
                }
            } catch (DateTimeParseException dtpe) {
                sb.append(
                        "End date is not valid. Use dd-MM-yyyy or ISO-8601 date-time (e.g. dd-MM-yyyy'T'HH:mm:ss.SSS'Z').");
            }
        }

        if (startLocal != null && endLocal != null && startLocal.isAfter(endLocal)) {
            sb.append("Start date must be before or equal to end date.");
        }

        log.debug(" validateDate error:{} {}", sb, "\n\n");
        if (sb.length() > 0) {
            throwBadRequestException(sb.toString(), "INVALID_DATE");
        }
    }

    private boolean isValidlogEntry(LocalDateTime startLocalDate, LocalDateTime endLocalDate,
            LocalDateTime logEntryLocalDate) {
        if (log.isDebugEnabled()) {
            log.debug(" startLocalDate:{}, endLocalDate:{}, logEntryLocalDate:{}", escapeLog(startLocalDate),
                    escapeLog(endLocalDate), logEntryLocalDate);
        }

        boolean isValid = false;

        if (logEntryLocalDate != null) {
            if (startLocalDate != null && endLocalDate != null
                    && ((logEntryLocalDate.isEqual(startLocalDate) || logEntryLocalDate.isAfter(startLocalDate))
                            && (logEntryLocalDate.isEqual(endLocalDate) || logEntryLocalDate.isBefore(endLocalDate)))) {
                isValid = true;
            } else if ((startLocalDate != null && endLocalDate == null
                    && (logEntryLocalDate.isEqual(startLocalDate) || logEntryLocalDate.isAfter(startLocalDate)))

                    || (startLocalDate == null && endLocalDate != null
                            && (logEntryLocalDate.isEqual(endLocalDate) || logEntryLocalDate.isBefore(endLocalDate)))) {
                isValid = true;
            }
        }
        return isValid;
    }

    private LocalDateTime getDate(String strDate, DateTimeFormatter formatter) throws DateTimeParseException {
        if (log.isDebugEnabled()) {
            log.debug(" Get Date strDate:{}, formatter:{}", escapeLog(strDate), formatter);
        }

        LocalDateTime logDateTime = null;
        try {
            if (StringUtils.isNotBlank(strDate)) {
                logDateTime = parseDate(strDate, formatter);
            }
        } catch (DateTimeParseException ex) {
            log.error("Error while parsing logDateTime:{} is:{}", logDateTime, ex);
            return logDateTime;
        }
        return logDateTime;
    }

    private LocalDateTime parseDate(String date, DateTimeFormatter formatter) throws DateTimeParseException {
        if (log.isDebugEnabled()) {
            log.debug(" Parse Date date:{}, formatter:{}", escapeLog(date), formatter);
        }
        return LocalDateTime.parse(date, formatter);
    }

    /**
     * Parses a user-supplied date string in either ISO-8601 date-time format (e.g.
     * dd-MM-yyyy'T'HH:mm:ss.SSS'Z') or legacy dd-MM-yyyy format. Returns the date
     * part as LocalDateTime for range comparison.
     *
     * @param dateStr the date or date-time string from the API request
     *
     * @return LocalDateTime for the given string, or null if dateStr is blank
     * @throws DateTimeParseException if the string cannot be parsed with any
     *                                supported format
     */
    private LocalDateTime parseDate(String dateStr) throws DateTimeParseException {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        String trimmed = dateStr.trim();
        DateTimeFormatter dateTimeFormatter = this.getDateTimeFormatter(trimmed);

        if (log.isInfoEnabled()) {
            log.info(" Parse Date dateStr:{}, dateTimeFormatter:{}", escapeLog(dateStr), dateTimeFormatter);
        }
        if (dateTimeFormatter != null) {
            try {
                return LocalDateTime.parse(trimmed, dateTimeFormatter);
            } catch (DateTimeParseException dtpe) {
                // continue
            }
        }

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
        return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    private DateTimeFormatter getDateTimeFormatter(String dateStr) {
        if (log.isDebugEnabled()) {
            log.debug(" Get DateTimeFormatter for dateStr:{} ", escapeLog(dateStr));
        }

        if (log.isDebugEnabled()) {
            log.debug(" Get DateTimeFormatter for dateStr:{}, dateStr.length():{} ", escapeLog(dateStr),
                    dateStr.length());
        }

        if (dateStr.length() == DEFAULT_DATE_FORMAT.length()) {
            return new DateTimeFormatterBuilder().append(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT))
                    .parseDefaulting(HOUR_OF_DAY, 0).parseDefaulting(MINUTE_OF_HOUR, 0)
                    .parseDefaulting(SECOND_OF_MINUTE, 0).toFormatter();

        } else if (dateStr.length() == AUDIT_FILE_ISO_OFFSET_DATE_TIME.length()) {
            return DateTimeFormatter.ofPattern(AUDIT_FILE_ISO_OFFSET_DATE_TIME);
        } else {
            return DateTimeFormatter.ofPattern(PARAM_DATE_ISO_OFFSET_DATE_TIME);
        }
    }
}