package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.*;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.jans.as.model.config.Constants.OPENID;
import static io.jans.as.model.config.Constants.TOKEN_TYPE_ACCESS_TOKEN;
import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TokenExchangeService {

    public static final String DEVICE_SECRET = "device_secret";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private TokenRestWebServiceValidator tokenRestWebServiceValidator;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private TokenCreatorService tokenCreatorService;

    @Inject
    private AttributeService attributeService;

    public void rotateDeviceSecretOnRefreshToken(HttpServletRequest httpRequest, AuthorizationGrant refreshGrant, String scope) {
        if (StringUtils.isBlank(scope) || !scope.contains(ScopeConstants.DEVICE_SSO)) {
            log.debug("Skip rotate device secret on refresh token. No device_sso scope.");
            return;
        }
        if (StringUtils.isBlank(refreshGrant.getSessionDn())) {
            return;
        }
        final SessionId sessionId = sessionIdService.getSessionByDn(refreshGrant.getSessionDn());
        if (sessionId == null) {
            return;
        }

        final String deviceSecret = httpRequest.getParameter(DEVICE_SECRET);

        // spec: rotate only if device_secret is not specified
        if (StringUtils.isBlank(deviceSecret)) {
            rotateDeviceSecret(sessionId, deviceSecret, true);
        }
    }

    public String rotateDeviceSecret(SessionId sessionId, String deviceSecret) {
        return rotateDeviceSecret(sessionId, deviceSecret, false);
    }

    public String rotateDeviceSecret(SessionId sessionId, String deviceSecret, boolean forceRotation) {
        if (BooleanUtils.isFalse(appConfiguration.getRotateDeviceSecret()) && !forceRotation) {
            return null;
        }

        String newDeviceSecret = HandleTokenFactory.generateDeviceSecret();

        final List<String> deviceSecrets = sessionId.getDeviceSecrets();
        deviceSecrets.remove(deviceSecret);
        deviceSecrets.add(newDeviceSecret);

        sessionIdService.updateSessionId(sessionId, false);

        return newDeviceSecret;
    }

    public JSONObject processTokenExchange(String scope, Function<JsonWebResponse, Void> idTokenPreProcessing, ExecutionContext executionContext) {
        final HttpServletRequest httpRequest = executionContext.getHttpRequest();
        final Client client = executionContext.getClient();
        final OAuth2AuditLog auditLog = executionContext.getAuditLog();

        final String audience = httpRequest.getParameter("audience");
        final String subjectToken = httpRequest.getParameter("subject_token");
        final String subjectTokenType = httpRequest.getParameter("subject_token_type");
        final String deviceSecret = httpRequest.getParameter("actor_token");
        final String actorTokenType = httpRequest.getParameter("actor_token_type");

        tokenRestWebServiceValidator.validateAudience(audience, auditLog);
        tokenRestWebServiceValidator.validateSubjectTokenType(subjectTokenType, auditLog);
        tokenRestWebServiceValidator.validateActorTokenType(actorTokenType, auditLog);
        tokenRestWebServiceValidator.validateActorToken(deviceSecret, auditLog);

        final SessionId sessionId = sessionIdService.getSessionByDeviceSecret(deviceSecret);
        tokenRestWebServiceValidator.validateSessionForTokenExchange(sessionId, deviceSecret, auditLog);
        checkNotNull(sessionId); // it checked by validator, added it only to relax static bugs checker

        tokenRestWebServiceValidator.validateSubjectToken(deviceSecret, subjectToken, sessionId, auditLog);

        TokenExchangeGrant tokenExchangeGrant = authorizationGrantList.createTokenExchangeGrant(sessionIdService.getUser(sessionId), client);
        tokenExchangeGrant.setSessionDn(sessionId.getDn());

        executionContext.setGrant(tokenExchangeGrant);

        scope = tokenExchangeGrant.checkScopesPolicy(scope);

        AccessToken accessToken = tokenExchangeGrant.createAccessToken(executionContext); // create token after scopes are checked

        IdToken idToken = null;
        if (isTrue(appConfiguration.getOpenidScopeBackwardCompatibility()) && tokenExchangeGrant.getScopes().contains(OPENID)) {
            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                    appConfiguration.getLegacyIdTokenClaims());

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(httpRequest, tokenExchangeGrant, client, appConfiguration, attributeService);

            executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
            executionContext.setPreProcessing(idTokenPreProcessing);
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            idToken = tokenExchangeGrant.createIdToken(
                    null, null, null, null, null, executionContext);
        }

        RefreshToken reToken = tokenCreatorService.createRefreshToken(executionContext, scope);

        executionContext.getAuditLog().updateOAuth2AuditLog(tokenExchangeGrant, true);

        String rotatedDeviceSecret = rotateDeviceSecret(sessionId, deviceSecret);

        JSONObject jsonObj = new JSONObject();
        try {
            TokenRestWebServiceImpl.fillJsonObject(jsonObj, accessToken, accessToken.getTokenType(), accessToken.getExpiresIn(), reToken, scope, idToken);
            jsonObj.put("issued_token_type", TOKEN_TYPE_ACCESS_TOKEN);
            if (StringUtils.isNotBlank(rotatedDeviceSecret)) {
                jsonObj.put(DEVICE_SECRET, rotatedDeviceSecret);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj;
    }

    public String createNewDeviceSecret(String sessionDn, Client client, String scope) {
        if (StringUtils.isBlank(scope) || !scope.contains(ScopeConstants.DEVICE_SSO)) {
            log.debug("Skip device secret. No device_sso scope.");
            return null;
        }
        if (client == null || !ArrayUtils.contains(client.getGrantTypes(), GrantType.TOKEN_EXCHANGE)) {
            log.debug("Skip device secret. Scope has {} value but client does not have Token Exchange Grant Type enabled ('urn:ietf:params:oauth:grant-type:token-exchange')", ScopeConstants.DEVICE_SSO);
            return null;
        }

        try {
            final SessionId sessionId = sessionIdService.getSessionByDn(sessionDn);
            if (sessionId == null) {
                log.debug("Unable to find session by dn: {}", sessionDn);
                return null;
            }

            String newDeviceSecret = HandleTokenFactory.generateDeviceSecret();
            sessionId.getDeviceSecrets().add(newDeviceSecret);

            sessionIdService.updateSessionId(sessionId, false);

            return newDeviceSecret;
        } catch (Exception e) {
            log.error("Failed to generate device_secret", e);
        }
        return null;
    }
}
