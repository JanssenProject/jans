/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import io.jans.as.server.ssa.ws.rs.action.*;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Implements all methods of the {@link SsaRestWebService} interface.
 */
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

    @Inject
    private SsaGetJwtAction ssaGetJwtAction;

    /**
     * Creates an SSA from the requested parameters.
     * <p>
     * Method calls the action where the SSA creation logic is implemented.
     * <p/>
     *
     * @param requestParams Valid json
     * @param httpRequest   Http request object
     * @return {@link Response} with status {@code 201} (Created) and with body the ssa token (jwt).
     */
    @Override
    public Response create(String requestParams, HttpServletRequest httpRequest) {
        return ssaCreateAction.create(requestParams, httpRequest);
    }

    /**
     * Get existing active SSA based on "jti" or "org_id".
     * <p>
     * Method calls the action where the SSA get logic is implemented.
     * <p/>
     *
     * @param jti         Unique identifier
     * @param orgId       Organization ID
     * @param httpRequest Http request
     * @return {@link Response} with status {@code 200 (Ok)} and with body List of SSA.
     */
    @Override
    public Response get(String jti, String orgId, HttpServletRequest httpRequest) {
        return ssaGetAction.get(jti, orgId, httpRequest);
    }

    /**
     * Validate existing active SSA based on "jti".
     * <p>
     * Method calls the action where the SSA validate logic is implemented.
     * <p/>
     *
     * @param jti Unique identifier
     * @return {@link Response} with status {@code 200} (Ok) if SSA has been validated.
     */
    @Override
    public Response validate(String jti) {
        return ssaValidateAction.validate(jti);
    }

    /**
     * Revoked existing active SSA based on "jti" or "org_id".
     * <p>
     * Method calls the action where the SSA revoke logic is implemented.
     * </p>
     *
     * @param jti         Unique identifier
     * @param orgId       Organization ID
     * @param httpRequest Http request
     * @return {@link Response} with status {@code 200 (Ok)} if SSA has been revoked.
     */
    @Override
    public Response revoke(String jti, String orgId, HttpServletRequest httpRequest) {
        return ssaRevokeAction.revoke(jti, orgId, httpRequest);
    }

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
    @Override
    public Response getSsaJwtByJti(String jti) {
        return ssaGetJwtAction.getJwtSsa(jti);
    }
}