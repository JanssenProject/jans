/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.ssa.ws.rs.SsaJsonService;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Provides the method to get JWT of SSA existing based on certain conditions.
 */
@Stateless
@Named
public class SsaGetJwtAction {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SsaJsonService ssaJsonService;

    @Inject
    private SsaService ssaService;

    @Inject
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    /**
     * Get JWT from existing active SSA based on "jti".
     *
     * <p>
     * Method will return the following exceptions:
     * - {@link WebApplicationException} with status {@code 401} if this functionality is not enabled, request has to have at least scope "ssa.admin".
     * - {@link WebApplicationException} with status {@code 422} if the SSA does not exist, is expired or used.
     * - {@link WebApplicationException} with status {@code 500} in case an uncontrolled error occurs when processing the method.
     * </p>
     *
     * @param jti Unique identifier
     * @return {@link Response} with status {@code 200 (Ok)} and the body containing JWT of SSA.
     */
    public Response getJwtSsa(String jti) {
        log.debug("Attempting to get JWT of SSA, jti: {}", jti);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.ok();
        try {
            final Client client = ssaRestWebServiceValidator.getClientFromSession();
            ssaRestWebServiceValidator.checkScopesPolicy(client, SsaScopeType.SSA_ADMIN.getValue());

            Ssa ssa = ssaRestWebServiceValidator.getValidSsaByJti(jti);

            Jwt jwt = ssaService.generateJwt(ssa);
            JSONObject jsonResponse = ssaJsonService.getJSONObject(jwt.toString());
            builder.entity(ssaJsonService.jsonObjectToString(jsonResponse));

        } catch (WebApplicationException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw e;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, SsaErrorResponseType.UNKNOWN_ERROR, "Unknown error");
        }

        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header(Constants.PRAGMA, Constants.NO_CACHE);
        builder.type(MediaType.APPLICATION_JSON_TYPE);
        return builder.build();
    }
}
