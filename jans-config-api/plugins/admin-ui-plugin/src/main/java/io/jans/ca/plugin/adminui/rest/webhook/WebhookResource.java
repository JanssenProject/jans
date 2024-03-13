package io.jans.ca.plugin.adminui.rest.webhook;

import io.jans.as.persistence.model.Scope;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.webhook.AuiFeature;
import io.jans.ca.plugin.adminui.model.webhook.ShortCodeRequest;
import io.jans.ca.plugin.adminui.model.webhook.WebhookEntry;
import io.jans.ca.plugin.adminui.service.webhook.WebhookService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.python.google.common.collect.Sets;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;

import static io.jans.as.model.util.Util.escapeLog;

@Path("/admin-ui/webhook")
public class WebhookResource extends BaseResource {
    public static final String SCOPE_WEBHOOK_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.readonly";
    public static final String SCOPE_WEBHOOK_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.write";
    public static final String SCOPE_WEBHOOK_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.delete";
    public static final String WEBHOOK = "webhook";
    public static final String WEBHOOK_ID_PATH_VARIABLE = "/{webhookId}";
    public static final String FEATURE_ID_PATH_VARIABLE = "/{featureId}";
    public static final String TRIGGER_PATH = "/trigger";
    public static final String ADMIN_UI_FEATURES = "/features";
    @Inject
    Logger log;

    @Inject
    WebhookService webhookService;

    @Operation(summary = "Gets list of Admin UI features", description = "Gets list of Admin UI features", operationId = "get-all-features", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuiFeature.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/get-all-features.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(ADMIN_UI_FEATURES)
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAdminUIFeatures() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Fetching all Admin UI features");
            }
            List<AuiFeature> auiFeatures = webhookService.getAllAuiFeatures();

            return Response.ok(auiFeatures).build();
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Gets list of Admin UI features mapped to webhookId", description = "Gets list of Admin UI features mapped to webhookId", operationId = "get-features-by-webhook-id", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuiFeature.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/get-features-by-webhook-id.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(ADMIN_UI_FEATURES + WEBHOOK_ID_PATH_VARIABLE)
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAuiFeaturesByWebhookId(@Parameter(description = "Webhook identifier") @PathParam(AppConstants.WEBHOOK_ID) @NotNull String webhookId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Fetching all Admin UI features by webhookId");
            }
            List<AuiFeature> auiFeatures = webhookService.getAllAuiFeaturesByWebhookId(webhookId);

            return Response.ok(auiFeatures).build();
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Gets list of Admin UI Webhooks mapped to featureId", description = "Gets list of Admin UI Webhooks mapped to featureId", operationId = "get-webhooks-by-feature-id", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebhookEntry.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/get-webhook-by-feature-id.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(FEATURE_ID_PATH_VARIABLE)
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllWebhooksByFeatureId(@Parameter(description = "Feature identifier") @PathParam(AppConstants.ADMIN_UI_FEATURE_ID) @NotNull String featureId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Fetching all Admin UI webhooks by featureId");
            }
            List<WebhookEntry> webhooks = webhookService.getWebhooksByFeatureId(featureId);

