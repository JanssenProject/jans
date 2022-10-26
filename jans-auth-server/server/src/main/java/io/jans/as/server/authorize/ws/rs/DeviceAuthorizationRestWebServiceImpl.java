/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.DeviceAuthorizationResponseParam;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.DeviceAuthorizationStatus;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.DeviceAuthorizationService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.StringHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.DEVICE_CODE;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.EXPIRES_IN;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.INTERVAL;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.USER_CODE;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.VERIFICATION_URI;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.VERIFICATION_URI_COMPLETE;
import static io.jans.as.model.token.TokenErrorResponseType.INVALID_CLIENT;
import static io.jans.as.model.token.TokenErrorResponseType.INVALID_GRANT;

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

    @Inject
    private ClientService clientService;

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
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.DEVICE_AUTHZ);

            SessionClient sessionClient = identity.getSessionClient();
            Client client = sessionClient != null ? sessionClient.getClient() : null;
            if (client == null) {
                client = clientService.getClient(clientId);
                if (!clientService.isPublic(client)) {
                    log.trace("Client is not public and not authenticated. Skip device authorization, clientId: {}", clientId);
                    throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, INVALID_CLIENT, "");
                }
            }
            if (client == null) {
                log.trace("Client is not unknown. Skip revoking.");
                throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, INVALID_CLIENT, "");
            }

            if (!deviceAuthorizationService.hasDeviceCodeCompatibility(client)) {
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, INVALID_GRANT, "");
            }

            List<String> scopes = new ArrayList<>();
            if (StringHelper.isNotEmpty(scope)) {
                Set<String> grantedScopes = scopeChecker.checkScopesPolicy(client, scope);
                scopes.addAll(grantedScopes);
            }

            String userCode = StringUtils.generateRandomReadableCode((byte) 8); // Entropy 20^8 which is suggested in the RFC8628 section 6.1
            String deviceCode = StringUtils.generateRandomCode((byte) 24); // Entropy 160 bits which is over userCode entropy based on RFC8628 section 5.2
            URI verificationUri = UriBuilder.fromUri(appConfiguration.getIssuer()).path("device-code").build();
            int expiresIn = appConfiguration.getDeviceAuthzRequestExpiresIn();
            int interval = appConfiguration.getDeviceAuthzTokenPollInterval();
            long lastAccess = System.currentTimeMillis();
            DeviceAuthorizationStatus status = DeviceAuthorizationStatus.PENDING;

            DeviceAuthorizationCacheControl deviceAuthorizationCacheControl = new DeviceAuthorizationCacheControl(userCode,
                    deviceCode, client, scopes, verificationUri, expiresIn, interval, lastAccess, status);
            deviceAuthorizationService.saveInCache(deviceAuthorizationCacheControl, true, true);
            log.info("Device authorization flow initiated, userCode: {}, deviceCode: {}, clientId: {}, verificationUri: {}, expiresIn: {}, interval: {}", userCode, deviceCode, clientId, verificationUri, expiresIn, interval);

            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            return Response.ok()
                    .entity(getResponseJSONObject(deviceAuthorizationCacheControl).toString(4).replace("\\/", "/"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Exception e) {
            log.error("Problems processing device authorization init flow, clientId: {}, scope: {}", clientId, scope, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private JSONObject getResponseJSONObject(DeviceAuthorizationCacheControl deviceAuthorizationCacheControl) throws JSONException {
        URI verificationUriComplete = UriBuilder.fromUri(deviceAuthorizationCacheControl.getVerificationUri())
                .queryParam(DeviceAuthorizationResponseParam.USER_CODE, deviceAuthorizationCacheControl.getUserCode())
                .build();

        JSONObject responseJsonObject = new JSONObject();

        responseJsonObject.put(DEVICE_CODE, deviceAuthorizationCacheControl.getDeviceCode());
        responseJsonObject.put(USER_CODE, deviceAuthorizationCacheControl.getUserCode());
        responseJsonObject.put(VERIFICATION_URI, deviceAuthorizationCacheControl.getVerificationUri());
        responseJsonObject.put(VERIFICATION_URI_COMPLETE, verificationUriComplete.toString());
        responseJsonObject.put(EXPIRES_IN, deviceAuthorizationCacheControl.getExpiresIn());
        responseJsonObject.put(INTERVAL, deviceAuthorizationCacheControl.getInterval());

        return responseJsonObject;
    }

}