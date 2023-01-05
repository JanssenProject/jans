/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

import io.jans.as.client.JwkClient;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.DefaultErrorResponse;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidClaimException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.authorize.ws.rs.AuthorizeRestWebServiceValidator;
import io.jans.as.server.ciba.CIBAAuthorizeParamsValidatorService;
import io.jans.as.server.ciba.CIBAEndUserNotificationService;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.CibaRequestCacheControl;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_CLIENT;
import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.UNAUTHORIZED_END_USER_DEVICE;
import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID;
import static io.jans.as.model.ciba.BackchannelAuthenticationResponseParam.AUTH_REQ_ID;
import static io.jans.as.model.ciba.BackchannelAuthenticationResponseParam.EXPIRES_IN;
import static io.jans.as.model.ciba.BackchannelAuthenticationResponseParam.INTERVAL;

/**
 * Implementation for request backchannel authorization through REST web services.
 *
 * @author Javier Rojas Blum
 * @version April 22, 2020
 */
@Path("/")
public class BackchannelAuthorizeRestWebServiceImpl implements BackchannelAuthorizeRestWebService {

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private UserService userService;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CIBAAuthorizeParamsValidatorService cibaAuthorizeParamsValidatorService;

    @Inject
    private CIBAEndUserNotificationService cibaEndUserNotificationService;

    @Inject
    private CibaRequestService cibaRequestService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Override
    public Response requestBackchannelAuthorizationPost(
            String clientId, String scope, String clientNotificationToken, String acrValues, String loginHintToken,
            String idTokenHint, String loginHint, String bindingMessage, String userCodeParam, Integer requestedExpiry,
            String request, String requestUri, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, SecurityContext securityContext) {
        scope = ServerUtil.urlDecode(scope); // it may be encoded

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.BACKCHANNEL_AUTHENTICATION);
        oAuth2AuditLog.setClientId(clientId);
        oAuth2AuditLog.setScope(scope);

