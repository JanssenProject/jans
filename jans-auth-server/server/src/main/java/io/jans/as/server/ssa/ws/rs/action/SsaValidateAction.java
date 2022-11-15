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
import io.jans.as.server.ssa.ws.rs.SsaService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.TimeZone;

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

    /**
     * Validates an existing SSA for a given "jti".
     *
     * <p>
     * Method will return a {@link WebApplicationException} with status {@code 422} if the SSA does not exist,
     * has been expired or is no longer active,
     * it will also return a {@link WebApplicationException} with status {@code 500} in case an uncontrolled
     * error occurs when processing the method.
     * </p>
     *
     * @param jti Unique identifier
     * @return {@link Response} with status {@code 200} (Ok) if SSA has been validated.
     */
    public Response validate(String jti) {
        log.debug("Attempting to validate ssa jti: '{}'", jti);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.ok();
        try {
            Ssa ssa = ssaService.findSsaByJti(jti);
            if (ssa == null ||
                    Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().after(ssa.getExpirationDate()) ||
                    !ssa.getState().equals(SsaState.ACTIVE)) {
                log.warn("Ssa jti: '{}' is null or status (expired, used or revoked)", jti);
                return ssaService.createUnprocessableEntityResponse().build();
            }
            if (ssa.getAttributes().getOneTimeUse()) {
                ssa.setState(SsaState.USED);
                ssaService.merge(ssa);
                log.info("Ssa jti: '{}', updated with status: {}", ssa.getId(), ssa.getState());
            }

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
