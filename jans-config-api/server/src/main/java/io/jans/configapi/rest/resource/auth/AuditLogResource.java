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
    public static final String AUDIT_FILE_DATE_FORMAT = "dd-MM-YYYY";

    static final String AUDIT = "/audit";

    private class LogPagedResult extends PagedResult<String> {
    };

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Operation(summary = "Get audit details.", description = "Get audit details.", operationId = "get-audit-data", tags = {
            "Logs" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.LOGGING_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LogPagedResult.class), examples = @ExampleObject(name = "Response example"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.LOGGING_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.LOGGING_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getLogsEnteries(
            @Parameter(description = "Search pattern") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Start-Date for which the log entries report is to be fetched") @QueryParam(value = "start_date") String startDate,
            @Parameter(description = "End-Date for which the log entries is to be fetched") @QueryParam(value = "end_date") String endDate)

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
        logEntriesList = filterLogByDateTime(logEntriesList, startDate, endDate);
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
            log.debug("Audit logEntriesList:{}, startIndex:{}, limit:{}", logEntriesList, startIndex, limit);
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
            log.debug("Get startIndex logEntriesList:{}, startIndex:{}", logEntriesList, startIndex);
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

    private List<String> filterLogByDateTime(List<String> logEntries, String startDate, String endDate) {
        log.debug(" logEntries:{}, startDate:{}, endDate:{} ", logEntries, startDate, endDate);

        if (logEntries == null || logEntries.isEmpty()
                || (StringUtils.isBlank(startDate) && StringUtils.isBlank(endDate))) {
            return logEntries;
        }
        List<String> filteredLogEntries = new ArrayList<>();
        try {
            String datePattern = (StringUtils.isNotBlank(getAuditDateFormat()) ? getAuditDateFormat()
                    : AUDIT_FILE_DATE_FORMAT);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern);
            validateDate(startDate, endDate, formatter);

            LocalDateTime startDateTime = parseDate(startDate, formatter);
            LocalDateTime endDateTime = parseDate(endDate, formatter);

            for (int i = 0; i < logEntries.size(); i++) {
                String line = logEntries.get(i);
                String timestampPart = line.substring(0, datePattern.length());
                LocalDateTime logDateTime = LocalDateTime.parse(timestampPart, formatter);
                log.info(" \n\n logDateTime:{}, startDateTime:{}, endDateTime:{} ", logDateTime, startDateTime, endDateTime);

                if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)
                        && ( (logDateTime.isEqual(startDateTime) || logDateTime.isAfter(startDateTime)) 
                                && (logDateTime.isEqual(endDateTime) || logDateTime.isBefore(endDateTime)))
                                ) {
                    filteredLogEntries.add(line);
                } else if (StringUtils.isNotBlank(startDate) && StringUtils.isBlank(endDate)
                        && (logDateTime.isEqual(startDateTime) || logDateTime.isAfter(startDateTime))) {
                    filteredLogEntries.add(line);
                } else if (StringUtils.isBlank(startDate) && StringUtils.isNotBlank(endDate)
                        && (logDateTime.isEqual(endDateTime) || logDateTime.isBefore(endDateTime))) {
                    filteredLogEntries.add(line);
                }

            }
        } catch (Exception ex) {
            log.error("Error while filtering log file with startDate:{} and endDate:{} is:{}", startDate, endDate, ex);
            return logEntries;
        }
        return filteredLogEntries;
    }

    private void validateDate(String startDate, String endDate, DateTimeFormatter formatter) {
        log.info(" Validate Date startDate:{}, endDate:{}, formatter:{}", startDate, endDate, formatter);

        StringBuilder sb = new StringBuilder();
        // validate startDate
        if (StringUtils.isNotBlank(startDate)) {
            try {
                parseDate(startDate, formatter);
            } catch (DateTimeParseException dtpe) {

                sb.append("Start date is not valid, date:{" + startDate + "} whereas valid format is:{"
                        + formatter.toString() + "}");
            }
        }

        // validate endDate
        if (StringUtils.isNotBlank(endDate)) {
            try {
                parseDate(endDate, formatter);
            } catch (DateTimeParseException dtpe) {

                sb.append("Start date is not valid, date:{" + startDate + "} whereas valid format is:{"
                        + formatter.toString() + "}");
            }
        }

        if (sb.toString().length() >= 0) {
            throwBadRequestException(sb.toString(), "INVALID_DATE");
        }
    }

    private LocalDateTime parseDate(String date, DateTimeFormatter formatter) throws DateTimeParseException {
        log.info(" Parse Date date:{}, formatter:{}", date, formatter);

        LocalDateTime localDateTime = null;
        try {
            localDateTime = LocalDateTime.parse(date, formatter);
        } catch (DateTimeParseException dtpe) {
            log.error("Error while parsing date:{} is:{}", date, dtpe);
            throw dtpe;
        }
        return localDateTime;
    }

}