            return Response.ok(webhooks).build();
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Gets list of webhooks", description = "Gets list of webhooks", operationId = "get-all-webhooks", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/get-all-webhooks.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWebhooks(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(AppConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "scopeType=spontaneous,defaultScope=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {
        try {
            if (log.isInfoEnabled()) {
                log.info("User search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                        escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                        escapeLog(sortOrder), escapeLog(fieldValuePair));
            }

            SearchRequest searchReq = createSearchRequest(AppConstants.WEBHOOK_DN, pattern, sortBy, sortOrder,
                    startIndex, limit, null, null, webhookService.getRecordMaxCount(), fieldValuePair, WebhookEntry.class);

            return Response.ok(this.doSearch(searchReq)).build();
        } catch (ApplicationException e) {
            log.error(e.getMessage(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Create Webhook", description = "Create Webhook", operationId = "post-webhook", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_WRITE}))
    @RequestBody(description = "Webhook object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebhookEntry.class), examples = @ExampleObject(name = "Request json example", value = "example/webhook/post-webhook-request.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Scope.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/post-webhook-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @POST
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response addWebhook(@Valid WebhookEntry webhook) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Webhook to be added - webhookEntry:{}", escapeLog(webhook.getDisplayName()));
            }

            WebhookEntry result = webhookService.addWebhook(webhook);
            log.debug("Id of newly added is {}", result.getInum());
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (ApplicationException e) {
            log.error(e.getMessage(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_SAVE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_SAVE_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Update Webhook", description = "Update Webhook", operationId = "put-webhook", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_WRITE}))
    @RequestBody(description = "Webhook object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebhookEntry.class), examples = @ExampleObject(name = "Request json example", value = "example/webhook/put-webhook.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebhookEntry.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/put-webhook.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @PUT
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWebhook(@Valid WebhookEntry webhook) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Webhook to be updated :{}", escapeLog(webhook.getInum()));
            }
            HashSet<String> webhookIdSet = Sets.newHashSet();
            webhookIdSet.add(webhook.getInum());
            List<WebhookEntry> existingWebhooks = webhookService.getWebhookByIds(webhookIdSet);
            if (existingWebhooks == null) {
                log.error(ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
            }
            if (!existingWebhooks.isEmpty()) {
                WebhookEntry existingWebhook = existingWebhooks.get(0);
                checkResourceNotNull(existingWebhook, WEBHOOK);
                webhook.setInum(existingWebhook.getInum());
                webhook.setDn(existingWebhook.getDn());
            }

            webhook = webhookService.updateWebhook(webhook);

            log.debug("Updated webhook:{}", webhook.getInum());
            return Response.ok(webhook).build();
        } catch (ApplicationException e) {
            log.error(e.getMessage(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_UPDATE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_SAVE_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Delete Webhook", description = "Delete Webhook", operationId = "delete-Webhook-by-inum", tags = {"Admin UI - Webhooks"},
            security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_DELETE}))
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @DELETE
    @Path(WEBHOOK_ID_PATH_VARIABLE)
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_DELETE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteWebhook(@Parameter(description = "Webhook identifier") @PathParam(AppConstants.WEBHOOK_ID) @NotNull String webhookId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Webhook to be deleted - webhookId:{}", escapeLog(webhookId));
            }
            HashSet<String> webhookIdSet = Sets.newHashSet();
            webhookIdSet.add(webhookId);
            List<WebhookEntry> result = webhookService.getWebhookByIds(webhookIdSet);
            if (result == null) {
                log.error(ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
            }
            if (!result.isEmpty()) {
                WebhookEntry existingWebhook = result.get(0);
                checkResourceNotNull(existingWebhook, WEBHOOK);
                webhookService.removeWebhook(existingWebhook);
            }
            log.debug("Webhook is deleted");
            return Response.noContent().build();
        } catch (ApplicationException e) {
            log.error(e.getMessage(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_DELETE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.WEBHOOK_DELETE_ERROR.getDescription()))
                    .build();
        }
    }

    @Operation(summary = "Trigger webhooks mapped to featureId", description = "Trigger webhooks mapped to featureId", operationId = "trigger-webhook", tags = {
            "Admin UI - Webhooks"}, security = @SecurityRequirement(name = "oauth2", scopes = {SCOPE_WEBHOOK_READ}))
    @RequestBody(description = "Webhook object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShortCodeRequest.class), examples = @ExampleObject(name = "Request json example", value = "example/webhook/trigger-webooks-request.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuiFeature.class), examples = @ExampleObject(name = "Response json example", value = "example/webhook/trigger-webooks-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @POST
    @Path(TRIGGER_PATH + FEATURE_ID_PATH_VARIABLE)
    @ProtectedApi(scopes = {SCOPE_WEBHOOK_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerWebhook(@Parameter(description = "Admin UI feature identifier") @PathParam(AppConstants.ADMIN_UI_FEATURE_ID) @NotNull String featureId,
                                   @Valid @NotNull List<ShortCodeRequest> shortCodes) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Triggering all webhooks for Admin UI feature - featureId: {}", escapeLog(featureId));
            }
            HashSet<String> featureIdSet = Sets.newHashSet();
            featureIdSet.add(featureId);
            List<AuiFeature> featureList = webhookService.getAuiFeaturesByIds(featureIdSet);
            if (CollectionUtils.isEmpty(featureList)) {
                log.error(ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
            }
            AuiFeature featureObj = featureList.get(0);
            if (CollectionUtils.isEmpty(featureObj.getWebhookIdsMapped())) {
                log.error(ErrorResponse.NO_WEBHOOK_FOUND.getDescription());
                throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.NO_WEBHOOK_FOUND.getDescription());
            }
            List<GenericResponse> responseList = webhookService.triggerEnabledWebhooks(Sets.newHashSet(featureObj.getWebhookIdsMapped()), shortCodes);

            return Response.ok(responseList).build();
        } catch (ApplicationException e) {
            log.error(e.getMessage(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.FETCH_DATA_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.FETCH_DATA_ERROR.getDescription()))
                    .build();
        }
    }

    private PagedResult<WebhookEntry> doSearch(SearchRequest searchReq) throws ApplicationException {
        if (log.isDebugEnabled()) {
            log.debug("Webhook search params - searchReq:{} ", searchReq);
        }

        PagedResult<WebhookEntry> pagedResult = webhookService.searchWebhooks(searchReq);
        if (log.isTraceEnabled()) {
            log.trace("PagedResult  - pagedResult:{}", pagedResult);
        }

        if (pagedResult != null) {
            log.debug(
                    "Webhook fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }
        log.debug("Webhook  - pagedResult:{}", pagedResult);
        return pagedResult;
    }

}
