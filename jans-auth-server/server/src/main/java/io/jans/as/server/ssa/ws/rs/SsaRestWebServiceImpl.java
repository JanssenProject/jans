/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import io.jans.as.server.ssa.ws.rs.action.SsaCreateAction;
import io.jans.as.server.ssa.ws.rs.action.SsaGetAction;
import io.jans.as.server.ssa.ws.rs.action.SsaRevokeAction;
import io.jans.as.server.ssa.ws.rs.action.SsaValidateAction;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class SsaRestWebServiceImpl implements SsaRestWebService {

    @Inject
    private SsaCreateAction ssaCreateAction;

    @Inject
    private SsaGetAction ssaGetAction;

    @Inject
    private SsaValidateAction ssaValidateAction;

    @Inject
    private SsaRevokeAction ssaRevokeAction;

    @Override
    public Response create(String requestParams, HttpServletRequest httpRequest) {
        return ssaCreateAction.create(requestParams, httpRequest);
    }

    @Override
    public Response get(Boolean softwareRoles, String jti, Long orgId, HttpServletRequest httpRequest) {
        return ssaGetAction.get(softwareRoles, jti, orgId, httpRequest);
    }

    @Override
    public Response validate(String jti) {
        return ssaValidateAction.validate(jti);
    }

    @Override
    public Response revoke(String jti, Long orgId, HttpServletRequest httpRequest) {
        return ssaRevokeAction.revoke(jti, orgId, httpRequest);
    }
}