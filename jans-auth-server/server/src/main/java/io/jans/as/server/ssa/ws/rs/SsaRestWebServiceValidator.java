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
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public Client getClientFromSession() {
        SessionClient sessionClient = identity.getSessionClient();
        if (sessionClient != null) {
            log.debug("Client: {}, obtained from session", sessionClient.getClient().getClientId());
            return sessionClient.getClient();
        }
        throw errorResponseFactory.createBadRequestException(SsaErrorResponseType.INVALID_CLIENT, "Invalid client");
    }

    public void checkScopesPolicy(Client client, String scope) {
        List<String> scopes = scopeService.getScopeIdsByDns(Arrays.stream(client.getScopes()).collect(Collectors.toList()));
        if (!scopes.contains(scope))
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, SsaErrorResponseType.UNAUTHORIZED_CLIENT, "Unauthorized client");
    }

    public void checkScopesPolicy(Client client, List<String> scopeList) {
        if (client == null || scopeList == null || scopeList.isEmpty()) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, SsaErrorResponseType.UNAUTHORIZED_CLIENT, "Unauthorized client");
        }
        List<String> scopes = scopeService.getScopeIdsByDns(Arrays.stream(client.getScopes()).collect(Collectors.toList()));
        if (scopeList.stream().noneMatch(scopes::contains)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, SsaErrorResponseType.UNAUTHORIZED_CLIENT, "Unauthorized client");
        }
    }
}
