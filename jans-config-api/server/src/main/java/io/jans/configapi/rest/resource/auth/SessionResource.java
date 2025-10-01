/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.common.model.session.SessionId;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.SessionService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.jans.as.model.util.Util.escapeLog;

import java.util.List;

import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.SESSION)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SessionResource extends ConfigBaseResource {

    private class SessionPagedResult extends PagedResult<SessionId> {
    };

    @Inject
    Logger log;

    @Inject
    SessionService sessionService;

    @Operation(summary = "Return all session", description = "Return all session", operationId = "get-sessions", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS, "revoke_session" }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SessionPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/session/get-session.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAllSessions() {

        SearchRequest searchReq = createSearchRequest(sessionService.getDnForSession(null), null, ApiConstants.JANSID,
                ApiConstants.ASCENDING, Integer.parseInt(ApiConstants.DEFAULT_LIST_START_INDEX),
                Integer.parseInt(ApiConstants.DEFAULT_LIST_SIZE), null, null, this.getMaxCount(), null,
                SessionId.class);

        SessionPagedResult sessionPagedResult = searchSession(searchReq);
        logger.info("Session fetched sessionPagedResult:{}", sessionPagedResult);
        return Response.ok(sessionPagedResult).build();
    }

    @Operation(summary = "Search session", description = "Search session", operationId = "search-session", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS, "revoke_session" }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SessionPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/session/search-session.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.SEARCH)
    public Response searchSessionEntries(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.JANSID) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "userDn=d5552516-4436-4908-ab36-3e9725246304,expirationDate>2025-09-25,expirationDate<2026-10-15")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Session serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }

        SearchRequest searchReq = createSearchRequest(sessionService.getDnForSession(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount(), fieldValuePair, SessionId.class);

        SessionPagedResult sessionPagedResult = searchSession(searchReq);
        logger.info("Session fetched based on name are:{}", sessionPagedResult);
        return Response.ok(sessionPagedResult).build();

    }

    @Operation(summary = "Get session by id.", description = "Get session by id.", operationId = "get-session-by-id", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS, "revoke_session" }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SessionId.class), examples = @ExampleObject(name = "Response example", value = "example/token/get-session-by-id.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.SID_PATH + ApiConstants.SID_PATH_PARAM)
    public Response getSessionById(
            @Parameter(description = "Session identifier.") @PathParam(ApiConstants.SID) @NotNull String sid) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete session identified by sid:{}", escapeLog(sid));
        }
        checkResourceNotNull(sid, ApiConstants.SID);
        final SessionId session = sessionService.getSessionBySid(sid);
        logger.debug("session:{}", session);
        return Response.ok(session).build();
    }

    @Operation(summary = "Revoke all sessions by userDn", description = "Revoke all sessions by userDn", operationId = "revoke-user-session", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS, ApiAccessConstants.JANS_AUTH_REVOKE_SESSION }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS,
            ApiAccessConstants.JANS_AUTH_REVOKE_SESSION }, groupScopes = {}, superScopes = {
                    ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.USER + ApiConstants.USERDN_PATH)
    public Response deleteUsersSession(
            @Parameter(description = "User domain name") @PathParam(ApiConstants.USERDN) @NotNull String userDn) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete session by userDn:{}", escapeLog(userDn));
        }
        checkResourceNotNull(userDn, ApiConstants.USERDN);
        sessionService.revokeUserSession(userDn);
        return Response.ok().build();
    }

    @Operation(summary = "Delete a session.", description = "Delete a session.", operationId = "delete-session", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS, ApiAccessConstants.JANS_AUTH_REVOKE_SESSION }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS,
            ApiAccessConstants.JANS_AUTH_REVOKE_SESSION }, groupScopes = {}, superScopes = {
                    ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.SID_PATH + ApiConstants.SID_PATH_PARAM)
    public Response deleteSessionBySid(
            @Parameter(description = "Session identifier.") @PathParam(ApiConstants.SID) @NotNull String sid) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete session identified by sid:{}", escapeLog(sid));
        }
        checkResourceNotNull(sid, ApiConstants.SID);

        sessionService.revokeSessionBySid(sid);
        return Response.ok().build();
    }

    private SessionPagedResult searchSession(SearchRequest searchReq) {

        logger.debug("Search Token by name params - searchReq:{} ", searchReq);
        SessionPagedResult sessionPagedResult = null;
        PagedResult<SessionId> pagedResult = sessionService.searchSession(searchReq);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Token fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            sessionPagedResult = getSessionPagedResult(pagedResult);
        }

        logger.debug("sessionPagedResult:{} ", sessionPagedResult);
        return sessionPagedResult;
    }

    private SessionPagedResult getSessionPagedResult(PagedResult<SessionId> pagedResult) {
        SessionPagedResult sessionPagedResult = null;
        if (pagedResult != null) {
            List<SessionId> sessionList = pagedResult.getEntries();
            sessionPagedResult = new SessionPagedResult();
            sessionPagedResult.setStart(pagedResult.getStart());
            sessionPagedResult.setEntriesCount(pagedResult.getEntriesCount());
            sessionPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            sessionPagedResult.setEntries(sessionList);
        }
        return sessionPagedResult;
    }

}
