package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Maps;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.AuthorizationChallengeSession;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.authorize.AuthorizationChallengeResponse;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.crypto.binding.TokenBindingParseException;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AuthorizationCodeGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalAuthorizationChallengeService;
import io.jans.as.server.util.ServerUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static io.jans.as.server.authorize.ws.rs.AuthorizeRestWebServiceImpl.getGenericRequestMap;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

/**
 * @author Yuriy Z
 */
@RequestScoped
@Named
public class AuthorizationChallengeService {

    @Inject
    private Logger log;

    @Inject
    private AuthzRequestService authzRequestService;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private AuthorizationChallengeValidator authorizationChallengeValidator;

    @Inject
    private ExternalAuthorizationChallengeService externalAuthorizationChallengeService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationChallengeSessionService authorizationChallengeSessionService;

    @Inject
    private Identity identity;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private CookieService cookieService;

    public Response requestAuthorization(AuthzRequest authzRequest) {
        log.debug("Attempting to request authz challenge: {}", authzRequest);

        authzRequestService.createOauth2AuditLog(authzRequest);

        try {
            return authorize(authzRequest);
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled())
                log.trace(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
    }

    public void prepareAuthzRequest(AuthzRequest authzRequest) {
        authzRequest.setScope(ServerUtil.urlDecode(authzRequest.getScope()));

        if (StringUtils.isNotBlank(authzRequest.getAuthorizationChallengeSession())) {
            final AuthorizationChallengeSession session = authorizationChallengeSessionService.getAuthorizationChallengeSession(authzRequest.getAuthorizationChallengeSession());

            authorizationChallengeValidator.validateDpopJkt(session, authzRequest.getDpop());

            authzRequest.setAuthorizationChallengeSessionObject(session);
            if (session != null) {
                final Map<String, String> attributes = session.getAttributes().getAttributes();

                final String clientId = attributes.get("client_id");
                if (StringUtils.isNotBlank(clientId) && StringUtils.isBlank(authzRequest.getClientId())) {
                    authzRequest.setClientId(clientId);
                }

                String acrValues = session.getAttributes().getAcrValues();
                if (StringUtils.isBlank(acrValues)) {
                    acrValues = attributes.get("acr_values");
                }
                if (StringUtils.isNotBlank(acrValues) && StringUtils.isBlank(authzRequest.getAcrValues())) {
                    authzRequest.setAcrValues(acrValues);
                }
            }
        }
    }

    public Response authorize(AuthzRequest authzRequest) throws IOException, TokenBindingParseException {
        final String state = authzRequest.getState();
        final String tokenBindingHeader = authzRequest.getHttpRequest().getHeader("Sec-Token-Binding");

        prepareAuthzRequest(authzRequest);

        SessionId sessionUser = identity.getSessionId();
        User user = sessionIdService.getUser(sessionUser);

        final Client client = authorizeRestWebServiceValidator.validateClient(authzRequest, false);
        authorizationChallengeValidator.validateGrantType(client, state);
        authorizationChallengeValidator.validateAccess(client);
        Set<String> scopes = scopeChecker.checkScopesPolicy(client, authzRequest.getScope());
        authorizeRestWebServiceValidator.validateAuthorizationDetails(authzRequest, client);

        final ExecutionContext executionContext = ExecutionContext.of(authzRequest);
        executionContext.setSessionId(sessionUser);

        if (user == null) {
            log.trace("Executing external authentication challenge");

            final boolean ok = externalAuthorizationChallengeService.externalAuthorize(executionContext);
            if (!ok) {
                log.debug("Not allowed by authorization challenge script, client_id {}.", client.getClientId());
                throw new WebApplicationException(errorResponseFactory
                        .newErrorResponse(Response.Status.BAD_REQUEST)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, state, "No allowed by authorization challenge script."))
                        .build());
            }

            user = executionContext.getUser() != null ? executionContext.getUser() : new User();

            // generate session if not exist and if allowed by config (or if session is prepared by script)
            if (sessionUser == null || executionContext.getAuthorizationChallengeSessionId() != null) {
                sessionUser = generateAuthenticateSessionWithCookieIfNeeded(authzRequest, user, executionContext.getAuthorizationChallengeSessionId());
            }
        }

