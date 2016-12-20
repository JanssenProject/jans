/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.token.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.common.AbstractToken;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.token.ValidateTokenErrorResponseType;
import org.xdi.oxauth.model.token.ValidateTokenParamsValidator;
import org.xdi.oxauth.util.ServerUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for validate token REST web services
 *
 * @author Javier Rojas Blum
 * @version January 27, 2016
 */
@Name("requestValidateTokenRestWebService")
public class ValidateTokenRestWebServiceImpl implements ValidateTokenRestWebService {

    @Logger
    private Log log;

    @In
    private ApplicationAuditLogger applicationAuditLogger;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @Override
    public Response validateAccessTokenGet(String accessToken, HttpServletRequest httpRequest, SecurityContext sec) {
        return validateAccessToken(accessToken, httpRequest, sec);
    }

    @Override
    public Response validateAccessTokenPost(String accessToken, HttpServletRequest httpRequest, SecurityContext sec) {
        return validateAccessToken(accessToken, httpRequest, sec);
    }

    private Response validateAccessToken(String accessToken, HttpServletRequest httpRequest, SecurityContext sec) {
        log.debug("Attempting to validate access token: {0}, Is Secure = {1}",
                accessToken, sec.isSecure());
        ResponseBuilder builder = Response.ok();
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.TOKEN_VALIDATE);

        try {
            if (!ValidateTokenParamsValidator.validateParams(accessToken)) {
                builder = Response.status(400);
                builder.entity(errorResponseFactory.getErrorAsJson(ValidateTokenErrorResponseType.INVALID_REQUEST));
            } else {
                AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

                if (authorizationGrant != null) {
                    AbstractToken token = authorizationGrant.getAccessToken(accessToken);

                    boolean valid = token.isValid();
                    int expiresIn = token.getExpiresIn();

                    CacheControl cacheControl = new CacheControl();
                    cacheControl.setPrivate(true);
                    cacheControl.setNoTransform(false);
                    cacheControl.setNoStore(true);
                    builder.cacheControl(cacheControl);
                    builder.header("Pragma", "no-cache");
                    builder.entity(getJSonResponse(valid, expiresIn));

                    oAuth2AuditLog.setClientId(authorizationGrant.getClientId());
                    oAuth2AuditLog.setUsername(authorizationGrant.getUserId());
                    oAuth2AuditLog.setScope(StringUtils.join(authorizationGrant.getScopes(), " "));
                    oAuth2AuditLog.setSuccess(true);
                } else {
                    builder = Response.status(401);
                    builder.entity(errorResponseFactory.getErrorAsJson(ValidateTokenErrorResponseType.INVALID_GRANT));
                }
            }
        } catch (Exception e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        }
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    /**
     * Builds a JSon String with the response parameters.
     */
    public String getJSonResponse(boolean valid, Integer expiresIn) {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("valid", valid); // Required
            if (expiresIn != null) { // Optional
                jsonObj.put("expires_in", expiresIn);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj.toString();
    }
}