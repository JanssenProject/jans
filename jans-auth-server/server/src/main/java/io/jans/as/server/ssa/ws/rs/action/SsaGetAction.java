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

    public Response get(Boolean softwareRoles, String jti, Long orgId, HttpServletRequest httpRequest) {
        log.debug("Attempting to read ssa: softwareRoles = {}, jti = '{}', orgId = {}", softwareRoles, jti, orgId);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.ok();
        try {
            final Client client = ssaRestWebServiceValidator.getClientFromSession();
            ssaRestWebServiceValidator.checkScopesPolicy(client, Arrays.asList(SsaScopeType.SSA_ADMIN.getValue(), SsaScopeType.SSA_PORTAL.getValue()));

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

    private JSONArray modifyGetScript(JSONArray jsonArray, ModifySsaResponseContext context, List<Ssa> ssaList) {
        if (!modifySsaResponseService.get(jsonArray, context)) {
            return ssaJsonService.getJSONArray(ssaList);
        }
        return jsonArray;
    }
}
