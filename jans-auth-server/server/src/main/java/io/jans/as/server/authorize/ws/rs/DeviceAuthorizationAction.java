/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.DeviceAuthorizationStatus;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.DeviceAuthorizationService;
import io.jans.as.server.service.SessionIdService;
import io.jans.jsf2.message.FacesMessages;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.jans.as.model.authorize.AuthorizeRequestParam.*;
import static io.jans.as.model.util.StringUtils.EASY_TO_READ_CHARACTERS;
import static io.jans.as.server.service.DeviceAuthorizationService.*;

/**
 * Action used to process all requests related to device authorization.
 */
@Named
@RequestScoped
@SuppressWarnings("java:S1948")
public class DeviceAuthorizationAction implements Serializable {

    private static final String INVALID_USER_CODE_MESSAGE_KEY = "device.authorization.invalid.user.code";

    @Inject
    private Logger log;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private LanguageBean languageBean;

    @Inject
    private DeviceAuthorizationService deviceAuthorizationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CookieService cookieService;

    // Query params
    private String code;
    private String sessionId;
    private String state;
    private String sessionState;
    private String error;
    private String errorDescription;
    private String userCode;

    // UI data
    private String userCodePart1;
    private String userCodePart2;
    private String titleMsg;
    private String descriptionMsg;

    // Internal process
    private Long lastAttempt = System.currentTimeMillis();
    private byte attempts;

    /**
     * Method used by the view to load all query params and set the page state.
     */
    public void pageLoaded() {
        log.info("Processing device authorization page request, userCode: {}, code: {}, sessionId: {}, state: {}, sessionState: {}, error: {}, errorDescription: {}", userCode, code, sessionId, state, sessionState, error, errorDescription);
        if (StringUtils.isNotBlank(error)) {
            this.titleMsg = error;
            this.descriptionMsg = errorDescription;
        }
        if (this.isDeviceAuthnCompleted()) {
            this.titleMsg = languageBean.getMessage("device.authorization.access.granted.title");
            this.descriptionMsg = languageBean.getMessage("device.authorization.authorization.completed.msg");
        }

        initializeSession();
    }

    /**
     * Reset data in session or create a new one whether there is no session.
     */
    public void initializeSession() {
        initializeOrCreateSession();
    }

    @SuppressWarnings("java:S1117")
    public SessionId initializeOrCreateSession() {
        SessionId sessionId = sessionIdService.getSessionId();
        Map<String, String> sessionAttributes = sessionId != null ? sessionId.getSessionAttributes() : new HashMap<>();
        sessionAttributes.put(Constants.DEVICE_AUTHORIZATION, Boolean.TRUE.toString());
        sessionAttributes.remove(SESSION_USER_CODE);

        if (StringUtils.isNotBlank(userCode)) {
            sessionAttributes.put(SESSION_USER_CODE, userCode);
        }
        if (sessionId == null) {
            SessionId deviceAuthzSession = sessionIdService.generateUnauthenticatedSessionId(null, new Date(), SessionIdState.UNAUTHENTICATED, sessionAttributes, false);
            sessionIdService.persistSessionId(deviceAuthzSession);
            cookieService.createSessionIdCookie(deviceAuthzSession, false);
            log.debug("Created session for device authorization grant page, sessionId: {}", deviceAuthzSession.getId());
        } else {
            if (StringUtils.isNotBlank(sessionId.getSessionAttributes().get(SESSION_LAST_ATTEMPT))
                    && StringUtils.isNotBlank(sessionId.getSessionAttributes().get(SESSION_ATTEMPTS))) {
                lastAttempt = Long.parseLong(sessionId.getSessionAttributes().get(SESSION_LAST_ATTEMPT));
                attempts = Byte.parseByte(sessionId.getSessionAttributes().get(SESSION_ATTEMPTS));
            }
            sessionAttributes.put(SESSION_LAST_ATTEMPT, String.valueOf(lastAttempt));
            sessionAttributes.put(SESSION_ATTEMPTS, String.valueOf(attempts));
            sessionId.setSessionAttributes(sessionAttributes);
            sessionIdService.updateSessionId(sessionId);
            log.debug("Updated session for device authorization grant page, sessionId: {}", sessionId.getId());
        }
        return sessionId;
    }

