/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.token.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AbstractToken;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.token.ValidateTokenErrorResponseType;
import org.xdi.oxauth.model.token.ValidateTokenParamsValidator;

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
    private ErrorResponseFactory errorResponseFactory;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @Override
    public Response validateAccessTokenGet(String accessToken, SecurityContext sec) {
        return validateAccessToken(accessToken, sec);
    }

    @Override
    public Response validateAccessTokenPost(String accessToken, SecurityContext sec) {
        return validateAccessToken(accessToken, sec);
    }

    private Response validateAccessToken(String accessToken, SecurityContext sec) {
        log.debug("Attempting to validate access token: {0}, Is Secure = {1}",
                accessToken, sec.isSecure());
        ResponseBuilder builder = Response.ok();

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
                } else {
                    builder = Response.status(401);
                    builder.entity(errorResponseFactory.getErrorAsJson(ValidateTokenErrorResponseType.INVALID_GRANT));
                }
            }
        } catch (Exception e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        }

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