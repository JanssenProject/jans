/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.session.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.session.EndSessionParamsValidator;
import org.xdi.oxauth.model.session.EndSessionResponseParam;
import org.xdi.oxauth.service.RedirectionUriService;
import org.xdi.oxauth.service.SessionIdService;
import org.xdi.oxauth.service.external.ExternalApplicationSessionService;
import org.xdi.oxauth.util.RedirectUri;
import org.xdi.oxauth.util.RedirectUtil;
import org.xdi.util.StringHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version October 1, 2015
 */
@Name("endSessionRestWebService")
public class EndSessionRestWebServiceImpl implements EndSessionRestWebService {

    @Logger
    private Log log;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private RedirectionUriService redirectionUriService;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @In
    private ExternalApplicationSessionService externalApplicationSessionService;

    @In
    private SessionIdService sessionIdService;

    @Override
    public Response requestEndSession(String idTokenHint, String postLogoutRedirectUri, String state, String sessionId,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {
        log.debug("Attempting to end session, idTokenHint: {0}, postLogoutRedirectUri: {1}, sessionId: {2}, Is Secure = {3}",
                idTokenHint, postLogoutRedirectUri, sessionId, sec.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        if (!EndSessionParamsValidator.validateParams(idTokenHint, postLogoutRedirectUri)) {
            builder = Response.status(400);
            builder.entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST));
        } else {
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
            boolean isExternalAuthenticatorLogoutPresent = false;
            boolean externalLogoutResult = false;
            if (authorizationGrant != null) {
                removeSessionId(sessionId, httpRequest, httpResponse);

                isExternalAuthenticatorLogoutPresent = externalApplicationSessionService.isEnabled();
                if (isExternalAuthenticatorLogoutPresent) {
                    externalLogoutResult = externalApplicationSessionService.executeExternalEndSessionMethods(httpRequest, authorizationGrant);
                    log.info("End session result for '{0}': '{1}'", authorizationGrant.getUser().getUserId(), "logout", externalLogoutResult);
                }
            }
            boolean isGrantAndNoExternalLogout = authorizationGrant != null && !isExternalAuthenticatorLogoutPresent;
            boolean isGrantAndExternalLogoutSuccessful = authorizationGrant != null && isExternalAuthenticatorLogoutPresent && externalLogoutResult;
            if (isGrantAndNoExternalLogout || isGrantAndExternalLogoutSuccessful) {
                authorizationGrant.revokeAllTokens();

                // Validate redirectUri
                String redirectUri = redirectionUriService.validatePostLogoutRedirectUri(authorizationGrant.getClient().getClientId(), postLogoutRedirectUri);

                if (StringUtils.isNotBlank(redirectUri)) {
                    RedirectUri redirectUriResponse = new RedirectUri(redirectUri);
                    if (StringUtils.isNotBlank(state)) {
                        redirectUriResponse.addResponseParameter(EndSessionResponseParam.STATE, state);
                    }

                    builder = RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, httpRequest);
                } else {
                    builder = Response.status(400);
                    builder.entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST));
                }
            } else {
                builder = Response.status(401);
                builder.entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_GRANT));
            }
        }
        return builder.build();
    }

    private void removeSessionId(String sessionId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String id = sessionId;
        if (StringHelper.isEmpty(id)) {
            id = sessionIdService.getSessionIdFromCookie(httpRequest);
        }

        if (StringHelper.isNotEmpty(id)) {
            SessionId ldapSessionId = sessionIdService.getSessionId(id);
            if (ldapSessionId != null) {
                boolean result = sessionIdService.remove(ldapSessionId);
                if (!result) {
                    log.error("Failed to remove session_id '{0}' from LDAP", id);
                }
            } else {
                log.error("Failed to load session from LDAP by session_id: '{0}'", id);
            }
        }

        sessionIdService.removeSessionIdCookie(httpResponse);
    }
}