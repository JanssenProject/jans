package io.jans.configapi.rest.resource.auth;


import io.jans.ads.model.Deployment;
import io.jans.configapi.core.util.ApiErrorResponse;
import io.jans.config.GluuConfiguration;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.AgamaDeploymentsService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.LdapConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.model.PagedResult;
import io.jans.service.custom.CustomScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class AuditLoggerResource {
    
    public static final String AUDIT_LOGGING_READ_SCOPE = "logging.read";
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");
    static final String AUDIT = "/audit";

   
    @Inject
    Logger log;
    
    @Operation(summary = "Get audit details.", description = "Get audit details.", operationId = "get-audit", tags = {
    "Logs" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.AUDIT_LOGGING_READ_SCOPE }))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    //@ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS }, groupScopes = {
    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
public Response getAttributes(
    @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
    @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
    @Parameter(description = "Status of the attribute") @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
    @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
    @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
    @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
    @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "adminCanEdit=true,dataType=string")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair)
{

if (log.isDebugEnabled()) {
    log.debug(
            "Search Attribute filters with limit:{}, pattern:{}, status:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
            escapeLog(limit), escapeLog(pattern), escapeLog(status), escapeLog(startIndex), escapeLog(sortBy),
            escapeLog(sortOrder), escapeLog(fieldValuePair));
}
readLogFile();


}

public String readLogFile() {
    StringBuilder logContent = new StringBuilder();
    String filePath = "/path/to/your/logs/" + fileName; // Adjust path as needed

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line;
        while ((line = reader.readLine()) != null) {
            logContent.append(line).append("\n");
        }
    } catch (IOException e) {
        return "Error reading log file: " + e.getMessage();
    }
    return logContent.toString();
}

}
