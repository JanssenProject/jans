/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.rest;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.jans.configapi.plugin.saml.service.IdpService;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.plugin.saml.form.BrokerIdentityProviderForm;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
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
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path(Constants.SAML_PATH + Constants.IDENTITY_PROVIDER)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdpResource extends BaseResource {

    private static final String SAML_IDP_DATA = "SAML IDP Data";
    private static final String SAML_IDP_DATA_FORM = "SAML IDP Data From";
    private static final String SAML_IDP_CHECK_STR = "IdentityProvider identified by '";
    private static final String NAME_CONFLICT = "NAME_CONFLICT";
    private static final String NAME_CONFLICT_MSG = "SAML IDP with same name %s already exists!";
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String UNAUTHORIZED_MSG = "Realm client authorization failed while creating IDP.";
    private static final String APPLICATION_ERROR = "Application Error";

    private class IdentityProviderPagedResult extends PagedResult<IdentityProvider> {
    };

    @Inject
    Logger log;

    @Inject
    IdpService idpService;

    @Operation(summary = "Retrieves SAML Identity Provider", description = "Retrieves SAML Identity Provider", operationId = "get-saml-identity-provider", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/trust-idp/get-all-saml-identity-provider.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_READ_ACCESS })
    public Response getAllSamlIdentityProvider(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "displayName=saml-idp,realm=jans")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair)
             {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Client serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }

        SearchRequest searchReq = createSearchRequest(idpService.getIdentityProviderDn(), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, ApiConstants.DEFAULT_MAX_COUNT, fieldValuePair, IdentityProvider.class);

        return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Get SAML Identity Provider by Inum", description = "Get SAML Identity Provider by Inum", operationId = "get-saml-identity-provider-by-inum", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProvider.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/trust-idp/get-saml-identity-provider-by-inum.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.INUM_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_READ_ACCESS })
    public Response getSamlIdentityProviderByInum(
            @Parameter(description = "Unique identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (log.isInfoEnabled()) {
            log.info("Fetch SAML IDP by inum:{}", escapeLog(inum));
        }
        IdentityProvider idp = idpService.getIdentityProviderByInum(inum);
        log.debug("SAML IDP fetched  idp:{}", idp);
        return Response.ok(idp).build();
    }

    @Operation(summary = "Get SAML SP Metadata as Json", description = "Get SAML SP Metadata as Json", operationId = "get-saml-sp-metadata-json", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.SP_METADATA_PATH + Constants.INUM_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_READ_ACCESS })
    public Response getSamlSPMetadataJson(
            @Parameter(description = "Unique identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (log.isInfoEnabled()) {
            log.info("Fetch SAML SP Metadata for IDP by inum:{}", escapeLog(inum));
        }
        String json = null;
        try {
            IdentityProvider identityProvider = idpService.getIdentityProviderByInum(inum);
            log.debug(" identityProvider:{} ", identityProvider);
            checkResourceNotNull(identityProvider, SAML_IDP_CHECK_STR + inum + "'");
            json = idpService.getSpMetadata(identityProvider);
            log.info(" json:{} ", json);
        } catch (Exception ex) {
            throwInternalServerException("SAML_SP_METADATA", ex.getMessage());
        }
        return Response.ok(json).build();
    }

    @Operation(summary = "Get SAML SP Metadata Endpoint URL", description = "Get SAML SP Metadata Endpoint URL", operationId = "get-saml-sp-metadata-url", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.SP_METADATA_FILE_PATH + Constants.INUM_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_READ_ACCESS })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getSamlSPMetadataURL(
            @Parameter(description = "Unique identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (log.isInfoEnabled()) {
            log.info("Fetch SAML SP Metadata URL IDP by inum:{}", escapeLog(inum));
        }
        IdentityProvider identityProvider = idpService.getIdentityProviderByInum(inum);
        log.debug(" identityProvider:{} ", identityProvider);
        checkResourceNotNull(identityProvider, SAML_IDP_CHECK_STR + inum + "'");
        String spMetadataUrl = idpService.getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
        log.info(" spMetadataUrl:{} ", spMetadataUrl);
        return Response.ok(spMetadataUrl).build();
    }

    @Operation(summary = "Create SAML Identity Provider", description = "Create SAML Identity Provider", operationId = "post-saml-identity-provider", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = BrokerIdentityProviderForm.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/trust-idp/post-saml-identity-provider.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = IdentityProvider.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/trust-idp/get-saml-identity-provider-by-inum.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path(Constants.UPLOAD_PATH)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_WRITE_ACCESS })
    public Response createSamlIdentityProvider(@MultipartForm BrokerIdentityProviderForm brokerIdentityProviderForm)
            throws IOException {
        if (log.isInfoEnabled()) {
            log.info("Create brokerIdentityProviderForm:{}", brokerIdentityProviderForm);
        }

        // validation
        checkResourceNotNull(brokerIdentityProviderForm, SAML_IDP_DATA_FORM);
        IdentityProvider idp = brokerIdentityProviderForm.getIdentityProvider();
        log.debug(" Create idp:{} ", idp);
        checkResourceNotNull(idp, SAML_IDP_DATA);
        checkNotNull(idp.getName(), "NAME");
        checkNotNull(idp.getDisplayName(), AttributeNames.DISPLAY_NAME);

        // check if IDP with same name already exists
        List<IdentityProvider> existingIdentityProviders = idpService.getIdentityProviderByName(idp.getName());
        log.debug(" existingIdentityProviders:{} ", existingIdentityProviders);
        if (existingIdentityProviders != null && !existingIdentityProviders.isEmpty()) {
            throwBadRequestException(NAME_CONFLICT,String.format(NAME_CONFLICT_MSG, idp.getName()));
        }

        InputStream metaDataFile = brokerIdentityProviderForm.getMetaDataFile();
        log.debug(" Create metaDataFile:{} ", metaDataFile);

        // create SAML IDP
        try {
            idp = idpService.createSamlIdentityProvider(idp, metaDataFile);
        } catch (WebApplicationException wex) {
            log.error("Application Error while creating IDP is - status:{}, message:{}",wex.getResponse().getStatus(), wex.getMessage());
            if (wex.getResponse() != null && wex.getResponse().getStatusInfo() != null
                    && wex.getResponse().getStatusInfo().equals(Status.CONFLICT)) {
                throwBadRequestException(NAME_CONFLICT,String.format(NAME_CONFLICT_MSG, idp.getName()));
            }else if (wex.getResponse() != null && wex.getResponse().getStatusInfo() != null
                    && wex.getResponse().getStatusInfo().equals(Status.UNAUTHORIZED)) {
                throwBadRequestException(UNAUTHORIZED,UNAUTHORIZED_MSG);
            }
            throwInternalServerException(APPLICATION_ERROR, wex.getMessage());
        }

        log.info("Create IdentityProvider - idp:{}", idp);
        return Response.status(Response.Status.CREATED).entity(idp).build();
    }

    @Operation(summary = "Update SAML Identity Provider", description = "Update SAML Identity Provider", operationId = "put-saml-identity-provider", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = BrokerIdentityProviderForm.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/trust-idp/put-saml-identity-provider.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = IdentityProvider.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/trust-idp/get-saml-identity-provider-by-inum.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @PUT
    @Path(Constants.UPLOAD_PATH)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_WRITE_ACCESS })
    public Response updateSamlIdentityProvider(@MultipartForm BrokerIdentityProviderForm brokerIdentityProviderForm)
            throws IOException {
        if (log.isInfoEnabled()) {
            log.info("Update brokerIdentityProviderForm:{}", brokerIdentityProviderForm);
        }

        // validation
        checkResourceNotNull(brokerIdentityProviderForm, SAML_IDP_DATA_FORM);
        IdentityProvider idp = brokerIdentityProviderForm.getIdentityProvider();
        log.debug(" Update idp:{} ", idp);

        checkResourceNotNull(idp, SAML_IDP_DATA);
        checkNotNull(idp.getName(), AttributeNames.NAME);
        checkNotNull(idp.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkNotNull(idp.getInum(), AttributeNames.INUM);
        IdentityProvider existingIdentityProvider = idpService.getIdentityProviderByInum(idp.getInum());

        log.debug(" existingIdentityProvider:{} ", existingIdentityProvider);
        checkResourceNotNull(existingIdentityProvider, SAML_IDP_CHECK_STR + idp.getInum() + "'");
        
        //if realm not provided use existing one
        if (StringUtils.isBlank(idp.getRealm())) {
            idp.setRealm(existingIdentityProvider.getRealm());
        }
        
        InputStream metaDataFile = brokerIdentityProviderForm.getMetaDataFile();
        log.debug(" Update metaDataFile:{} ", metaDataFile);

        // update SAML IDP
        try {
        idp = idpService.updateSamlIdentityProvider(idp, metaDataFile);
        } catch (WebApplicationException wex) {
            log.error("Application Error while updating IDP is - status:{}, message:{}",wex.getResponse().getStatus(), wex.getMessage());
            if (wex.getResponse() != null && wex.getResponse().getStatusInfo() != null
                    && wex.getResponse().getStatusInfo().equals(Status.CONFLICT)) {
                throwBadRequestException(NAME_CONFLICT,
                        "SAML IDP with same name '" + idp.getName() + "' already exists!");
            }else if (wex.getResponse() != null && wex.getResponse().getStatusInfo() != null
                    && wex.getResponse().getStatusInfo().equals(Status.UNAUTHORIZED)) {
                throwBadRequestException("UNAUTHORIZED", UNAUTHORIZED_MSG);
            }
            throwInternalServerException(APPLICATION_ERROR, wex.getMessage());
        }
        
        log.info("Updated IdentityProvider idp:{}", idp);
        return Response.ok(idp).build();
    }

    @Operation(summary = "Delete SAML Identity Provider", description = "Delete SAML Identity Provider", operationId = "delete-saml-identity-provider", tags = {
            "SAML - Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_DELETE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteIdentityProvider(
            @Parameter(description = "Unique identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("IdentityProvider to be deleted - inum:{} ", escapeLog(inum));
        }
        IdentityProvider existingIdentityProvider = idpService.getIdentityProviderByInum(inum);
        log.debug(" existingIdentityProvider:{} ", existingIdentityProvider);
        checkResourceNotNull(existingIdentityProvider, SAML_IDP_CHECK_STR + inum + "'");

        idpService.deleteIdentityProvider(existingIdentityProvider, true);
        return Response.noContent().build();
    }

    
    private IdentityProviderPagedResult doSearch(SearchRequest searchReq) {

        if (log.isInfoEnabled()) {
            log.info("IdentityProvider search params - searchReq:{} ", escapeLog(searchReq));
        }
        IdentityProviderPagedResult pagedIdentityProvider = null;
        try {
            PagedResult<IdentityProvider> pagedResult = idpService.getIdentityProviders(searchReq);
            if (log.isTraceEnabled()) {
                log.debug("IdentityProvider PagedResult - pagedResult:{}", pagedResult);
            }

            pagedIdentityProvider = new IdentityProviderPagedResult();
            if (pagedResult != null) {
                log.debug("IdentityProviders fetched - pagedResult.getEntries():{}", pagedResult.getEntries());
                List<IdentityProvider> identityProviderList = pagedResult.getEntries();
                pagedIdentityProvider.setStart(pagedResult.getStart());
                pagedIdentityProvider.setEntriesCount(pagedResult.getEntriesCount());
                pagedIdentityProvider.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
                pagedIdentityProvider.setEntries(identityProviderList);
            }

            log.info("pagedIdentityProvider:{}", pagedIdentityProvider);
        } catch (Exception ex) {
            throwInternalServerException(ex.getMessage());
        }
        return pagedIdentityProvider;
    }
    
}