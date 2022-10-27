package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
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

    public String rotateDeviceSecret(SessionId sessionId, String actorToken) {
        if (BooleanUtils.isFalse(appConfiguration.getRotateDeviceSecret())) {
            return null;
        }

        String newDeviceSecret = HandleTokenFactory.generateDeviceSecret();

        final List<String> deviceSecrets = sessionId.getDeviceSecrets();
        deviceSecrets.remove(actorToken);
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
        final String actorToken = httpRequest.getParameter("actor_token");
        final String actorTokenType = httpRequest.getParameter("actor_token_type");

        tokenRestWebServiceValidator.validateAudience(audience, auditLog);
        tokenRestWebServiceValidator.validateSubjectTokenType(subjectTokenType, auditLog);
        tokenRestWebServiceValidator.validateActorTokenType(actorTokenType, auditLog);
        tokenRestWebServiceValidator.validateActorToken(actorToken, auditLog);

        final SessionId sessionId = sessionIdService.getSessionByDeviceSecret(actorToken);
        tokenRestWebServiceValidator.validateSessionForTokenExchange(sessionId, actorToken, auditLog);
        checkNotNull(sessionId); // it checked by validator, added it only to relax static bugs checker

        tokenRestWebServiceValidator.validateSubjectToken(subjectToken, sessionId, auditLog);

        TokenExchangeGrant tokenExchangeGrant = authorizationGrantList.createTokenExchangeGrant(new User(), client);
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

        String rotatedDeviceSecret = rotateDeviceSecret(sessionId, actorToken);

        JSONObject jsonObj = new JSONObject();
        try {
            TokenRestWebServiceImpl.fillJsonObject(jsonObj, accessToken, accessToken.getTokenType(), accessToken.getExpiresIn(), reToken, scope, idToken);
            jsonObj.put("issued_token_type", TOKEN_TYPE_ACCESS_TOKEN);
            if (StringUtils.isNotBlank(rotatedDeviceSecret)) {
                jsonObj.put("device_secret", rotatedDeviceSecret);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj;
    }
}