    /**
     * Processes user code introduced or loaded in the veritification page and redirects whether user code is correct
     * or return an error if there is something wrong.
     */
    @SuppressWarnings("java:S1117")
    public void processUserCodeVerification() {
        try {
            SessionId session = sessionIdService.getSessionId();
            if (session == null) {
                log.trace("No session found during device authorization, creating new unauthenticated session ...");
                session = initializeOrCreateSession();
            }

            if (!preventBruteForcing(session)) {
                log.trace("Brute force prevention during device authorization, sessionId: {}", session.getId());
                facesMessages.add(FacesMessage.SEVERITY_WARN, languageBean.getMessage("device.authorization.brute.forcing.msg"));
                return;
            }

            final String userCode;
            if (StringUtils.isBlank(userCodePart1) && StringUtils.isBlank(userCodePart2)) {
                userCode = session.getSessionAttributes().get(SESSION_USER_CODE).toUpperCase();
            } else {
                userCode = (userCodePart1 + '-' + userCodePart2).toUpperCase();
            }

            if (!validateFormat(userCode)) {
                log.trace("Invalid user code during device authorization, userCode: {}, sessionId: {}", userCode, session.getId());
                facesMessages.add(FacesMessage.SEVERITY_WARN, languageBean.getMessage(INVALID_USER_CODE_MESSAGE_KEY));
                return;
            }

            DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthzByUserCode(userCode);
            log.debug("Verifying device authorization cache data: {}", cacheData);

            String message = null;
            if (cacheData != null) {
                if (cacheData.getStatus() == DeviceAuthorizationStatus.PENDING) {
                    session.getSessionAttributes().put(SESSION_USER_CODE, userCode);
                    session.getSessionAttributes().remove(SESSION_LAST_ATTEMPT);
                    session.getSessionAttributes().remove(SESSION_ATTEMPTS);
                    sessionIdService.updateSessionId(session);

                    redirectToAuthorization(cacheData);
                } else if (cacheData.getStatus() == DeviceAuthorizationStatus.DENIED) {
                    log.trace("Cache data status=DENIED during device authorization, userCode: {}, sessionId: {}", userCode, session.getId());
                    message = languageBean.getMessage("device.authorization.access.denied.msg");
                } else {
                    log.trace("Cache data EXPIRED during device authorization, userCode: {}, sessionId: {}", userCode, session.getId());
                    message = languageBean.getMessage("device.authorization.expired.code.msg");
                }
            } else {
                log.trace("Unable to validate user code during device authorization, userCode: {}, sessionId: {}", userCode, session.getId());
                message = languageBean.getMessage(INVALID_USER_CODE_MESSAGE_KEY);
            }

            if (message != null) {
                log.trace("Message during device authorization, message: {}, userCode: {}, sessionId: {}", message, userCode, session.getId());
                facesMessages.add(FacesMessage.SEVERITY_WARN, message);
            }
        } catch (Exception e) {
            log.error("Failed to process user code validation", e);

            facesMessages.clear();
            facesMessages.add(FacesMessage.SEVERITY_WARN, languageBean.getMessage(INVALID_USER_CODE_MESSAGE_KEY));
        }
    }

