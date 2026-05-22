package io.jans.configapi.plugin.shibboleth.rest;

import java.io.InputStream;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.shibboleth.form.TrustRelationshipForm;

import io.jans.configapi.plugin.shibboleth.model.EntityType;
import io.jans.configapi.plugin.shibboleth.model.MetadataSourceType;
import io.jans.configapi.plugin.shibboleth.model.profile.*;
import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;

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

@Path(Constants.TRUST_RELATIONSHIP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethResource extends BaseResource {

    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_FORM = "Trust Relationship From";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP = "Trust Relationship";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_CHECK_STR = "Trust Relationship identified by '";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_INUM = "Trust Relationship inum";
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

    private class TrustRelationshipPagedResult extends PagedResult<TrustRelationship> {
    };

    private class StringPagedResult extends PagedResult<String> {
    };

    @Inject
    private Logger logger;

    @Inject
    private ShibbolethService shibbolethService;

    @Operation(summary = "Gets trusted service providers", description = "Gets list of trusted service providers", operationId = "get-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationshipPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProviders(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "Index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Page number to be retrieved, the number of pages is the total number of records divided by the page size (rounded up)") @DefaultValue(ApiConstants.PAGE_INDEX) @QueryParam(value = "PAGE") int page,
            @Parameter(description = "Field and value pair for searching", examples = @ExampleObject(name = "Field value example", value = "applicationType=web,persistClientAuthorizations=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Shibboleth trust search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, page:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(page), escapeLog(fieldValuePair));
        }
        SearchRequest searchReq = createSearchRequest(shibbolethService.getDnForTrustRelationship(null), pattern,
                sortBy, sortOrder, startIndex, limit, null, null, shibbolethService.getRecordMaxCount(), fieldValuePair,
                TrustRelationship.class);
        searchReq.setPage(page);

        return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Gets trusted service provider by unique identifier", description = "shibbolethService", operationId = "get-shibboleth-trust-by-inum", tags = {
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
    @Path(Constants.INUM_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProviderByInum(
            @Parameter(description = "Trust Relationship identifier") @PathParam(Constants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("Shibboleth trust search by - inum:{}", escapeLog(inum));
        }

        TrustRelationship trustRelationship = this.getTrustRelationshipByInum(inum);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Gets trusted service providers by name.", description = "Gets list of trusted service providers by name", operationId = "get-shibboleth-trust-by-name", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust-by-name.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path(Constants.NAME_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProvidersByName(
            @Parameter(description = "Trust Relationship Name") @PathParam(Constants.NAME) @NotNull String name) {
        if (logger.isDebugEnabled()) {
            logger.debug("Shibboleth trust by name:{}", escapeLog(name));
        }
        List<TrustRelationship> trustRelationshipList = shibbolethService.getAllTrustRelationshipByDisplayName(name);
        return Response.ok(trustRelationshipList).build();
    }

    @Operation(summary = "Get attribute list Trust Relationship", description = "Get attribute list Trust Relationship", operationId = "get-shibboleth-trust-attribute-list", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class)), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust-attribute-list.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path(Constants.INUM_PATH_PARAM + Constants.RELEASE_POLICY + Constants.EFFECTIVE)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustRelationshipAttribute(
            @Parameter(description = "Trust Relationship identifier") @PathParam(Constants.INUM) @NotNull String inum) {

        if (logger.isDebugEnabled()) {
            logger.debug("Verify TrustRelationship for attribute by - inum:{}", escapeLog(inum));
        }

        TrustRelationship trustRelationship = this.getTrustRelationshipByInum(inum);
        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);

     
        return Response.ok(trustRelationship.getReleasedAttributes()).build();
    }

    @Operation(summary = "Gets list of federation entity ID based on federation ID", description = "Gets list of federation entity ID based on federation ID", operationId = "get-entities-by-fed-id", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StringPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-federation-entities-by-fed-id.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path(Constants.FED_ID_PATH_PARAM + Constants.DISCOVERY + Constants.ENTITIES)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getFederationEntityId(
            @Parameter(description = "Federation ID") @PathParam(Constants.FED_ID) @NotNull String fedId,
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Get feration entity ID by fedId:{}", escapeLog(fedId));
        }
        Set<String> entityIds = shibbolethService.getFederationEntityId(fedId);
        if (logger.isDebugEnabled()) {
            logger.debug("EntityIds under federation:{} are:{}", escapeLog(fedId), entityIds);
        }

        StringPagedResult pagedResult = this.getEntityIdsPagedResult(entityIds, startIndex, limit);

        return Response.ok(pagedResult).build();
    }

    @Operation(summary = "Adds trusted service provider", description = "Adds a new trusted service provider", operationId = "post-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response addTrustRelationship(TrustRelationship trustRelationship) {
        logger.info("POST TrustRelationship");
        if (logger.isInfoEnabled()) {
            logger.info("Add TrustRelationship  trustRelationship:{}", escapeLog(trustRelationship));
        }
        // validation
        validateTrustRelationship(trustRelationship, null, false);
        shibbolethService.addTrustRelationship(trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Update Trust Relationship details", description = "Update Trust Relationship details", operationId = "put-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    @PUT
    @Path(Constants.INUM_PATH_PARAM)
    public Response updateTrustRelationship(
            @Parameter(description = "TrustRelationship inum") @PathParam(Constants.INUM) @NotNull String inum,
            @MultipartForm TrustRelationshipForm trustRelationshipForm, InputStream metadatafile) throws IOException {
        logger.info("Update TrustRelationship");
        if (logger.isInfoEnabled()) {
            logger.info("Update TrustRelationship identified by inum:{}, trustRelationshipForm:{}", escapeLog(inum),
                    escapeLog(trustRelationshipForm));
        }

        validateTrustRelationshipForm(trustRelationshipForm, metadatafile, true);
        TrustRelationship trustRelationship = shibbolethService
                .updateTrustRelationship(trustRelationshipForm.getTrustRelationship(), metadatafile);
        return Response.status(Response.Status.OK).entity(trustRelationship).build();
    }

    /**
     * Delete the TrustRelationship identified by the given inum.
     *
     * Validates that the TrustRelationship exists and removes it from storage.
     *
     * @param inum identifier (inum) of the TrustRelationship to delete
     * @return a 204 No Content response on successful deletion
     */
    @Operation(summary = "Marks Trust Relationship for deletion", description = "Marks Trust Relationship for deletion", operationId = "delete-shibboleth-trust-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_DELETE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_DELETE_ACCESS, ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteTrustRelationship(
            @Parameter(description = "TrustRelationship inum") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("TrustRelationship to be deleted - inum:{} ", escapeLog(inum));
        }

        this.shibbolethService.deleteTrustRelationship(inum);

        return Response.noContent().build();
    }

    @Operation(summary = "Gets configured SAML profiles for a trust relationship", description = "Gets configured SAML profiles for a trust relationship", operationId = "get-shibboleth-trust-saml-profiles", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLProfile.class)), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust-saml-profiles.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path(Constants.INUM_PATH_PARAM + Constants.SAML_PROFILES)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustRelationshipSamlProfiles(
            @Parameter(description = "Trust Relationship identifier") @PathParam(Constants.INUM) @NotNull String inum) {

        if (logger.isDebugEnabled()) {
            logger.debug("Shibboleth TrustRelationship SamlProfiles search by - inum:{}", escapeLog(inum));
        }

        TrustRelationship trustRelationship = this.getTrustRelationshipByInum(inum);
        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);

        return Response.ok(trustRelationship.getJansProfileConfiguration()).build();
    }

    @Operation(summary = "Updates configured SAML profiles for a trust relationship", description = "Updates configured SAML profiles for a trust relationship", operationId = "post-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, array = @ArraySchema(schema = @Schema(implementation = SAMLProfile.class)), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/put-shibboleth-trust-saml-profiles.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLProfile.class)), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/put-shibboleth-trust-saml-profiles.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @PUT
    @Path(Constants.INUM_PATH_PARAM + Constants.SAML_PROFILES)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response updateTrustRelationshipSamlProfiles(
            @Parameter(description = "Trust Relationship identifier") @PathParam(Constants.INUM) @NotNull String inum,
            List<SAMLProfile> samlProfileList) throws IOException {
        logger.info("POST TrustRelationship");
        if (logger.isInfoEnabled()) {
            logger.info("Update TrustRelationship inum:{}, SAMLProfiles:{}", escapeLog(inum),
                    escapeLog(samlProfileList));
        }

        TrustRelationship trustRelationship = this.getTrustRelationshipByInum(inum);
        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);

        // validation
        if (samlProfileList == null || samlProfileList.isEmpty()) {
            throwBadRequestException(DATA_NULL_CHK, "SAML Profile details should not be null");
        }

        trustRelationship.setJansProfileConfiguration(samlProfileList);

        // update
        trustRelationship = shibbolethService.updateTrustRelationship(trustRelationship);

        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    /* Helper methods */

    private TrustRelationship getTrustRelationshipByInum(String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("Get TrustRelationship by - inum:{}", escapeLog(inum));
        }
        TrustRelationship trustRelationship = shibbolethService.getTrustRelationshipByInum(inum);

        if (trustRelationship == null) {
            throwNotFoundException(NOT_FOUND_ERROR, String.format(NOT_FOUND_MSG, inum));
        }

        return trustRelationship;
    }

    private TrustRelationshipPagedResult doSearch(SearchRequest searchReq) {

        if (logger.isInfoEnabled()) {
            logger.info("TrustRelationship search params - searchReq:{}", escapeLog(searchReq));
        }

        PagedResult<TrustRelationship> pagedResult = shibbolethService.getTrustRelationship(searchReq);
        if (logger.isTraceEnabled()) {
            logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        }

        TrustRelationshipPagedResult pagedTrustRelationship = new TrustRelationshipPagedResult();
        if (pagedResult != null) {
            logger.debug("TrustRelationship fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            List<TrustRelationship> trustRelationships = pagedResult.getEntries();

            pagedTrustRelationship.setStart(pagedResult.getStart());
            pagedTrustRelationship.setEntriesCount(pagedResult.getEntriesCount());
            pagedTrustRelationship.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedTrustRelationship.setEntries(
                    this.getPageData(trustRelationships, searchReq.getCount().intValue(), searchReq.getPage()));
        }

        logger.info("TrustRelationship pagedTrustRelationship:{}", pagedTrustRelationship);
        return pagedTrustRelationship;
    }

    private StringPagedResult getEntityIdsPagedResult(Set<String> entityIdSet, int startIndex, int limit) {
        if (logger.isDebugEnabled()) {
            logger.debug("Federation entityIdSet:{}, startIndex:{}, limit:{}", entityIdSet, escapeLog(startIndex),
                    escapeLog(limit));
        }

        StringPagedResult stringPagedResult = new StringPagedResult();
        if (entityIdSet == null || entityIdSet.isEmpty()) {
            return stringPagedResult;
        }

        try {
            List<String> entityIdList = new ArrayList<>(entityIdSet);

            // verify start and limit index
            getStartIndex(entityIdList, startIndex);
            int toIndex = (startIndex + limit <= entityIdSet.size()) ? startIndex + limit : entityIdSet.size();
            logger.info("Final startIndex:{}, limit:{}, toIndex:{}", startIndex, limit, toIndex);

            // Extract paginated data
            List<String> sublist = entityIdList.subList(startIndex, toIndex);

            stringPagedResult.setStart(startIndex);
            stringPagedResult.setEntriesCount(limit);
            stringPagedResult.setTotalEntriesCount(entityIdList.size());
            stringPagedResult.setEntries(sublist);

        } catch (IndexOutOfBoundsException ioe) {
            logger.error("Error while getting log data is - ", ioe);
            throwBadRequestException("Index may be incorrect, total entries:{" + entityIdSet.size()
                    + "}, startIndex provided:{" + startIndex + "} , endtIndex provided:{" + limit + "} ");

        }

        logger.info("Federation Entities stringPagedResult:{}", stringPagedResult);

        return stringPagedResult;
    }

    private int getStartIndex(List<String> dataList, int startIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Get startIndex dataList:{}, startIndex:{}", dataList, escapeLog(startIndex));
        }

        if (dataList != null && !dataList.isEmpty()) {
            try {
                dataList.get(startIndex);
            } catch (IndexOutOfBoundsException ioe) {
                logger.error("Error while getting data at startIndex:{}", startIndex, ioe);
                throwBadRequestException("Page startIndex is incorrect, total entries are:{" + dataList.size()
                        + "}, but startIndex provided is:{" + startIndex + "} ");
            }
        }
        return startIndex;
    }

    private List<TrustRelationship> getPageData(List<TrustRelationship> dataList, int limit, int pageNo) {
        if (logger.isDebugEnabled()) {
            logger.debug("Get Page data dataList:{}, limit:{}, pageNo:{}", dataList, escapeLog(limit),
                    escapeLog(pageNo));
        }

        List<TrustRelationship> pageDataList = null;
        if (dataList == null || dataList.isEmpty()) {
            return pageDataList;
        }
        int dataSize = dataList.size();
        int totalPages =  (int)Math.ceil((double)dataSize / limit);
        logger.info("dataSize:{}, limit:{}, totalPages:{}", dataSize, limit, totalPages);
        if (totalPages < pageNo) {
            throwBadRequestException("Total pages in paginated result are:{" + totalPages
                    + "}, but page provided in request is:{" + pageNo + "} ");
        }
        int startIndex = pageNo==1? 0: (pageNo * limit +1);
        logger.error("startIndex:{}", startIndex);
        try {
            dataList.get(startIndex);
        } catch (IndexOutOfBoundsException ioe) {
            logger.error("Error while getting data as startIndex:{}", startIndex, ioe);
            throwBadRequestException(
                    "Page startIndex:{ " + startIndex + " } calulated as per pageNo:{ " + pageNo + " } is incorrect");
        }

        int toIndex = (startIndex + limit <= dataList.size()) ? startIndex + limit : dataList.size();

        logger.error("toIndex:{}", toIndex);
       /* try {
            dataList.get(toIndex);
        } catch (IndexOutOfBoundsException ioe) {
            logger.error("Error while getting data as toIndex:{}", toIndex, ioe);
            throwBadRequestException(
                    "Page toIndex:{ " + toIndex + " } calulated as per pageNo:{ " + pageNo + " } is incorrect");
        }*/
        pageDataList = dataList.subList(startIndex, toIndex);
        logger.debug("toIndex:{}, pageDataList:{}", toIndex, pageDataList);
        return pageDataList;
    }

    private void validateTrustRelationshipForm(TrustRelationshipForm trustRelationshipForm, InputStream metaDataFile,
            boolean isUpdate) {
        checkResourceNotNull(trustRelationshipForm, SHIBBOLETH_TRUST_RELATIONSHIP_FORM);
        validateTrustRelationship(trustRelationshipForm.getTrustRelationship(), metaDataFile, isUpdate);
    }

    private void validateTrustRelationship(TrustRelationship trustRelationship, InputStream metaDataFile,
            boolean isUpdate) {
        if (logger.isInfoEnabled()) {
            logger.info("trustRelationship:{}, metaDataFile:{}, isUpdate:{}", escapeLog(trustRelationship), escapeLog(metaDataFile), isUpdate);
        }
        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);

        // mandatory attributes
        List<String> missingAttributesList = new ArrayList<>();
        if (StringUtils.isBlank(trustRelationship.getDisplayName())) {
            missingAttributesList.add(DISPLAY_NAME);
        }
        if (trustRelationship.getEntityType() == null) {
            missingAttributesList.add(TRUST_NATURE);
        }

        if (!missingAttributesList.isEmpty()) {
            checkNotNull(missingAttributesList);
        }

        // check if Enity type is valid
        if ((EntityType.getByValue(trustRelationship.getEntityType().getValue()) == null)
                || (StringUtils.isBlank(trustRelationship.getEntityType().getValue()))) {
            throwBadRequestException(INVALID_TRUST_NATURE,
                    String.format(INVALID_TRUST_NATURE_MSG, trustRelationship.getEntityType()));
        }

        // inum - if update
        final String inum = trustRelationship.getInum();
        if (isUpdate) {
            checkNotNull(inum, SHIBBOLETH_TRUST_RELATIONSHIP_INUM);
        }

        // check if TrustRelationship with same name already exists
        List<TrustRelationship> existingTrustRelationshipList = shibbolethService
                .getAllTrustRelationshipByDisplayName(trustRelationship.getDisplayName());
        logger.debug(" existingTrustRelationship:{} ", existingTrustRelationshipList);
        if (existingTrustRelationshipList != null && !existingTrustRelationshipList.isEmpty()) {
            List<TrustRelationship> list = null;
            if (isUpdate) {
                List<String> inumList = existingTrustRelationshipList.stream().map(TrustRelationship::getInum)
                        .collect(Collectors.toList());
                logger.info("TrustRelationship's with name:{}, inumList:{}", trustRelationship.getDisplayName(),
                        inumList);
                list = existingTrustRelationshipList.stream().filter(e -> !e.getInum().equalsIgnoreCase(inum))
                        .collect(Collectors.toList());
                logger.info("Other TrustRelationship's with same name:{} list:{}", trustRelationship.getDisplayName(),
                        list);

            }
            if ((!isUpdate) || (list != null && !list.isEmpty())) {
                throwBadRequestException(NAME_CONFLICT,
                        String.format(NAME_CONFLICT_MSG, trustRelationship.getDisplayName()));
            }
        }

        if (isUpdate) {
            validateMetaData(trustRelationship, metaDataFile);
        }
    }

    private void validateMetaData(TrustRelationship trustRelationship, InputStream metaDataFile) {
        logger.info("validateSpMetaDataSourceType trustRelationship:{}", trustRelationship);

        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);
        checkResourceNotNull(trustRelationship.getMetadataSource(), "MetadataSource");
        checkResourceNotNull(trustRelationship.getMetadataSource().getMetadataSourceType(), "MetadataSourceType");
        logger.info("Validate trustRelationship.getMetadataSource():{}", trustRelationship.getMetadataSource());
        MetadataSourceType metadataSourceType = trustRelationship.getMetadataSource().getMetadataSourceType();

        switch (metadataSourceType) {
        case FILE:
            validateFileMetaDataSourceType(metaDataFile);
            break;
        case URI:
            validateURIMetaDataSourceType(trustRelationship);
            break;
        case UPSTREAM:
            validateUpstreamMetaDataSourceType(trustRelationship);
            break;
        case MANUAL:
            validateManualMetaDataSourceType(trustRelationship);
            break;
        case MDQ:
            validateMDQMetaDataSourceType(trustRelationship);
            break;
        default:
            return;
        }

    }

    private void validateFileMetaDataSourceType(InputStream metaDataFile) {

        // If MetaDataSourceType==FILE and it is not Update flow
        try {
            if ((metaDataFile == null || metaDataFile.available() <= 0)) {
                throwBadRequestException(DATA_NULL_CHK, String.format(DATA_NULL_MSG, "Metadata File"));
            }
        } catch (IOException ioex) {
            throwBadRequestException(METADATA_FILE_ERR, "Error while processing Metadata File");
        }
    }

    private void validateURIMetaDataSourceType(TrustRelationship trustRelationship) {

        checkNotNull(trustRelationship.getMetadataSource().getMetadataStr(), "Metadata URI");

        // check if URI is valid
        this.shibbolethService.urlExists(trustRelationship.getMetadataSource().getMetadataStr());
    }

    private void validateUpstreamMetaDataSourceType(TrustRelationship trustRelationship) {

        checkNotNull(trustRelationship.getMetadataSource().getMetadataStr(), "Metadata");

        // check if UPSTREAM data is valid
        // TO-DO ??

    }

    private void validateManualMetaDataSourceType(TrustRelationship trustRelationship) {
        logger.info("validateManualMetaDataSourceType trustRelationship:{}", trustRelationship);

        checkNotNull(trustRelationship.getMetadataSource().getMetadataStr(), "Metadata string");

        InputStream metaDataInputStream = new ByteArrayInputStream(
                trustRelationship.getMetadataSource().getMetadataStr().getBytes());
        try {
            if ((metaDataInputStream.available() <= 0)) {
                throwBadRequestException(DATA_NULL_CHK,
                        String.format(DATA_NULL_MSG, "SP MetaData String should be provided"));
            }
        } catch (IOException ioex) {
            throwBadRequestException("METADATA_ERR", "Error while processing Manual Metadata string");
        }

    }

    private void validateMDQMetaDataSourceType(TrustRelationship trustRelationship) {

        checkNotNull(trustRelationship.getMetadataSource().getMetadataStr(), "Metadata MDQ detail");

        // check if MDQ data is valid
        // TO-DO ??

    }

}
