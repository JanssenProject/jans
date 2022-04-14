/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.register.ws.rs.RegisterValidator;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterDeleteAction {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ClientService clientService;

    @Inject
    private TokenService tokenService;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private RegisterValidator registerValidator;

    public Response delete(String clientId, String authorization, SecurityContext securityContext) {
        try {
            errorResponseFactory.validateComponentEnabled(ComponentType.REGISTRATION);
            String accessToken = tokenService.getToken(authorization);

            log.debug("Attempting to delete client: clientId = {}, registrationAccessToken = {} isSecure = {}", clientId, accessToken, securityContext.isSecure());

            if (!registerParamsValidator.validateParamsClientRead(clientId, accessToken)) {
                log.trace("Client parameters are invalid.");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "");
            }

            if (isTrue(appConfiguration.getDcrAuthorizationWithClientCredentials())) {
                registerValidator.validateAuthorizationAccessToken(accessToken, clientId);
            }

            Client client = clientService.getClient(clientId, accessToken);
            if (client == null) {
                throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, RegisterErrorResponseType.INVALID_TOKEN, "");
            }

            clientService.remove(client);

            return Response
                    .status(Response.Status.NO_CONTENT)
                    .cacheControl(ServerUtil.cacheControl(true, false))
                    .header(Constants.PRAGMA, Constants.NO_CACHE).build();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null) {
                return e.getResponse();
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "Failed to process request.");
        }
    }
}
