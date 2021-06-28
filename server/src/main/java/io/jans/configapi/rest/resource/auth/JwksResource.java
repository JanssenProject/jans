/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.model.config.Conf;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@Path(ApiConstants.CONFIG + ApiConstants.JWKS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JwksResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_READ_ACCESS })
    public Response get() {
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS })
    public Response put(WebKeysConfiguration webkeys) {
        log.debug("JWKS details to be updated - webkeys = " + webkeys);
        final Conf conf = configurationService.findConf();
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS })
    public Response patch(String requestString) throws JsonPatchException, IOException {
        log.debug("JWKS details to be patched - requestString = " + requestString);
        final Conf conf = configurationService.findConf();
        WebKeysConfiguration webKeys = conf.getWebKeys();
        webKeys = Jackson.applyPatch(requestString, webKeys);
        conf.setWebKeys(webKeys);
        configurationService.merge(conf);
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS })
    @Path(ApiConstants.KEY_PATH)
    public Response getKeyById(@NotNull JSONWebKey jwk) {
        log.debug("Add a new Key to the JWKS = " + jwk);
        Conf conf = configurationService.findConf();
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        log.debug("WebKeysConfiguration before addding new key =" + webkeys);

        // Reject if key with same kid already exists
        // if(webkeys.getKeys().stream().anyMatch(x -> x.getKid()!=null &&
        // x.getKid().equals(jwk.getKid())) ){
        if (getJSONWebKey(webkeys, jwk.getKid()) != null) {
            throw new NotAcceptableException(
                    getNotAcceptableException("JWK with same kid - '" + jwk.getKid() + "' already exists!"));
        }

        // Add key
        webkeys.getKeys().add(jwk);
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        webkeys = configurationService.findConf().getWebKeys();
        return Response.status(Response.Status.CREATED).entity(jwk).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_READ_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response getKeyById(@PathParam(ApiConstants.KID) @NotNull String kid) {
        log.debug("Fetch JWK details by kid = " + kid);
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        log.debug("WebKeysConfiguration before addding new key =" + webkeys);
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        return Response.ok(jwk).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response patch(@PathParam(ApiConstants.KID) @NotNull String kid, @NotNull String requestString)
            throws JsonPatchException, IOException {
        log.debug("JWKS details to be patched for kid = " + kid + " ,requestString = " + requestString);
        Conf conf = configurationService.findConf();
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        if (jwk == null) {
            throw new NotFoundException(getNotFoundError("JWK with kid - '" + kid + "' does not exist!"));
        }

        // Patch
        jwk = Jackson.applyPatch(requestString, jwk);
        log.debug("JWKS details patched - jwk = " + jwk);

        // Remove old Jwk
        conf.getWebKeys().getKeys().removeIf(x -> x.getKid() != null && x.getKid().equals(kid));
        log.debug("WebKeysConfiguration after removing old key =" + conf.getWebKeys().getKeys());

        // Update
        conf.getWebKeys().getKeys().add(jwk);
        configurationService.merge(conf);

        return Response.ok(jwk).build();
    }

    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response deleteKey(@PathParam(ApiConstants.KID) @NotNull String kid) {
        log.debug("Key to be to be deleted - kid = " + kid);
        final Conf conf = configurationService.findConf();
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        if (jwk == null) {
            throw new NotFoundException(getNotFoundError("JWK with kid - '" + kid + "' does not exist!"));
        }

        conf.getWebKeys().getKeys().removeIf(x -> x.getKid() != null && x.getKid().equals(kid));
        configurationService.merge(conf);
        return Response.noContent().build();
    }

    private JSONWebKey getJSONWebKey(WebKeysConfiguration webkeys, String kid) {
        if (kid != null && webkeys.getKeys() != null && !webkeys.getKeys().isEmpty()) {
            return webkeys.getKeys().stream().filter(x -> x.getKid() != null && x.getKid().equals(kid)).findAny()
                    .orElse(null);
        }
        return null;
    }
}
