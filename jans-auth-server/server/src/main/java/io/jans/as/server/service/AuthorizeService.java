/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.collect.Sets;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.ciba.PushErrorResponseType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.KeyOpsType;
import io.jans.as.model.jwk.Use;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.auth.Authenticator;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushErrorService;
import io.jans.as.server.model.common.CibaRequestCacheControl;
import io.jans.as.server.model.common.CibaRequestStatus;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.DeviceAuthorizationStatus;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.jsf2.message.FacesMessages;
import io.jans.jsf2.service.FacesService;
import io.jans.util.security.StringEncrypter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
@RequestScoped
public class AuthorizeService {

    @Inject
    private Logger log;

    @Inject
    private ClientService clientService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CookieService cookieService;

    @Inject
    private ClientAuthorizationsService clientAuthorizationsService;

    @Inject
    private Identity identity;

    @Inject
    private Authenticator authenticator;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ScopeService scopeService;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private CIBAPingCallbackService cibaPingCallbackService;

    @Inject
    private CIBAPushErrorService cibaPushErrorService;

    @Inject
    private CibaRequestService cibaRequestService;

    @Inject
    private DeviceAuthorizationService deviceAuthorizationService;
    
    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;
    
    public SessionId getSession() {
        return getSession(null);
    }

    public SessionId getSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            sessionId = cookieService.getSessionIdFromCookie();
            if (StringUtils.isBlank(sessionId)) {
                return null;
            }
        }

        if (!identity.isLoggedIn()) {
            authenticator.authenticateBySessionId(sessionId);
        }

        SessionId ldapSessionId = sessionIdService.getSessionId(sessionId);
        if (ldapSessionId == null) {
            identity.logout();
        }

        return ldapSessionId;
    }

    public void permissionGranted(HttpServletRequest httpRequest, final SessionId session) {
        log.trace("permissionGranted");
        try {
            final User user = sessionIdService.getUser(session);
            if (user == null) {
                log.debug("Permission denied. Failed to find session user: userDn = {}", session.getUserDn());
                permissionDenied(session);
                return;
            }

            String clientId = session.getSessionAttributes().get(AuthorizeRequestParam.CLIENT_ID);
            final Client client = clientService.getClient(clientId);
            if (client == null) {
                log.debug("Permission denied. Failed to find client by id: {}", clientId);
                permissionDenied(session);
                return;
            }

            String scope = session.getSessionAttributes().get(AuthorizeRequestParam.SCOPE);
            String responseType = session.getSessionAttributes().get(AuthorizeRequestParam.RESPONSE_TYPE);

            boolean persistDuringImplicitFlow = !io.jans.as.model.common.ResponseType.isImplicitFlow(responseType);
            if (!client.getTrustedClient() && persistDuringImplicitFlow && client.getPersistClientAuthorizations()) {
                final Set<String> scopes = Sets.newHashSet(io.jans.as.model.util.StringUtils.spaceSeparatedToList(scope));
                clientAuthorizationsService.add(user.getAttribute("inum"), client.getClientId(), scopes);
            }
            session.addPermission(clientId, true);
            sessionIdService.updateSessionId(session);
            identity.setSessionId(session);

            // OXAUTH-297 - set session_id cookie
            if (!appConfiguration.getInvalidateSessionCookiesAfterAuthorizationFlow()) {
                cookieService.createSessionIdCookie(session, false);
            }
            Map<String, String> sessionAttribute = requestParameterService.getAllowedParameters(session.getSessionAttributes());

            if (sessionAttribute.containsKey(AuthorizeRequestParam.PROMPT)) {
                List<Prompt> prompts = Prompt.fromString(sessionAttribute.get(AuthorizeRequestParam.PROMPT), " ");
                prompts.remove(Prompt.CONSENT);
                sessionAttribute.put(AuthorizeRequestParam.PROMPT, io.jans.as.model.util.StringUtils.implodeEnum(prompts, " "));
            }

            final String parametersAsString = requestParameterService.parametersAsString(sessionAttribute);
            String uri = httpRequest.getContextPath() + "/restv1/authorize?" + parametersAsString;
            log.trace("permissionGranted, redirectTo: {}", uri);

            if (invalidateSessionCookiesIfNeeded()) {
                if (!uri.contains(AuthorizeRequestParam.SESSION_ID) && appConfiguration.getSessionIdRequestParameterEnabled()) {
                    uri += "&session_id=" + session.getId();
                }
            }
            facesService.redirectToExternalURL(uri);
        } catch (UnsupportedEncodingException e) {
            log.trace(e.getMessage(), e);
        }
    }

    public void permissionDenied(final SessionId session) {
        log.trace("permissionDenied");
        invalidateSessionCookiesIfNeeded();

        if (session == null) {
            authenticationFailedSessionInvalid();
            return;
        }
        
        String baseRedirectUri = session.getSessionAttributes().get(AuthorizeRequestParam.REDIRECT_URI);
        String state = session.getSessionAttributes().get(AuthorizeRequestParam.STATE);
        ResponseMode responseMode = ResponseMode.fromString(session.getSessionAttributes().get(AuthorizeRequestParam.RESPONSE_MODE));
        List<ResponseType> responseType = ResponseType.fromString(session.getSessionAttributes().get(AuthorizeRequestParam.RESPONSE_TYPE), " ");

        RedirectUri redirectUri = new RedirectUri(baseRedirectUri, responseType, responseMode);
        redirectUri.parseQueryString(errorResponseFactory.getErrorAsQueryString(AuthorizeErrorResponseType.ACCESS_DENIED, state));

        // CIBA
        Map<String, String> sessionAttribute = requestParameterService.getAllowedParameters(session.getSessionAttributes());
        if (sessionAttribute.containsKey(AuthorizeRequestParam.AUTH_REQ_ID)) {
            String authReqId = sessionAttribute.get(AuthorizeRequestParam.AUTH_REQ_ID);
            CibaRequestCacheControl request = cibaRequestService.getCibaRequest(authReqId);

            if (request != null && request.getClient() != null) {
                if (request.getStatus() == CibaRequestStatus.PENDING) {
                    cibaRequestService.removeCibaRequest(authReqId);
                }
                switch (request.getClient().getBackchannelTokenDeliveryMode()) {
                    case POLL:
                        request.setStatus(CibaRequestStatus.DENIED);
                        request.setTokensDelivered(false);
                        cibaRequestService.update(request);
                        break;
                    case PING:
                        request.setStatus(CibaRequestStatus.DENIED);
                        request.setTokensDelivered(false);
                        cibaRequestService.update(request);

                        cibaPingCallbackService.pingCallback(
                                request.getAuthReqId(),
                                request.getClient().getBackchannelClientNotificationEndpoint(),
                                request.getClientNotificationToken()
                        );
                        break;
                    case PUSH:
                        cibaPushErrorService.pushError(
                                request.getAuthReqId(),
                                request.getClient().getBackchannelClientNotificationEndpoint(),
                                request.getClientNotificationToken(),
                                PushErrorResponseType.ACCESS_DENIED,
                                "The end-user denied the authorization request.");
                        break;
                }
            }
        }
        if (sessionAttribute.containsKey(DeviceAuthorizationService.SESSION_USER_CODE)) {
            processDeviceAuthDeniedResponse(sessionAttribute);
        }

		if (responseMode == ResponseMode.JWT) {
			String clientId = session.getSessionAttributes().get(AuthorizeRequestParam.CLIENT_ID);
			Client client = clientService.getClient(clientId);
			facesService.redirectToExternalURL(createJarmRedirectUri(redirectUri, client, session));
        }   
        else 
        	facesService.redirectToExternalURL(redirectUri.toString());
    }
    
    private String createJarmRedirectUri(RedirectUri redirectUri, Client client, final SessionId session)
    {
		String jarmRedirectUri = redirectUri.toString();
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
				.fromString(client.getAttributes().getAuthorizationSignedResponseAlg());
		redirectUri.setSignatureAlgorithm(signatureAlgorithm);
		redirectUri.addResponseParameter("error", "access_denied");
		redirectUri.addResponseParameter("error_description", "User Denied the Access");
		redirectUri.setIssuer(appConfiguration.getIssuer());
		redirectUri.setAudience(client.getClientId());
		redirectUri.setCryptoProvider(cryptoProvider);
		String keyId = null;
		try {
			String clientSecret = clientService.decryptSecret(client.getClientSecret());
			redirectUri.setSharedSecret(clientSecret);
			keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
					Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE, KeyOpsType.CONNECT);
		} catch (CryptoProviderException e) {
			log.error(e.getMessage(), e);
		} catch (StringEncrypter.EncryptionException e) {
			log.error(e.getMessage(), e);
		}
		redirectUri.setKeyId(keyId);

		String jarmQueryString = redirectUri.getQueryString();
		log.info("The JARM Query Response:" + jarmQueryString);
		jarmRedirectUri = jarmRedirectUri + jarmQueryString;

		return jarmRedirectUri;
    }
    
    private void authenticationFailedSessionInvalid() {
        facesMessages.add(FacesMessage.SEVERITY_ERROR, "login.errorSessionInvalidMessage");
        facesService.redirect("/error.xhtml");
    }

    public List<Scope> getScopes() {
        SessionId session = getSession();
        String scope = session.getSessionAttributes().get("scope");

        return getScopes(scope);
    }

    public List<Scope> getScopes(String scopes) {
        List<Scope> result = new ArrayList<Scope>();

        if (scopes != null && !scopes.isEmpty()) {
            String[] scopesName = scopes.split(" ");
            for (String scopeName : scopesName) {
                Scope s = scopeService.getScopeById(scopeName);
                if (s != null && s.getDescription() != null) {
                    result.add(s);
                }
            }
        }

        return result;
    }

    private boolean invalidateSessionCookiesIfNeeded() {
        if (appConfiguration.getInvalidateSessionCookiesAfterAuthorizationFlow()) {
            return invalidateSessionCookies();
        }
        return false;
    }

    private boolean invalidateSessionCookies() {
        try {
            if (externalContext.getResponse() instanceof HttpServletResponse) {
                final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();

                log.trace("Invalidated {} cookie.", CookieService.SESSION_ID_COOKIE_NAME);
                httpResponse.addHeader("Set-Cookie", CookieService.SESSION_ID_COOKIE_NAME + "=deleted; Path=/; Secure; HttpOnly; Expires=Thu, 01 Jan 1970 00:00:01 GMT;");

                log.trace("Invalidated {} cookie.", CookieService.CONSENT_SESSION_ID_COOKIE_NAME);
                httpResponse.addHeader("Set-Cookie", CookieService.CONSENT_SESSION_ID_COOKIE_NAME + "=deleted; Path=/; Secure; HttpOnly; Expires=Thu, 01 Jan 1970 00:00:01 GMT;");
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private void processDeviceAuthDeniedResponse(Map<String, String> sessionAttribute) {
        String userCode = sessionAttribute.get(DeviceAuthorizationService.SESSION_USER_CODE);
        DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthzByUserCode(userCode);

        if (cacheData != null && cacheData.getStatus() == DeviceAuthorizationStatus.PENDING) {
            cacheData.setStatus(DeviceAuthorizationStatus.DENIED);
            deviceAuthorizationService.saveInCache(cacheData, true, false);
            deviceAuthorizationService.removeDeviceAuthRequestInCache(userCode, null);
        }
    }
}
