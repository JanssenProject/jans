package io.jans.ca.plugin.adminui.rest.adminui;

import com.fasterxml.jackson.annotation.JsonView;
import io.jans.ca.plugin.adminui.model.adminui.AdminUIPolicyStore;
import io.jans.ca.plugin.adminui.model.adminui.PolicyStoreViews;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.adminui.AdminUISecurityService;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.slf4j.Logger;

import static io.jans.as.model.util.Util.escapeLog;

@Path("/admin-ui/security")
public class AdminUISecurityResource extends BaseResource {
    static final String POLICY_STORE = "policyStore";
    public static final String POLICY_STORE_INUM = "/{INUM}";
    static final String SYNC_ROLE_SCOPES_MAPPING = "/syncRoleScopesMapping";
    static final String SECURITY_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/security.readonly";
    static final String SECURITY_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/security.write";

    @Inject
    Logger log;

    @Inject
    AdminUISecurityService adminUISecurityService;

    @Operation(summary = "Get Admin UI policy store", description = "Get Admin UI policy store", operationId = "get-adminui-policy-store", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminUIPolicyStore.class, description = "Get Admin UI policy store")), examples = @ExampleObject(name = "Response json example", value = "example/adminui/security/get-all-policy-store-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(POLICY_STORE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_READ}, groupScopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response getPolicyStore(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(AppConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "scopeType=spontaneous,defaultScope=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair
    ) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Policy Store search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                        escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                        escapeLog(sortOrder), escapeLog(fieldValuePair));
            }
            SearchRequest searchReq = createSearchRequest(AppConstants.POLICY_STORE_DN, pattern, sortBy, sortOrder,
                    startIndex, limit, null, null, adminUISecurityService.getRecordMaxCount(), fieldValuePair, AdminUIPolicyStore.class);
            log.info("Policy Store received.");
            return Response.ok(this.doSearch(searchReq)).build();

        } catch (ApplicationException e) {
            log.error(ErrorResponse.POLICY_STORE_GET_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_GET_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    @Operation(summary = "Create Admin UI Policy Store", description = "Create Admin UI Policy Store", operationId = "create-adminui-policy-store", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_WRITE}))
    @RequestBody(description = "Policy Store Object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminUIPolicyStore.class), examples = @ExampleObject(name = "Request json example", value = "example/adminui/security/create-policy-store-request.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Create Admin UI policy store")), examples = @ExampleObject(name = "Response json example", value = "example/adminui/security/create-policy-store-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @POST
    @Path(POLICY_STORE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response createPolicyStore(@JsonView(PolicyStoreViews.Create.class) @Valid @NotNull AdminUIPolicyStore adminUIPolicyStore) {
        try {
            log.info("Create Admin UI Policy Store.");
            GenericResponse response = adminUISecurityService.uploadPolicyStore(adminUIPolicyStore);
            log.info("Successfully created Admin UI Policy Store");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.POLICY_STORE_UPLOAD_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_UPLOAD_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    @Operation(summary = "Edit Admin UI Policy Store", description = "Edit Admin UI Policy Store", operationId = "edit-adminui-policy-store", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_WRITE}))
    @RequestBody(description = "Policy Store Object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminUIPolicyStore.class), examples = @ExampleObject(name = "Request json example", value = "example/adminui/security/edit-policy-store-request.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Edit Admin UI policy store")), examples = @ExampleObject(name = "Response json example", value = "example/adminui/security/edit-policy-store-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Not Found"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @PUT
    @Path(POLICY_STORE + POLICY_STORE_INUM)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response editPolicyStore(
            @Parameter(description = "Policy store inum") @PathParam("INUM") @NotNull String inum,
            @JsonView(PolicyStoreViews.Edit.class) @Valid @NotNull AdminUIPolicyStore adminUIPolicyStore) {
        try {
            log.info("Edit Admin UI Policy Store: {}", escapeLog(inum));
            GenericResponse response = adminUISecurityService.editPolicyStore(inum, adminUIPolicyStore);
            log.info("Successfully edited Admin UI Policy Store");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.POLICY_STORE_UPDATE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_UPDATE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    @Operation(summary = "Delete Admin UI Policy Store", description = "Delete Admin UI Policy Store", operationId = "delete-adminui-policy-store", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_WRITE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Delete Admin UI policy store")), examples = @ExampleObject(name = "Response json example", value = "example/adminui/security/delete-policy-store-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Not Found"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @DELETE
    @Path(POLICY_STORE + POLICY_STORE_INUM)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response deletePolicyStore(
            @Parameter(description = "Policy store inum") @PathParam("INUM") @NotNull String inum) {
        try {
            log.info("Delete Admin UI Policy Store: {}", escapeLog(inum));
            GenericResponse response = adminUISecurityService.deletePolicyStore(inum);
            log.info("Successfully deleted Admin UI Policy Store");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.POLICY_STORE_DELETE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_DELETE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    @Operation(summary = "Sync role-to-scope mappings from the policy store", description = "Sync the role-to-scope mappings from the policy store. If a remote policy store URL is configured and enabled, the mappings will be generated from the remote policy store; otherwise, they will be generated from the default policy store.", operationId = "sync-role-to-scopes-mappings", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_WRITE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Sync Role-to-Scopes mapping from policy-store")), examples = @ExampleObject(name = "Response json example", value = "example/adminui/security/sync-role-scopes-mapping-response.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @POST
    @Path(SYNC_ROLE_SCOPES_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response syncRoleScopeMapping() {
        try {
            log.info("Sync Role-to-Scopes mappings from the policy-store.");
            GenericResponse response = adminUISecurityService.syncRoleScopeMapping();
            log.info("Sync Role-to-Scopes mappings from the policy-store completed");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    private PagedResult<AdminUIPolicyStore> doSearch(SearchRequest searchReq) throws ApplicationException {
        if (log.isDebugEnabled()) {
            log.debug("Policy Store search params - searchReq:{} ", searchReq);
        }

        PagedResult<AdminUIPolicyStore> pagedResult = adminUISecurityService.searchPolicyStores(searchReq);
        if (log.isTraceEnabled()) {
            log.trace("PagedResult  - pagedResult:{}", pagedResult);
        }

        if (pagedResult != null) {
            log.debug(
                    "Policy Store fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }
        log.debug("Policy Store  - pagedResult:{}", pagedResult);
        return pagedResult;
    }
}
