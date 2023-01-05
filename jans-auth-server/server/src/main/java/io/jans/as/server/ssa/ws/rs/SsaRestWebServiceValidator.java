/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ScopeService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides methods to validate different params about SSA.
 */
@Named
@Stateless
public class SsaRestWebServiceValidator {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private Identity identity;

    @Inject
    private ScopeService scopeService;

    /**
     * Get client from session
     *
     * @return {@link Client} if obtained.
     * @throws WebApplicationException with status {@code 401} and key <b>INVALID_CLIENT</b> if the client cannot
     *                                 be obtained.
     */
    public Client getClientFromSession() throws WebApplicationException {
        SessionClient sessionClient = identity.getSessionClient();
        if (sessionClient != null) {
            log.debug("Client: {}, obtained from session", sessionClient.getClient().getClientId());
            return sessionClient.getClient();
        }
        throw errorResponseFactory.createBadRequestException(SsaErrorResponseType.INVALID_CLIENT, "Invalid client");
    }

    /**
     * Check if the client has the given scope.
     *
     * @param client Client to check scope
     * @param scope  Scope to validate
     * @throws WebApplicationException with status {@code 401} and key <b>UNAUTHORIZED_CLIENT</b> if you don't have the scope.
     */
    public void checkScopesPolicy(Client client, String scope) throws WebApplicationException {
        List<String> scopes = scopeService.getScopeIdsByDns(Arrays.stream(client.getScopes()).collect(Collectors.toList()));
        if (!scopes.contains(scope))
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, SsaErrorResponseType.UNAUTHORIZED_CLIENT, "Unauthorized client");
    }

    /**
     * Check if the client has at least one scope from the list of scopes.
     *
     * @param client    Client to check scope
     * @param scopeList List of scope to validated
     * @throws WebApplicationException with status {@code 401} and key <b>UNAUTHORIZED_CLIENT</b> if you don't have the scope.
     */
    public void checkScopesPolicy(Client client, List<String> scopeList) throws WebApplicationException {
        if (client == null || scopeList == null || scopeList.isEmpty()) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, SsaErrorResponseType.UNAUTHORIZED_CLIENT, "Unauthorized client");
        }
        List<String> scopes = scopeService.getScopeIdsByDns(Arrays.stream(client.getScopes()).collect(Collectors.toList()));
        if (scopeList.stream().noneMatch(scopes::contains)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, SsaErrorResponseType.UNAUTHORIZED_CLIENT, "Unauthorized client");
        }
    }
}
