/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpService;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.plugin.keycloak.idp.broker.form.BrokerIdentityProviderForm;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.util.security.StringEncrypter.EncryptionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path(Constants.KEYCLOAK + Constants.SAML_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdpResource extends BaseResource {

    private static final String SAML_IDP_DATA = "SAML IDP Data";
    private static final String SAML_IDP_DATA_FORM = "SAML IDP Data From";

    private class IdentityProviderPagedResult extends PagedResult<IdentityProvider> {
    };

    @Inject
    Logger log;

    @Inject
    IdpService idpService;

    @Operation(summary = "Retrieves SAML Identity Provider", description = "Retrieves SAML Identity Provider", operationId = "get-saml-identity-provider", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/idp/get-all-saml-identity-provider.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_READ_ACCESS })
    public Response getAllSamlIdentityProvider(
        @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
        @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
        @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
        @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
        @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder)
        {
    if (log.isDebugEnabled()) {
        log.debug("Client serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                escapeLog(sortOrder));
    }

    SearchRequest searchReq = createSearchRequest(idpService.getIdentityProviderDn(), pattern, sortBy, sortOrder,
            startIndex, limit, null, null, ApiConstants.DEFAULT_MAX_COUNT, IdentityProvider.class);

    return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Get SAML Identity Provider by Inum", description = "Get SAML Identity Provider by Inum", operationId = "get-saml-identity-provider-by-inum", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_READ_ACCESS })
    public Response getSamlIdentityProviderByInum(
            @Parameter(description = "Unique identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.error("Fetch SAML IDP by inum:{}", inum);
        IdentityProvider idp = idpService.getIdentityProviderByInum(inum);
        log.error("SAML IDP fetched  idp:{}", idp);
        return Response.ok(idp).build();
    }

    @Operation(summary = "Create SAML Identity Provider", description = "Create SAML Identity Provider", operationId = "post-saml-identity-provider", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = BrokerIdentityProviderForm.class), examples = {
            @ExampleObject(value = "") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = IdentityProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path(Constants.UPLOAD_PATH)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_WRITE_ACCESS })
    public Response createSamlIdentityProvider(@MultipartForm BrokerIdentityProviderForm brokerIdentityProviderForm)
            throws IOException {
        log.error("Create brokerIdentityProviderForm:{}", brokerIdentityProviderForm);

        // validation
        checkResourceNotNull(brokerIdentityProviderForm, SAML_IDP_DATA_FORM);
        IdentityProvider idp = brokerIdentityProviderForm.getIdentityProvider();
        log.error(" Create idp:{} ", idp);
        checkResourceNotNull(idp, SAML_IDP_DATA);
        checkNotNull(idp.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkNotNull(idp.getRealm(), Constants.REALM);
        InputStream metaDataFile = brokerIdentityProviderForm.getMetaDataFile();
        log.error(" Create metaDataFile:{} ", metaDataFile);
        if (metaDataFile != null) {
            log.error(" IDP metaDataFile.available():{}", metaDataFile.available());
        }

        // create SAML IDP
        idp = idpService.createSamlIdentityProvider(idp, metaDataFile);

        log.error("Create IdentityProvider - idp:{}", idp);
        return Response.status(Response.Status.CREATED).entity(idp).build();
    }

    @Operation(summary = "Update SAML Identity Provider", description = "Update SAML Identity Provider", operationId = "put-saml-identity-provider", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = BrokerIdentityProviderForm.class), examples = {
            @ExampleObject(value = "") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = IdentityProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @PUT
    @Path(Constants.UPLOAD_PATH)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_WRITE_ACCESS })
    public Response updateSamlIdentityProvider(@MultipartForm BrokerIdentityProviderForm brokerIdentityProviderForm)
            throws IOException {
        log.error("Update brokerIdentityProviderForm:{}", brokerIdentityProviderForm);

        // validation
        checkResourceNotNull(brokerIdentityProviderForm, SAML_IDP_DATA_FORM);
        IdentityProvider idp = brokerIdentityProviderForm.getIdentityProvider();
        log.error(" Update idp:{} ", idp);
        checkResourceNotNull(idp, SAML_IDP_DATA);
        checkNotNull(idp.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkNotNull(idp.getInum(), AttributeNames.INUM);
        checkNotNull(idp.getRealm(), Constants.REALM);
        IdentityProvider existingIdentityProvider = idpService.getIdentityProviderByInum(idp.getInum());
        log.error(" existingIdentityProvider:{} ", existingIdentityProvider);
        checkResourceNotNull(existingIdentityProvider, "IdentityProvider identified by '" + idp.getInum() + "'");
        InputStream metaDataFile = brokerIdentityProviderForm.getMetaDataFile();
        log.error(" Update metaDataFile:{} ", metaDataFile);
        if (metaDataFile != null) {
            log.error(" IDP metaDataFile.available():{}", metaDataFile.available());
        }

        // create SAML IDP
        idp = idpService.updateSamlIdentityProvider(idp, metaDataFile);

        log.error("Updated IdentityProvider idp:{}", idp);
        return Response.ok(idp).build();
    }

    @Operation(summary = "Delete SAML Identity Provider", description = "Delete SAML Identity Provider", operationId = "delete-saml-identity-provider", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
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
            @Parameter(description = "Unique identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (log.isDebugEnabled()) {
            log.debug("IdentityProvider to be deleted - inum:{} ", escapeLog(inum));
        }
        IdentityProvider existingIdentityProvider = idpService.getIdentityProviderByInum(inum);
        log.error(" existingIdentityProvider:{} ", existingIdentityProvider);
        checkResourceNotNull(existingIdentityProvider, "IdentityProvider identified by '" + inum + "'");

        idpService.deleteIdentityProvider(existingIdentityProvider);
        return Response.noContent().build();
    }

    private IdentityProviderPagedResult doSearch(SearchRequest searchReq)
            throws IllegalAccessException, InvocationTargetException {
        if (log.isInfoEnabled()) {
            log.info("IdentityProvider search params - searchReq:{}, realm:{} ", escapeLog(searchReq));
        }

        PagedResult<IdentityProvider> pagedResult = idpService.getIdentityProviders(searchReq);
        if (log.isTraceEnabled()) {
            log.debug("IdentityProvider PagedResult - pagedResult:{}", pagedResult);
        }

        IdentityProviderPagedResult pagedIdentityProvider = new IdentityProviderPagedResult();
        if (pagedResult != null) {
            log.debug("IdentityProviders fetched - pagedResult.getEntries():{}", pagedResult.getEntries());
            List<IdentityProvider> identityProviderList = pagedResult.getEntries();
            pagedIdentityProvider.setStart(pagedResult.getStart());
            pagedIdentityProvider.setEntriesCount(pagedResult.getEntriesCount());
            pagedIdentityProvider.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedIdentityProvider.setEntries(identityProviderList);
        }

        log.info("pagedIdentityProvider:{}", pagedIdentityProvider);
        return pagedIdentityProvider;

    }

}