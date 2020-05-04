/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gluu.oxauth.client.BaseRequest;
import org.gluu.oxauth.model.ciba.CibaCallback;
import org.gluu.oxauth.model.ciba.CibaRequestSession;
import org.gluu.oxauth.model.ciba.PingCibaCallback;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.service.CibaService;
import org.gluu.oxauth.service.CibaSessions;
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
            ObjectMapper objectMapper = new ObjectMapper();
            CibaCallback callback = objectMapper.readValue(requestParams, CibaCallback.class);
            if (callback.getAuthReqId() == null || ! cibaSessions.getSessions().containsKey(callback.getAuthReqId())) {
                log.error("Nothing to process, auth_req_id param is empty or doesn't exist. Value gotten: {}", callback.getAuthReqId());
                return null;
            }
            CibaRequestSession session = cibaSessions.getSessions().get(callback.getAuthReqId());
            String validAuthorization = BaseRequest.getEncodedCredentials(session.getClientId(), session.getClientSecret());
            if (validAuthorization == null || !validAuthorization.equals(authorization)) {
                log.warn("Authorization header not verified.");
                return null;
            }
            if (session.getTokenDeliveryMode() == BackchannelTokenDeliveryMode.PING) {
                PingCibaCallback pingCibaCallback = objectMapper.readValue(requestParams, PingCibaCallback.class);
                cibaService.processPingCallback(pingCibaCallback, session);
            } else if (session.getTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {
                log.warn("Not implemented yet...");
            }
            return null;
        } catch (Exception e) {
            log.error("Problems processing ciba callback", e);
            return null;
        }
    }

}