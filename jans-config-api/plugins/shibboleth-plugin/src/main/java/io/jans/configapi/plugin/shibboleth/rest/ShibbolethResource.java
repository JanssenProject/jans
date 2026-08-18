package io.jans.configapi.plugin.shibboleth.rest;

import java.io.InputStream;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipQuery;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipSummaryEntry;
import io.jans.configapi.plugin.shibboleth.service.ShibbolethService;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.TRUST_RELATIONSHIP_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethResource extends BaseResource {

    private static final String SHIBBOLETH_TRUST_RELATIONSHIP = "Trust Relationship";

    private static final String NULL_MSG = "NULL_PARAM";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_ID_ERROR = "Trust Relationship Id should not be null!";
    private static final String DISPLAY_NAME = "Display Name";
    private static final String TRUST_NATURE = "Trust Nature";
    private static final String INVALID_TRUST_NATURE = "INVALID_TRUST_NATURE";
    private static final String INVALID_TRUST_NATURE_MSG = "Trust Nature is invalid.";

    private static final String NOT_FOUND_ERROR = "NOT_FOUND_ERROR";
    private static final String NOT_FOUND_MSG = "Trust Relationship with identifier `%s` does not exist!";
    private static final String NAME_CONFLICT = "NAME_CONFLICT";
    private static final String NAME_CONFLICT_MSG = "Trust Relationship with same name `%s` already exists!";
    private static final String DATA_NULL_CHK = "RESOURCE_IS_NULL";
    private static final String DATA_NULL_MSG = "`%s` should not be null!";
    private static final String METADATA_FILE = "METADATA_FILE";
    private static final String METADATA_FILE_ERR = "METADATA_FILE_ERR";

    private class TrustRelationshipSummaryEntryPagedResult extends PagedResult<TrustRelationshipSummaryEntry> {
    };

    @Inject
    private Logger logger;

    @Inject
    private ShibbolethService shibbolethService;

    @Operation(summary = "Gets list of TrustRelationship", description = "Gets list of TrustRelationship", operationId = "get-shibboleth-trust-relationship", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationshipSummaryEntryPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-all-shibboleth-trust.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProviders(
            @Parameter(description = "DisplayName Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.DISPLAY_NAME_FILTER) String displayNameFilter,
            @Parameter(description = "Description Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.DESCRIPTION_FILTER) String descriptionFilter,
            @Parameter(description = "Page number to be retrieved, the number of pages is the total number of records divided by the page size (rounded up)") @DefaultValue(ApiConstants.PAGE_INDEX) @QueryParam(value = "PAGE") int page,
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit) {
        if (logger.isInfoEnabled()) {
            logger.info("Shibboleth trust search param - limit:{}, displayNameFilter:{}, descriptionFilter:{}, page:{}",
                    escapeLog(limit), escapeLog(displayNameFilter), escapeLog(descriptionFilter), escapeLog(page));
        }

        TrustRelationshipQuery trustRelationshipQuery = createTrustRelationshipQuery(displayNameFilter,
                descriptionFilter, page, limit);
        PagedResult<TrustRelationshipSummaryEntry> pagedTrustRelationshipResult = shibbolethService
                .getTrustRelationship(trustRelationshipQuery);
        logger.info(" pagedTrustRelationshipResult:{} ", pagedTrustRelationshipResult);
        return Response.status(Response.Status.OK).entity(pagedTrustRelationshipResult).build();
    }

    @Operation(summary = "Fetch TrustRelationship provider by unique identifier", description = "Fetch TrustRelationship provider by unique identifier", operationId = "get-shibboleth-trust-relationship-id", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust-by-id.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path(Constants.ID_PATH + Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustRelationshipById(
            @Parameter(description = "TrustRelationship identifier") @PathParam(Constants.ID) @NotNull Id id) {
        if (logger.isDebugEnabled()) {
            logger.debug("Shibboleth TrustRelationship search by - id:{}", escapeLog(id));
        }

        if (id == null || id.getValue() == null) {
            throwBadRequestException(NULL_MSG, SHIBBOLETH_TRUST_RELATIONSHIP_ID_ERROR);
        }
        logger.error("Fetch Shibboleth TrustRelationship by - id:{}", id);
        TrustRelationship trustRelationship = shibbolethService.findById(id);
        logger.error("Shibboleth TrustRelationship by - id:{}, trustRelationship:{}", id, trustRelationship);
        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Adds trusted service provider", description = "Adds a new trusted service provider", operationId = "post-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateTrustRelationshipRequest.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response addTrustRelationship(CreateTrustRelationshipRequest createTrustRelationshipRequest) {
        logger.info("POST TrustRelationship");
        if (logger.isInfoEnabled()) {
            logger.info("Add TrustRelationship  createTrustRelationshipRequest:{}",
                    escapeLog(createTrustRelationshipRequest));
        }
        // validation
        checkResourceNotNull(createTrustRelationshipRequest, "TrustRelationship request is null");
        TrustRelationship trustRelationship = shibbolethService.addTrustRelationship(createTrustRelationshipRequest);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Delete TrustRelationship provider by unique identifier", description = "Delete TrustRelationship provider by unique identifier", operationId = "get-shibboleth-trust-relationship-id", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust-by-id.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @DELETE
    @Path(Constants.ID_PATH + Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_DELETE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response deleteTrustRelationshipById(
            @Parameter(description = "TrustRelationship identifier") @PathParam(Constants.ID) @NotNull Id id) {
        if (logger.isDebugEnabled()) {
            logger.debug("Shibboleth TrustRelationship search by - id:{}", escapeLog(id));
        }

        if (id == null || id.getValue() == null) {
            throwBadRequestException(NULL_MSG, SHIBBOLETH_TRUST_RELATIONSHIP_ID_ERROR);
        }
        logger.error("Delete Shibboleth TrustRelationship by - id:{}", id);
        shibbolethService.delete(id);
        logger.error("Successfully deleted TrustRelationship by - id:{}", id);
        return Response.noContent().build();
    }

    /* Helper Method */

    private TrustRelationshipQuery createTrustRelationshipQuery(String displayNameFilter, String descriptionFilter,
            int page, int limit) {
        return new TrustRelationshipQuery(displayNameFilter, descriptionFilter, page, limit);
    }

}
