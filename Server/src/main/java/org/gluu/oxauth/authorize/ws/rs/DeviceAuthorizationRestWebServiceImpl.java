/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.authorize.ws.rs;

import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.authorize.DeviceAuthorizationResponseParam;
import org.gluu.oxauth.model.authorize.ScopeChecker;
import org.gluu.oxauth.model.common.DeviceAuthorizationCacheControl;
import org.gluu.oxauth.model.common.DeviceAuthorizationStatus;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.session.SessionClient;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.oxauth.security.Identity;
import org.gluu.oxauth.service.*;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.util.StringHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.gluu.oxauth.model.authorize.DeviceAuthorizationResponseParam.*;
import static org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType.INVALID_REQUEST;
import static org.gluu.oxauth.model.token.TokenErrorResponseType.INVALID_CLIENT;

/**
 * Implementation for device authorization rest service.
 */
@Path("/")
public class DeviceAuthorizationRestWebServiceImpl implements DeviceAuthorizationRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private Identity identity;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DeviceAuthorizationService deviceAuthorizationService;

    @Context
    private HttpServletRequest servletRequest;


    @Override
    public Response deviceAuthorization(String clientId, String scope, HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse, SecurityContext securityContext) {
        scope = ServerUtil.urlDecode(scope); // it may be encoded

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.DEVICE_CODE_AUTHORIZATION);
        oAuth2AuditLog.setClientId(clientId);
        oAuth2AuditLog.setScope(scope);

        try {
            log.debug("Attempting to request device codes: clientId = {}, scope = {}", clientId, scope);
            SessionClient sessionClient = identity.getSessionClient();
            Client client = null;
            if (sessionClient != null) {
                client = sessionClient.getClient();
            }
            if (client == null) {
                throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, INVALID_CLIENT, "");
            }
            if (!deviceAuthorizationService.hasDeviceCodeCompatibility(client)) {
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, INVALID_REQUEST, "");
            }

            List<String> scopes = new ArrayList<>();
            if (StringHelper.isNotEmpty(scope)) {
                Set<String> grantedScopes = scopeChecker.checkScopesPolicy(client, scope);
                scopes.addAll(grantedScopes);
            }

            String userCode = StringUtils.generateRandomReadableCode((byte) 8); // Entropy 20^8 which is suggested in the RFC8628 section 6.1
            String deviceCode = StringUtils.generateRandomCode((byte) 24); // Entropy 160 bits which is over userCode entropy based on RFC8628 section 5.2
            URI verificationUri = UriBuilder.fromUri(appConfiguration.getIssuer()).path("device-code").build();
            URI verificationUriComplete = UriBuilder.fromUri(verificationUri).queryParam(DeviceAuthorizationResponseParam.USER_CODE, userCode).build();
            int expiresIn = appConfiguration.getDeviceAuthorizationRequestExpiresIn();
            int interval = appConfiguration.getDeviceAuthorizationTokenPoolInterval();
            long lastAccess = System.currentTimeMillis();
            DeviceAuthorizationStatus status = DeviceAuthorizationStatus.PENDING;

            DeviceAuthorizationCacheControl deviceAuthorizationCacheControl = new DeviceAuthorizationCacheControl(userCode,
                    deviceCode, client, scopes, verificationUri, verificationUriComplete, expiresIn, interval, lastAccess, status);
            deviceAuthorizationService.saveInCache(deviceAuthorizationCacheControl);
            log.info("Device authorization flow initiated, userCode: {}, deviceCode: {}, clientId: {}, verificationUri: {}, expiresIn: {}, interval: {}", userCode, deviceCode, clientId, verificationUri, expiresIn, interval);

            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            return Response.ok()
                    .entity(getResponseJSONObject(deviceAuthorizationCacheControl).toString(4).replace("\\/", "/"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        } catch (Exception e) {
            log.error("Problems processing device authorization init flow, clientId: {}, scope: {}", clientId, scope);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private JSONObject getResponseJSONObject(DeviceAuthorizationCacheControl deviceAuthorizationCacheControl) throws JSONException {
        JSONObject responseJsonObject = new JSONObject();

        responseJsonObject.put(DEVICE_CODE, deviceAuthorizationCacheControl.getDeviceCode());
        responseJsonObject.put(USER_CODE, deviceAuthorizationCacheControl.getUserCode());
        responseJsonObject.put(VERIFICATION_URI, deviceAuthorizationCacheControl.getVerificationUri());
        responseJsonObject.put(VERIFICATION_URI_COMPLETE, deviceAuthorizationCacheControl.getVerificationUriComplete());
        responseJsonObject.put(EXPIRES_IN, deviceAuthorizationCacheControl.getExpiresIn());
        responseJsonObject.put(INTERVAL, deviceAuthorizationCacheControl.getInterval());

        return responseJsonObject;
    }

}