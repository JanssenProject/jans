/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.session.ws.rs;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Date;

/**
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Path("/")
public class CheckSessionStatusRestWebServiceImpl {

	public static final String SESSION_CUSTOM_STATE = "session_custom_state";

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CookieService cookieService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @GET
    @Path("/session_status")
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestCheckSessionStatus(@Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse,
                                              @Context SecurityContext securityContext) throws IOException {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.STATUS_SESSION);
        String sessionIdCookie = cookieService.getSessionIdFromCookie(httpRequest);
        log.debug("Found session '{}' cookie: '{}'", CookieService.SESSION_ID_COOKIE_NAME, sessionIdCookie);

        CheckSessionResponse response = new CheckSessionResponse("unknown", "");

        SessionId sessionId = sessionIdService.getSessionId(sessionIdCookie);
        if (sessionId != null) {
            response.setState(sessionId.getState().getValue());
            response.setAuthTime(sessionId.getAuthenticationTime());

            String sessionCustomState = sessionId.getSessionAttributes().get(SESSION_CUSTOM_STATE);
            if (StringHelper.isNotEmpty(sessionCustomState)) {
                response.setCustomState(sessionCustomState);
            }
        }

        String responseJson = ServerUtil.asJson(response);
        log.debug("Check session status response: '{}'", responseJson);

 		return Response.ok().type(MediaType.APPLICATION_JSON).entity(responseJson)
				.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate())
				.header(Constants.PRAGMA, Constants.NO_CACHE).build();
    }

    class CheckSessionResponse {

        @JsonProperty(value = "state")
        String state;

        @JsonProperty(value = "custom_state")
        String customState;

        @JsonProperty(value = "auth_time")
        Date authTime;

        public CheckSessionResponse(String state, String stateExt) {
            this.state = state;
            this.customState = stateExt;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCustomState() {
            return customState;
        }

        public void setCustomState(String customState) {
            this.customState = customState;
        }

        public Date getAuthTime() {
            return authTime;
        }

        public void setAuthTime(Date authTime) {
            this.authTime = authTime;
        }

    }

}
