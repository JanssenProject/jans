/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

/**
 * Provides the method to validate an existing SSA considering certain conditions.
 */
@Stateless
@Named
public class SsaValidateAction {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SsaService ssaService;

    @Inject
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    /**
     * Validates an existing SSA for a given "jti".
     *
     * @param jti Unique identifier
     * @return {@link Response} with status {@code 200 (Ok)} if SSA has been validated or
     * {@code 400 (Bad Request) with <b>invalid_jti<b/> key}, when jti does not exist, is invalid or state is in (expired, used or revoked) or
     * {@code 500 (Internal Server Error) with <b>unknown_error<b/> key}, in case an uncontrolled error occurs when processing the method.
     */
    public Response validate(String jti) throws WebApplicationException {
        log.debug("Attempting to validate ssa jti: '{}'", jti);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.ok();
        try {
            Ssa ssa = ssaRestWebServiceValidator.getValidSsaByJti(jti);
            if (ssa.getAttributes().getOneTimeUse()) {
                ssa.setState(SsaState.USED);
                ssaService.merge(ssa);
                log.info("Ssa jti: '{}', updated with status: {}", ssa.getId(), ssa.getState());
            }

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
