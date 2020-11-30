/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.ws;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.rp.ciba.CibaCallback;
import io.jans.as.rp.ciba.CibaRequestSession;
import io.jans.as.rp.service.CibaService;
import io.jans.as.rp.service.CibaSessions;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Process all callbacks done from the OP related to CIBA implementation.
 */
@Path("/")
public class CibaClientNotificationEndpointImpl implements CibaClientNotificationEndpoint {

    @Inject
    private Logger log;

    @Inject
    private CibaSessions cibaSessions;

    @Inject
    private CibaService cibaService;


    @Override
    public Response processCallback(String authorization,
                                    String requestParams,
                                    HttpServletRequest request,
                                    SecurityContext securityContext) {
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CibaCallback callback = objectMapper.readValue(requestParams, CibaCallback.class);
            if (callback.getAuthReqId() == null || ! cibaSessions.getSessions().containsKey(callback.getAuthReqId())) {
                log.error("Nothing to process, auth_req_id param is empty or doesn't exist. Value gotten: {}", callback.getAuthReqId());
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            CibaRequestSession session = cibaSessions.getSessions().get(callback.getAuthReqId());
            authorization = authorization != null ? authorization.replace("Bearer ", "") : "";
            if (!session.getClientNotificationToken().equals(authorization)) {
                log.warn("Authorization header not verified.");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            if (session.getTokenDeliveryMode() == BackchannelTokenDeliveryMode.PING) {
                cibaService.processPingCallback(requestParams, session);
            } else if (session.getTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {
                cibaService.processPushCallback(requestParams, session);
            }
            return null;
        } catch (Exception e) {
            log.error("Problems processing ciba callback", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}