/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs;

import io.jans.as.server.register.ws.rs.action.RegisterCreateAction;
import io.jans.as.server.register.ws.rs.action.RegisterDeleteAction;
import io.jans.as.server.register.ws.rs.action.RegisterReadAction;
import io.jans.as.server.register.ws.rs.action.RegisterUpdateAction;
import io.jans.as.server.service.MetricService;
import io.jans.model.metric.MetricType;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Implementation for register REST web services.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version March 29, 2022
 */
@Path("/")
public class RegisterRestWebServiceImpl implements RegisterRestWebService {

    @Inject
    private MetricService metricService;

    @Inject
    private RegisterCreateAction registerCreateAction;

    @Inject
    private RegisterUpdateAction registerUpdateAction;

    @Inject
    private RegisterReadAction registerReadAction;

    @Inject
    private RegisterDeleteAction registerDeleteAction;

    @Override
    public Response requestRegister(String requestParams, HttpServletRequest httpRequest, SecurityContext securityContext) {
        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.DYNAMIC_CLIENT_REGISTRATION_RATE).time();
        try {
            return registerCreateAction.createClient(requestParams, httpRequest, securityContext);
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public Response requestClientUpdate(String requestParams, String clientId, String authorization, HttpServletRequest httpRequest, SecurityContext securityContext) {
        return registerUpdateAction.updateClient(requestParams, clientId, authorization, httpRequest, securityContext);
    }

    @Override
    public Response requestClientRead(String clientId, String authorization, HttpServletRequest httpRequest,
                                      SecurityContext securityContext) {
        return registerReadAction.readClient(clientId, authorization, httpRequest, securityContext);
    }

    @Override
    public Response delete(String clientId, String authorization, HttpServletRequest httpRequest,  SecurityContext securityContext) {
        return registerDeleteAction.delete(clientId, authorization, httpRequest, securityContext);
    }
}