        String grantAcr = executionContext.getScript() != null ? executionContext.getScript().getName() : authzRequest.getAcrValues();

        AuthorizationCodeGrant authorizationGrant = authorizationGrantList.createAuthorizationCodeGrant(user, client, new Date());
        authorizationGrant.setNonce(authzRequest.getNonce());
        authorizationGrant.setJwtAuthorizationRequest(authzRequest.getJwtRequest());
        authorizationGrant.setTokenBindingHash(TokenBindingMessage.getTokenBindingIdHashFromTokenBindingMessage(tokenBindingHeader, client.getIdTokenTokenBindingCnf()));
        authorizationGrant.setScopes(scopes);
        authorizationGrant.setAuthzDetails(authzRequest.getAuthzDetails());
        authorizationGrant.setCodeChallenge(authzRequest.getCodeChallenge());
        authorizationGrant.setCodeChallengeMethod(authzRequest.getCodeChallengeMethod());
        authorizationGrant.setClaims(authzRequest.getClaims());
        authorizationGrant.setSessionDn(sessionUser != null ? sessionUser.getDn() : "no_session_for_authorization_challenge"); // no need for session as at Authorization Endpoint
        authorizationGrant.setAcrValues(grantAcr);
        authorizationGrant.setAuthorizationChallenge(true);
        authorizationGrant.save();

        String authorizationCode = authorizationGrant.getAuthorizationCode().getCode();

        return createSuccessfulResponse(authorizationCode);
    }

    private SessionId generateAuthenticateSessionWithCookieIfNeeded(AuthzRequest authzRequest, User user, SessionId scriptGeneratedSession) {
        if (user == null) {
            log.trace("Skip session_id generation because user is null");
            return null;
        }

        if (isFalse(appConfiguration.getAuthorizationChallengeShouldGenerateSession())) {
            log.trace("Skip session_id generation because it's not allowed by AS configuration ('authorizationChallengeShouldGenerateSession=false')");
            return null;
        }

        if (scriptGeneratedSession != null) {
            log.trace("Authorization Challenge script generated session: {}.", scriptGeneratedSession.getId());
            cookieService.createSessionIdCookie(scriptGeneratedSession, authzRequest.getHttpRequest(), authzRequest.getHttpResponse(), false);
            log.trace("Created cookie for authorization Challenge script generated session: {}.", scriptGeneratedSession.getId());
            return scriptGeneratedSession;
        }

        Map<String, String> genericRequestMap = getGenericRequestMap(authzRequest.getHttpRequest());

        Map<String, String> parameterMap = Maps.newHashMap(genericRequestMap);
        Map<String, String> requestParameterMap = requestParameterService.getAllowedParameters(parameterMap);

        SessionId sessionUser = sessionIdService.generateAuthenticatedSessionId(authzRequest.getHttpRequest(), user.getDn(), authzRequest.getPrompt());
        final Set<String> sessionAttributesKeySet = sessionUser.getSessionAttributes().keySet();
        requestParameterMap.forEach((key, value) -> {
            if (!sessionAttributesKeySet.contains(key)) {
                sessionUser.getSessionAttributes().put(key, value);
            }
        });

        cookieService.createSessionIdCookie(sessionUser, authzRequest.getHttpRequest(), authzRequest.getHttpResponse(), false);
        sessionIdService.updateSessionId(sessionUser);
        log.trace("Session updated with {}", sessionUser);

        return sessionUser;
    }

    public Response createSuccessfulResponse(String authorizationCode) throws IOException {
        AuthorizationChallengeResponse response = new AuthorizationChallengeResponse();
        response.setAuthorizationCode(authorizationCode);

        return Response.status(Response.Status.OK)
                .entity(ServerUtil.asJson(response))
                .cacheControl(ServerUtil.cacheControl(true))
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