        // ATTENTION : please do not add more parameter in this debug method because it will not work with Seam 2.2.2.Final,
        // there is limit of 10 parameters (hardcoded), see: org.jboss.seam.core.Interpolator#interpolate
        log.debug("Attempting to request backchannel authorization: "
                        + "clientId = {}, scope = {}, clientNotificationToken = {}, acrValues = {}, loginHintToken = {}, "
                        + "idTokenHint = {}, loginHint = {}, bindingMessage = {}, userCodeParam = {}, requestedExpiry = {}, "
                        + "request= {}",
                clientId, scope, clientNotificationToken, acrValues, loginHintToken,
                idTokenHint, loginHint, bindingMessage, userCodeParam, requestedExpiry, request);
        log.debug("Attempting to request backchannel authorization: "
                + "isSecure = {}", securityContext.isSecure());

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.CIBA);

        Response.ResponseBuilder builder = Response.ok();

        SessionClient sessionClient = identity.getSessionClient();
        Client client = null;
        if (sessionClient != null) {
            client = sessionClient.getClient();
        }

        if (client == null) {
            builder = Response.status(Response.Status.UNAUTHORIZED.getStatusCode()); // 401
            builder.entity(errorResponseFactory.getErrorAsJson(INVALID_CLIENT));
            return builder.build();
        }

        if (!cibaRequestService.hasCibaCompatibility(client)) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 401
            builder.entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST));
            return builder.build();
        }

        List<String> scopes = new ArrayList<>();
        if (StringHelper.isNotEmpty(scope)) {
            Set<String> grantedScopes = scopeChecker.checkScopesPolicy(client, scope);
            scopes.addAll(grantedScopes);
        }

        JwtAuthorizationRequest jwtRequest = null;
        if (StringUtils.isNotBlank(request) || StringUtils.isNotBlank(requestUri)) {
            jwtRequest = JwtAuthorizationRequest.createJwtRequest(request, requestUri,
                    client, null, cryptoProvider, appConfiguration);
            if (jwtRequest == null) {
                log.error("The JWT couldn't be processed");
                builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                builder.entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST));
                throw new WebApplicationException(builder.build());
            }
            authorizeRestWebServiceValidator.validateCibaRequestObject(jwtRequest, client.getClientId());
            // JWT wins
            if (!jwtRequest.getScopes().isEmpty()) {
                scopes.addAll(scopeChecker.checkScopesPolicy(client, jwtRequest.getScopes()));
            }
            if (StringUtils.isNotBlank(jwtRequest.getClientNotificationToken())) {
                clientNotificationToken = jwtRequest.getClientNotificationToken();
            }
            if (StringUtils.isNotBlank(jwtRequest.getAcrValues())) {
                acrValues = jwtRequest.getAcrValues();
            }
            if (StringUtils.isNotBlank(jwtRequest.getLoginHintToken())) {
                loginHintToken = jwtRequest.getLoginHintToken();
            }
            if (StringUtils.isNotBlank(jwtRequest.getIdTokenHint())) {
                idTokenHint = jwtRequest.getIdTokenHint();
            }
            if (StringUtils.isNotBlank(jwtRequest.getLoginHint())) {
                loginHint = jwtRequest.getLoginHint();
            }
            if (StringUtils.isNotBlank(jwtRequest.getBindingMessage())) {
                bindingMessage = jwtRequest.getBindingMessage();
            }
            if (StringUtils.isNotBlank(jwtRequest.getUserCode())) {
                userCodeParam = jwtRequest.getUserCode();
            }
            if (jwtRequest.getRequestedExpiry() != null) {
                requestedExpiry = jwtRequest.getRequestedExpiry();
            } else if (jwtRequest.getExp() != null) {
                requestedExpiry = Math.toIntExact(jwtRequest.getExp() - System.currentTimeMillis() / 1000);
            }
        }
        if (appConfiguration.isFapi() && jwtRequest == null) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            builder.entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST));
            return builder.build();
        }
        User user = null;
        try {
            if (Strings.isNotBlank(loginHint)) { // login_hint
                user = userService.getUniqueUserByAttributes(appConfiguration.getBackchannelLoginHintClaims(), loginHint);
            } else if (Strings.isNotBlank(idTokenHint)) { // id_token_hint
                AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
                if (authorizationGrant == null) {
                    builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                    builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
                    return builder.build();
                }
                user = authorizationGrant.getUser();
            }
            if (Strings.isNotBlank(loginHintToken)) { // login_hint_token
                Jwt jwt = Jwt.parse(loginHintToken);

                SignatureAlgorithm algorithm = jwt.getHeader().getSignatureAlgorithm();
                String keyId = jwt.getHeader().getKeyId();

                if (algorithm == null || Strings.isBlank(keyId)) {
                    builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                    builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
                    return builder.build();
                }

                boolean validSignature = false;
                if (algorithm.getFamily() == AlgorithmFamily.RSA) {
                    RSAPublicKey publicKey = JwkClient.getRSAPublicKey(client.getJwksUri(), keyId);
                    RSASigner rsaSigner = new RSASigner(algorithm, publicKey);
                    validSignature = rsaSigner.validate(jwt);
                } else if (algorithm.getFamily() == AlgorithmFamily.EC) {
                    ECDSAPublicKey publicKey = JwkClient.getECDSAPublicKey(client.getJwksUri(), keyId);
                    ECDSASigner ecdsaSigner = new ECDSASigner(algorithm, publicKey);
                    validSignature = ecdsaSigner.validate(jwt);
                }
                if (!validSignature) {
                    builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                    builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
                    return builder.build();
                }

                JSONObject subject = jwt.getClaims().getClaimAsJSON("subject");
                if (subject == null || !subject.has("subject_type") || !subject.has(subject.getString("subject_type"))) {
                    builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                    builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
                    return builder.build();
                }

                String subjectTypeKey = subject.getString("subject_type");
                String subjectTypeValue = subject.getString(subjectTypeKey);

                user = userService.getUniqueUserByAttributes(appConfiguration.getBackchannelLoginHintClaims(), subjectTypeValue);
            }
        } catch (InvalidJwtException e) {
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        if (user == null) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
            return builder.build();
        }

        try {
            String userCode = (String) user.getAttribute("jansBackchannelUsrCode", true, false);
            DefaultErrorResponse cibaAuthorizeParamsValidation = cibaAuthorizeParamsValidatorService.validateParams(
                    scopes, clientNotificationToken, client.getBackchannelTokenDeliveryMode(),
                    loginHintToken, idTokenHint, loginHint, bindingMessage, client.getBackchannelUserCodeParameter(),
                    userCodeParam, userCode, requestedExpiry);
            if (cibaAuthorizeParamsValidation != null) {
                builder = Response.status(cibaAuthorizeParamsValidation.getStatus());
                builder.entity(errorResponseFactory.errorAsJson(
                        cibaAuthorizeParamsValidation.getType(), cibaAuthorizeParamsValidation.getReason()));
                return builder.build();
            }

            String deviceRegistrationToken = (String) user.getAttribute("jansBackchannelDeviceRegistrationTkn", true, false);
            if (deviceRegistrationToken == null) {
                builder = Response.status(Response.Status.UNAUTHORIZED.getStatusCode()); // 401
                builder.entity(errorResponseFactory.getErrorAsJson(UNAUTHORIZED_END_USER_DEVICE));
                return builder.build();
            }

            int expiresIn = requestedExpiry != null ? requestedExpiry : appConfiguration.getBackchannelAuthenticationResponseExpiresIn();
            Integer interval = client.getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH ?
                    null : appConfiguration.getBackchannelAuthenticationResponseInterval();
            long currentTime = new Date().getTime();

            CibaRequestCacheControl cibaRequestCacheControl = new CibaRequestCacheControl(user, client, expiresIn, scopes,
                    clientNotificationToken, bindingMessage, currentTime, acrValues);

            cibaRequestService.save(cibaRequestCacheControl, expiresIn);

            String authReqId = cibaRequestCacheControl.getAuthReqId();

            // Notify End-User to obtain Consent/Authorization
            cibaEndUserNotificationService.notifyEndUser(
                    cibaRequestCacheControl.getScopesAsString(),
                    cibaRequestCacheControl.getAcrValues(),
                    authReqId,
                    deviceRegistrationToken);

            builder.entity(getJSONObject(
                    authReqId,
                    expiresIn,
                    interval).toString(4).replace("\\/", "/"));

            builder.type(MediaType.APPLICATION_JSON_TYPE);
            builder.cacheControl(ServerUtil.cacheControl(true, false));
        } catch (JSONException e) {
            builder = Response.status(400);
            builder.entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST));
            log.error(e.getMessage(), e);
        } catch (InvalidClaimException e) {
            builder = Response.status(400);
            builder.entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST));
            log.error(e.getMessage(), e);
        }

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    private JSONObject getJSONObject(String authReqId, int expiresIn, Integer interval) throws JSONException {
        JSONObject responseJsonObject = new JSONObject();

        responseJsonObject.put(AUTH_REQ_ID, authReqId);
        responseJsonObject.put(EXPIRES_IN, expiresIn);

        if (interval != null) {
            responseJsonObject.put(INTERVAL, interval);
        }

        return responseJsonObject;
    }

}