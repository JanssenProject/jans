/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.service.external.ModifySsaResponseService;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import io.jans.as.server.ssa.ws.rs.SsaContextBuilder;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * Provides the method to revoke an existing SSA considering certain conditions.
 */
@Stateless
@Named
public class SsaRevokeAction {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SsaService ssaService;

    @Inject
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Inject
    private ModifySsaResponseService modifySsaResponseService;

    @Inject
    private SsaContextBuilder ssaContextBuilder;

    /**
     * Revoked existing active SSA based on "jti" or "org_id".
     *
     * <p>
     * Method will return a {@link WebApplicationException} with status {@code 401} if this functionality is not enabled,
     * request has to have at least scope "ssa.admin",
     * {@link WebApplicationException} with status {@code 406} if "jti" or "org_id" filters are not valid,
     * {@link WebApplicationException} with status {@code 422} if the SSA does not exist, has expired or is no longer active,
     * it will also return a {@link WebApplicationException} with status code {@code 500} in case an uncontrolled
     * error occurs when processing the method.
     * </p>
     * <p>
     * After revoking the SSA, it calls custom script to perform an additional process.
     * <a href="https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/extension/ssa_modify_response/ssa_modify_response.py">SSA Custom Script</a>,
     * method revoke.
     * </p>
     * <p>
     * Method updates the list of SSA and marks them as REVOKED in the database.
     * </p>
     *
     * @param jti         Unique identifier
     * @param orgId       Organization ID
     * @param httpRequest Http request
     * @return {@link Response} with status {@code 200 (Ok)} if SSA has been revoked.
     */
    public Response revoke(String jti, Long orgId, HttpServletRequest httpRequest) {
        log.debug("Attempting to revoke ssa, jti: '{}', orgId: {}", jti, orgId);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.ok();
        try {
            if (isNotValidParam(jti, orgId)) {
                return ssaService.createNotAcceptableResponse().build();
            }

            final Client client = ssaRestWebServiceValidator.getClientFromSession();
            ssaRestWebServiceValidator.checkScopesPolicy(client, SsaScopeType.SSA_ADMIN.getValue());

            final List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, SsaState.ACTIVE, client.getClientId(), client.getScopes());
            if (ssaList.isEmpty()) {
                return ssaService.createUnprocessableEntityResponse().build();
            }
            for (Ssa ssa : ssaList) {
                ssa.setState(SsaState.REVOKED);
                ssaService.merge(ssa);
                log.info("Ssa jti: '{}' updated status to '{}'", ssa.getId(), ssa.getState().getValue());
            }

            ModifySsaResponseContext context = ssaContextBuilder.buildModifySsaResponseContext(httpRequest, client);
            modifySsaResponseService.revoke(ssaList, context);

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

    /**
     * Validate "jti" or "org_id" parameters
     *
     * @param jti   Unique identifier
     * @param orgId Organization ID
     * @return true if the parameters are valid or false otherwise.
     */
    private boolean isNotValidParam(String jti, Long orgId) {
        return StringUtils.isBlank(jti) && orgId == null;
    }
}
