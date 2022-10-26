/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.session.EndSessionRequestParam;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.jsf2.service.FacesService;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.service.JsonService;
import io.jans.util.StringHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@RequestScoped
@Named
public class LogoutAction {

    private static final String EXTERNAL_LOGOUT = "external_logout";
    private static final String EXTERNAL_LOGOUT_DATA = "external_logout_data";

    @Inject
    private Logger log;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private JsonService jsonService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesContext facesContext;

    @Inject
    private LanguageBean languageBean;

    private String idTokenHint;
    private String postLogoutRedirectUri;
    private SessionId sessionId;


    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public void redirect() {
        SessionId sessionId = sessionIdService.getSessionId();

        boolean validationResult = validateParameters();
        if (!validationResult) {
            try {
                restoreLogoutParametersFromSession(sessionId);
            } catch (IOException ex) {
                logoutFailed();
                log.debug("Failed to restore logout parameters from session", ex);
            }

            validationResult = validateParameters();
            if (!validationResult) {
                missingLogoutParameters();
                return;
            }
        }

        ExternalLogoutResult externalLogoutResult = processExternalAuthenticatorLogOut(sessionId);
        if (ExternalLogoutResult.FAILURE == externalLogoutResult) {
            logoutFailed();
            return;
        } else if (ExternalLogoutResult.REDIRECT == externalLogoutResult) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        // Required parameters
        if (idTokenHint != null && !idTokenHint.isEmpty()) {
            sb.append(EndSessionRequestParam.ID_TOKEN_HINT + "=").append(idTokenHint);
        }

        if (sessionId != null && !postLogoutRedirectUri.isEmpty()) {
            sb.append("&" + EndSessionRequestParam.SID + "=").append(sessionId.getOutsideSid());
        }

        if (postLogoutRedirectUri != null && !postLogoutRedirectUri.isEmpty()) {
            sb.append("&" + EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI + "=").append(postLogoutRedirectUri);
        }

        facesService.redirectToExternalURL("restv1/end_session?" + sb.toString());
    }

    private boolean validateParameters() {
        return (StringHelper.isNotEmpty(idTokenHint) || (sessionId != null)) && StringHelper.isNotEmpty(postLogoutRedirectUri);
    }

    private ExternalLogoutResult processExternalAuthenticatorLogOut(SessionId sessionId) {
        if ((sessionId != null) && sessionId.getSessionAttributes().containsKey(EXTERNAL_LOGOUT)) {
            log.debug("Detected callback from external system. Resuming logout.");
            return ExternalLogoutResult.SUCCESS;
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            Boolean endSessionWithAccessToken = appConfiguration.getEndSessionWithAccessToken();
            if ((endSessionWithAccessToken != null) && endSessionWithAccessToken) {
                authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(idTokenHint);
            }
        }
        if ((authorizationGrant == null) && (sessionId == null)) {
            return ExternalLogoutResult.FAILURE;
        }

        String acrValues;
        if (authorizationGrant == null) {
            acrValues = sessionIdService.getAcr(sessionId);
        } else {
            acrValues = authorizationGrant.getAcrValues();
        }

        boolean isExternalAuthenticatorLogoutPresent = StringHelper.isNotEmpty(acrValues);
        if (isExternalAuthenticatorLogoutPresent) {
            log.debug("Attemptinmg to execute logout method of '{}' external authenticator.", acrValues);

            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService.getCustomScriptConfigurationByName(acrValues);
            if (customScriptConfiguration == null) {
                log.error("Failed to get ExternalAuthenticatorConfiguration. acr_values: {}", acrValues);
                return ExternalLogoutResult.FAILURE;
            } else {
                boolean scriptExternalLogoutResult = externalAuthenticationService.executeExternalLogout(customScriptConfiguration, null);
                ExternalLogoutResult externalLogoutResult = scriptExternalLogoutResult ? ExternalLogoutResult.SUCCESS : ExternalLogoutResult.FAILURE;


                final String userDn = sessionId != null ? sessionId.getUserDn() : "";
                final String sId = sessionId != null ? sessionId.getId() : "";
                log.debug("Logout result is '{}' for session '{}', userDn: '{}'", externalLogoutResult, sId, userDn);

                int apiVersion = externalAuthenticationService.executeExternalGetApiVersion(customScriptConfiguration);
                if (apiVersion < 3) {
                    // Not support redirect to external system at logout
                    return externalLogoutResult;
                }

                log.trace("According to API version script supports logout redirects");
                String logoutExternalUrl = externalAuthenticationService.getLogoutExternalUrl(customScriptConfiguration, null);
                log.debug("External logout result is '{}' for user '{}'", logoutExternalUrl, userDn);

                if (StringHelper.isEmpty(logoutExternalUrl)) {
                    return externalLogoutResult;
                }

                // Store in session parameters needed to call end_session
                try {
                    storeLogoutParametersInSession(sessionId);
                } catch (IOException ex) {
                    log.debug("Failed to persist logout parameters in session", ex);

                    return ExternalLogoutResult.FAILURE;
                }

                // Redirect to external URL
                facesService.redirectToExternalURL(logoutExternalUrl);
                return ExternalLogoutResult.REDIRECT;
            }
        } else {
            return ExternalLogoutResult.SUCCESS;
        }
    }

