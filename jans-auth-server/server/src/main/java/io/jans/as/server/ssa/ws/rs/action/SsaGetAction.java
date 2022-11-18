/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.service.external.ModifySsaResponseService;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import io.jans.as.server.ssa.ws.rs.SsaContextBuilder;
import io.jans.as.server.ssa.ws.rs.SsaJsonService;
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
import org.json.JSONArray;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Provides the method to get existing SSAs based on certain conditions.
 */
@Stateless
@Named
public class SsaGetAction {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SsaJsonService ssaJsonService;

    @Inject
    private SsaService ssaService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttributeService attributeService;

    @Inject
    private ModifySsaResponseService modifySsaResponseService;

    @Inject
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Inject
    private SsaContextBuilder ssaContextBuilder;

    /**
     * Get existing active SSA based on "jti" or "org_id".
     * <p>
     * Method will return a {@link WebApplicationException} with status {@code 401} if this functionality is not enabled,
     * request has to have at least scope "ssa.admin" or "ssa.portal",
     * it will also return a {@link WebApplicationException} with status {@code 500} in case an uncontrolled
     * error occurs when processing the method.
     * </p>
     * <p>
     * Response of this method can be modified using the following custom script
     * <a href="https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/extension/ssa_modify_response/ssa_modify_response.py">SSA Custom Script</a>,
     * method get.
     * </p>
     * <p>
     * Method also performs the search based on the scope, if the scope is "ssa.admin" it is based on all SSA records,
     * but if the scope is "ssa.portal", then it only returns the SSA list corresponding to the same org.
     * </p>
     *
     * @param jti         Unique identifier
     * @param orgId       Organization ID
     * @param httpRequest Http request
     * @return {@link Response} with status {@code 200 (Ok)} and the body containing the list of SSAs.
     */
    public Response get(String jti, Long orgId, HttpServletRequest httpRequest) {
        log.debug("Attempting to read ssa: softwareRoles = {}, orgId = {}", jti, orgId);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.ok();
        try {
            final Client client = ssaRestWebServiceValidator.getClientFromSession();
            ssaRestWebServiceValidator.checkScopesPolicy(client, Arrays.asList(SsaScopeType.SSA_ADMIN.getValue(), SsaScopeType.SSA_PORTAL.getValue(), SsaScopeType.SSA_DEVELOPER.getValue()));

            final List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, SsaState.ACTIVE, client.getClientId(), client.getScopes());

            JSONArray jsonArray = ssaJsonService.getJSONArray(ssaList);
            ModifySsaResponseContext context = ssaContextBuilder.buildModifySsaResponseContext(httpRequest, null, client, appConfiguration, attributeService);
            jsonArray = modifyGetScript(jsonArray, context, ssaList);
            builder.entity(ssaJsonService.jsonArrayToString(jsonArray));

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
     * Modify the JSONArray through the custom script.
     *
     * <p>
     * Method returns the same json array in case the script execution returned false.
     * </p>
     *
     * @param jsonArray Json array with list of SSA
     * @param context   Modify ssa response context
     * @param ssaList   List of SSA
     * @return Modified Json array.
     */
    private JSONArray modifyGetScript(JSONArray jsonArray, ModifySsaResponseContext context, List<Ssa> ssaList) {
        if (!modifySsaResponseService.get(jsonArray, context)) {
            return ssaJsonService.getJSONArray(ssaList);
        }
        return jsonArray;
    }
}
