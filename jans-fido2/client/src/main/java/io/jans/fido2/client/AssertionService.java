/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.client;

import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionOptionsGenerate;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.common.ClientContextHeaders;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * The endpoint allows to start and finish Fido2 assertion process
 *
 * @author Yuriy Movchan
 * @version 12/21/2018
 */
public interface AssertionService {

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response authenticate(AssertionOptions assertionOptions);

    /**
     * As {@link #authenticate(AssertionOptions)}, additionally forwarding the end user's
     * connection details so metrics describe the user rather than this client.
     *
     * Pass null for either value when the caller does not hold the browser's request; the
     * header is then omitted and the server falls back to what it can observe itself.
     *
     * @param assertionOptions assertion options
     * @param clientIp end user's IP address, or null
     * @param clientUserAgent end user's browser user agent, or null
     * @return assertion options response
     */
    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response authenticate(AssertionOptions assertionOptions,
            @HeaderParam(ClientContextHeaders.CLIENT_IP) String clientIp,
            @HeaderParam(ClientContextHeaders.CLIENT_USER_AGENT) String clientUserAgent);


    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    public Response verify(AssertionResult assertionResult);

    /**
     * As {@link #verify(AssertionResult)}, additionally forwarding the end user's
     * connection details so metrics describe the user rather than this client.
     *
     * @param assertionResult assertion result
     * @param clientIp end user's IP address, or null
     * @param clientUserAgent end user's browser user agent, or null
     * @return assertion verification response
     */
    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    public Response verify(AssertionResult assertionResult,
            @HeaderParam(ClientContextHeaders.CLIENT_IP) String clientIp,
            @HeaderParam(ClientContextHeaders.CLIENT_USER_AGENT) String clientUserAgent);

}