    private void storeLogoutParametersInSession(@Nullable SessionId sessionId) throws IOException {
        if (sessionId == null) {
            return;
        }
        Map<String, String> sessionAttributes = sessionId.getSessionAttributes();

        LogoutParameters logoutParameters = new LogoutParameters(idTokenHint, postLogoutRedirectUri);

        String logoutParametersJson = jsonService.objectToJson(logoutParameters);
        String logoutParametersBase64 = Base64Util.base64urlencode(logoutParametersJson.getBytes(StandardCharsets.UTF_8));

        sessionAttributes.put(EXTERNAL_LOGOUT, Boolean.toString(true));
        sessionAttributes.put(EXTERNAL_LOGOUT_DATA, logoutParametersBase64);

        sessionIdService.updateSessionId(sessionId);
    }

    private boolean restoreLogoutParametersFromSession(SessionId sessionId) throws IllegalArgumentException, IOException {
        if (sessionId == null) {
            return false;
        }

        this.sessionId = sessionId;
        Map<String, String> sessionAttributes = sessionId.getSessionAttributes();

        boolean restoreParameters = sessionAttributes.containsKey(EXTERNAL_LOGOUT);
        if (!restoreParameters) {
            return false;
        }

        String logoutParametersBase64 = sessionAttributes.get(EXTERNAL_LOGOUT_DATA);
        String logoutParametersJson = new String(Base64Util.base64urldecode(logoutParametersBase64), StandardCharsets.UTF_8);

        LogoutParameters logoutParameters = jsonService.jsonToObject(logoutParametersJson, LogoutParameters.class);

        this.idTokenHint = logoutParameters.getIdTokenHint();
        this.postLogoutRedirectUri = logoutParameters.getPostLogoutRedirectUri();

        return true;
    }

    public void missingLogoutParameters() {
        String message = languageBean.getMessage("logout.missingParameters");
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message));
        facesService.redirect("/error.xhtml");
    }

    public void logoutFailed() {
        String message = languageBean.getMessage("logout.failedToProceed");
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message));
        facesService.redirect("/error.xhtml");
    }

    public static class LogoutParameters {
        private String idTokenHint;
        private String postLogoutRedirectUri;

        public LogoutParameters() {
        }

        public LogoutParameters(String idTokenHint, String postLogoutRedirectUri) {
            this.idTokenHint = idTokenHint;
            this.postLogoutRedirectUri = postLogoutRedirectUri;
        }

        public String getIdTokenHint() {
            return idTokenHint;
        }

        public void setIdTokenHint(String idTokenHint) {
            this.idTokenHint = idTokenHint;
        }

        public String getPostLogoutRedirectUri() {
            return postLogoutRedirectUri;
        }

        public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
            this.postLogoutRedirectUri = postLogoutRedirectUri;
        }
    }

    private enum ExternalLogoutResult {
        SUCCESS,
        FAILURE,
        REDIRECT
    }

}