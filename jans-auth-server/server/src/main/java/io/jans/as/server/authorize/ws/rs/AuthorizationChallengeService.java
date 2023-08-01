package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.AuthorizationChallengeResponse;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.crypto.binding.TokenBindingParseException;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AuthorizationCodeGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAuthorizationChallengeService;
import io.jans.as.server.util.ServerUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

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

    public Response requestAuthorization(AuthzRequest authzRequest) {
        log.debug("Attempting to request authz challenge: {}", authzRequest);

        authzRequestService.createOauth2AuditLog(authzRequest);

        try {
            return authorize(authzRequest);
        } catch (WebApplicationException e) {
            if (log.isErrorEnabled() && AuthzRequestService.canLogWebApplicationException(e))
                log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
    }

    public Response authorize(AuthzRequest authzRequest) throws IOException, TokenBindingParseException {
        final String state = authzRequest.getState();
        authzRequest.setScope(ServerUtil.urlDecode(authzRequest.getScope()));
        String tokenBindingHeader = authzRequest.getHttpRequest().getHeader("Sec-Token-Binding");

        final Client client = authorizeRestWebServiceValidator.validateClient(authzRequest, false);
        authorizationChallengeValidator.validateGrantType(client, state);
        Set<String> scopes = scopeChecker.checkScopesPolicy(client, authzRequest.getScope());

        final ExecutionContext executionContext = ExecutionContext.of(authzRequest);
        final boolean ok = externalAuthorizationChallengeService.externalAuthorize(executionContext);
        if (!ok) {
            log.debug("Not allowed by authorization challenge script, client_id {}.", client.getClientId());
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, state, "No allowed by authorization challenge script."))
                    .build());
        }

        User user = executionContext.getUser() != null ? executionContext.getUser() : new User();
        String grantAcr = executionContext.getScript() != null ? executionContext.getScript().getName() : authzRequest.getAcrValues();

        AuthorizationCodeGrant authorizationGrant = authorizationGrantList.createAuthorizationCodeGrant(user, client, new Date());
        authorizationGrant.setNonce(authzRequest.getNonce());
        authorizationGrant.setJwtAuthorizationRequest(authzRequest.getJwtRequest());
        authorizationGrant.setTokenBindingHash(TokenBindingMessage.getTokenBindingIdHashFromTokenBindingMessage(tokenBindingHeader, client.getIdTokenTokenBindingCnf()));
        authorizationGrant.setScopes(scopes);
        authorizationGrant.setCodeChallenge(authzRequest.getCodeChallenge());
        authorizationGrant.setCodeChallengeMethod(authzRequest.getCodeChallengeMethod());
        authorizationGrant.setClaims(authzRequest.getClaims());
        authorizationGrant.setSessionDn("no_session_for_authorization_challenge"); // no need for session as at Authorization Endpoint
        authorizationGrant.setAcrValues(grantAcr);
        authorizationGrant.save();

        String authorizationCode = authorizationGrant.getAuthorizationCode().getCode();

        return createSuccessfulResponse(authorizationCode);
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