    /**
     * Prevents brute forcing for user code field from device_authorization page.
     *
     * @param session Session used to keep data related to all attemps done.
     */
    private boolean preventBruteForcing(SessionId session) {
        lastAttempt = Long.valueOf(session.getSessionAttributes().getOrDefault(SESSION_LAST_ATTEMPT, "0"));
        attempts = Byte.parseByte(session.getSessionAttributes().getOrDefault(SESSION_ATTEMPTS, "0"));
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttempt > 500 && attempts < 5) {
            lastAttempt = currentTime;
            attempts++;
            session.getSessionAttributes().put(SESSION_LAST_ATTEMPT, String.valueOf(lastAttempt));
            session.getSessionAttributes().put(SESSION_ATTEMPTS, String.valueOf(attempts));
            sessionIdService.updateSessionId(session);
            return true;
        } else {
            log.trace("User has done too many failed user code verification requests, sessionId: {}", sessionIdService.getSessionId());
            return false;
        }
    }

    /**
     * Ensures that the user code gotten from UI is well formatted.
     *
     * @param userCode User code to be processed.
     */
    private boolean validateFormat(String userCode) {
        String regex = "[" + EASY_TO_READ_CHARACTERS + "]{4}-[" + EASY_TO_READ_CHARACTERS + "]{4}";
        return userCode.matches(regex);
    }

    /**
     * Process data related to device authorization and redirects to the authorization page.
     *
     * @param cacheData Data related to the device code request.
     */
    @SuppressWarnings("java:S1117")
    private void redirectToAuthorization(DeviceAuthorizationCacheControl cacheData) {
        try {
            log.info("Redirecting to authorization code flow to process device authorization, data: {}", cacheData);
            String authorizationEndpoint = appConfiguration.getAuthorizationEndpoint();
            String clientId = cacheData.getClient().getClientId();
            String responseType = appConfiguration.getDeviceAuthzResponseTypeToProcessAuthz();
            String scope = Util.listAsString(cacheData.getScopes());
            String state = UUID.randomUUID().toString();
            String nonce = UUID.randomUUID().toString();
            String acr = appConfiguration.getDeviceAuthzAcr();

            RedirectUri authRequest = new RedirectUri(authorizationEndpoint);
            authRequest.addResponseParameter(CLIENT_ID, clientId);
            authRequest.addResponseParameter(RESPONSE_TYPE, responseType);
            authRequest.addResponseParameter(SCOPE, scope);
            authRequest.addResponseParameter(STATE, state);
            authRequest.addResponseParameter(NONCE, nonce);
            if (StringUtils.isNotBlank(acr)) {
                authRequest.addResponseParameter(ACR_VALUES, acr);
            }

            final String redirectTo = authRequest.toString();
            log.debug("Redirecting to: {}", redirectTo);
            FacesContext.getCurrentInstance().getExternalContext().redirect(redirectTo);
        } catch (IOException e) {
            log.error("Problems trying to redirect to authorization page from device authorization action", e);
            String message = languageBean.getMessage("error.errorEncountered");
            facesMessages.add(FacesMessage.SEVERITY_WARN, message);
        } catch (Exception e) {
            log.error("Exception processing redirection", e);
            String message = languageBean.getMessage("error.errorEncountered");
            facesMessages.add(FacesMessage.SEVERITY_WARN, message);
        }
    }

    /**
     * Checks if page is loaded for a new device request.
     */
    public boolean isNewRequest() {
        return StringUtils.isBlank(code) && StringUtils.isBlank(sessionId) && StringUtils.isBlank(state)
                && StringUtils.isBlank(error) && StringUtils.isBlank(errorDescription);
    }

    /**
     * Checks if page should show error messages.
     */
    public boolean isErrorResponse() {
        return StringUtils.isNotBlank(error);
    }

    /**
     * Checks if page should be shown in complete verification mode, it means that the
     * user code has been shared by the url.
     */
    public boolean isCompleteVerificationMode() {
        return isNewRequest() && StringUtils.isNotBlank(userCode);
    }

    /**
     * Checks if the authorization is complete and page should show confirmation to the end-user.
     */
    public boolean isDeviceAuthnCompleted() {
        return StringUtils.isNotBlank(code) && StringUtils.isNotBlank(state) && StringUtils.isBlank(error);
    }

    public String getUserCodePart1() {
        return userCodePart1;
    }

    public void setUserCodePart1(String userCodePart1) {
        this.userCodePart1 = userCodePart1;
    }

    public String getUserCodePart2() {
        return userCodePart2;
    }

    public void setUserCodePart2(String userCodePart2) {
        this.userCodePart2 = userCodePart2;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getTitleMsg() {
        return titleMsg;
    }

    public void setTitleMsg(String titleMsg) {
        this.titleMsg = titleMsg;
    }

    public String getDescriptionMsg() {
        return descriptionMsg;
    }

    public void setDescriptionMsg(String descriptionMsg) {
        this.descriptionMsg = descriptionMsg;
    }
}

