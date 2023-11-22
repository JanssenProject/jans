/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

@Path(Constants.KEYCLOAK + Constants.SAML_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdpResource extends BaseResource {

    private static final String SAML_IDP_DATA = "SAML IDP Data";
    private static final String SAML_IDP_DATA_FORM = "SAML IDP Data From";

   @Inject
    Logger log;

    @Inject
    IdpService idpService;

    @Operation(summary = "Retrieves SAML Identity Provider", description = "Retrieves SAML Identity Provider", operationId = "get-saml-identity-provider", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = {Constants.JANS_IDP_SAML_READ_ACCESS})
    public Response getAllSamlIdentityProvider(@Parameter(description = "Search size - max size of the results to return") @DefaultValue(Constants.REALM_MASTER) @QueryParam(value = Constants.REALM) String realm) {
        log.error("Fetch SAML IDP from realm:{}", realm);
        List<IdentityProvider> idpList = idpService.getAllIdentityProviders(realm);
        log.error("SAML IDP fetched idpList:{}", idpList);
        return Response.ok(idpList).build();
    }

    @Operation(summary = "Create SAML Identity Provider", description = "Create SAML Identity Provider", operationId = "post-saml-identity-provider", tags = {
            "Jans - SAML Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_SAML_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = BrokerIdentityProviderForm.class), examples = {
            @ExampleObject(value = "") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = IdentityProviderRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path(Constants.UPLOAD_PATH)
    @ProtectedApi(scopes = { Constants.JANS_IDP_SAML_WRITE_ACCESS })
    public Response createSamlIdentityProvider(@MultipartForm BrokerIdentityProviderForm brokerIdentityProviderForm)
            throws IOException {
        log.error("Create brokerIdentityProviderForm:{}", brokerIdentityProviderForm);

        checkResourceNotNull(brokerIdentityProviderForm, SAML_IDP_DATA_FORM);

        IdentityProvider idp = brokerIdentityProviderForm.getIdentityProvider();
        log.error(" Create idp:{} ", idp);
        
        //validation
        checkResourceNotNull(idp, SAML_IDP_DATA);
        checkNotNull(idp.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkNotNull(idp.getRealm(), Constants.REALM);
        InputStream metaDataFile = brokerIdentityProviderForm.getMetaDataFile();
        log.error(" Create metaDataFile:{} ", metaDataFile);
        if (metaDataFile != null) {
            log.error(" IDP metaDataFile.available():{}", metaDataFile.available());
        }

        //create SAML IDP
        idp = idpService.createSamlIdentityProvider(idp, metaDataFile);
        
        log.error("Create created by idp:{}", idp);
        return Response.status(Response.Status.CREATED).entity(idp).build();
    }
}