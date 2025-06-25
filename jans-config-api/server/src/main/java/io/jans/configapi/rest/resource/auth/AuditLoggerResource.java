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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.AUDIT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditLoggerResource extends ConfigBaseResource {

    public static final String AUDIT_FILE_PATH = "/opt/jans/jetty/jans-config-api/logs/";
    public static final String AUDIT_FILE_NAME = "configapi-audit.log";
    public static final String AUDIT_LOGGING_READ_SCOPE = "logging.read";
    static final String AUDIT = "/audit";

    private class LogPagedResult extends PagedResult<String> {
    };

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Operation(summary = "Get audit details.", description = "Get audit details.", operationId = "get-audit-data", tags = {
            "Logs" }, security = @SecurityRequirement(name = "oauth2", scopes = { AUDIT_LOGGING_READ_SCOPE }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LogPagedResult.class), examples = @ExampleObject(name = "Response example"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUDIT_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_AUDIT_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getLogsEnteries(
            @Parameter(description = "Search pattern") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit) {
        if (log.isDebugEnabled()) {
            log.debug("Search Attribute filters with pattern:{}, startIndex:{}, limit:{}", escapeLog(pattern),
                    escapeLog(startIndex), escapeLog(limit));
        }
        return Response.ok(this.doSearch(getPattern(pattern), startIndex, limit)).build();
    }

    private LogPagedResult doSearch(String pattern, int startIndex, int limit) {

        log.info("Fetch log pattern:{}, startIndex:{}, limit:{}", pattern, startIndex, limit);

        List<String> logEntriesList = getLogEntries(getAuditLogFile(), pattern);
        log.debug("Log fetched  - logEntriesList:{}", logEntriesList);

        LogPagedResult logPagedResult = getLogPagedResult(logEntriesList, startIndex, limit);
        log.info("Audit Log PagedResult:{}", logPagedResult);
        return logPagedResult;

    }

    private List<String> getLogEntries(String file, String pattern) {
        log.error("Fetch log file:{}, pattern:{}", file, pattern);

        List<String> logEntries = new ArrayList<>();
        try {
            logEntries = Files.lines(java.nio.file.Path.of(file)).filter(s -> s.matches(pattern))
                    .collect(Collectors.toList());
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
        log.info("Audit logEntriesList:{}, startIndex:{}, limit:{}", logEntriesList, startIndex, limit);

        LogPagedResult logPagedResult = new LogPagedResult();
        if (logEntriesList != null && !logEntriesList.isEmpty()) {

            try {

                // verify start and limit index
                getStartIndex(logEntriesList, startIndex);
                limit = (limit <= logEntriesList.size()) ? limit : logEntriesList.size();
                log.info("Final startIndex:{}, limit:{}", startIndex, limit);

                // Extract paginated data
                List<String> sublist = logEntriesList.subList(startIndex, limit);

                logPagedResult.setStart(startIndex);
                logPagedResult.setEntriesCount(limit);
                logPagedResult.setTotalEntriesCount(logEntriesList.size());
                logPagedResult.setEntries(sublist);

            } catch (IndexOutOfBoundsException ioe) {
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
        log.info("Get startIndex logEntriesList:{}, startIndex:{}", logEntriesList, startIndex);

        if (logEntriesList != null && !logEntriesList.isEmpty()) {

            try {
                logEntriesList.get(startIndex);
            } catch (IndexOutOfBoundsException ioe) {
                throwBadRequestException("Page start index incorrect, total entries:{" + logEntriesList.size()
                        + "}, but provided:{" + startIndex + "} ");

            }
        }
        return startIndex;
    }

}
