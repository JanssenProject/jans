/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.message.FacesMessages;
import org.gluu.oxauth.i18n.LanguageBean;
import org.gluu.oxauth.model.common.DeviceAuthorizationCacheControl;
import org.gluu.oxauth.model.common.DeviceAuthorizationStatus;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.DeviceAuthorizationService;
import org.gluu.oxauth.util.RedirectUri;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import static org.gluu.oxauth.model.authorize.AuthorizeRequestParam.*;

@Named
@SessionScoped
public class DeviceAuthorizationAction implements Serializable {

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

    // Query params
    private String code;
    private String sessionId;
    private String state;
    private String sessionState;
    private String error;
    private String errorDescription;
    private String userCode;

    // UI data
    private String userCode1;
    private String userCode2;

    // Internal process
    private Long lastAttempt;
    private byte attempts;

    @PostConstruct
    public void init() {
        lastAttempt = System.currentTimeMillis();
        attempts = 0;
        code = sessionId = state = sessionState = error = errorDescription = userCode = userCode1 = userCode2 = null;
    }

    public void pageLoaded() {
        log.info("Processing device authorization page request, userCode: {}, code: {}, sessionId: {}, state: {}, sessionState: {}, error: {}, errorDescription: {}", userCode, code, sessionId, state, sessionState, error, errorDescription);
    }

    public void processUserCodeVerification() {
        String message = null;
        if (!preventBruteForcing()) {
            message = languageBean.getMessage("device.authorization.expired.code.msg");
            facesMessages.add(FacesMessage.SEVERITY_WARN, message);
            return;
        }

        String userCode;
        if (isCompleteVerificationMode()) {
            userCode = this.userCode;
        } else {
            userCode = userCode1 + '-' + userCode2;
        }

        DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthorizationCacheData(null, userCode);
        if (cacheData != null) {
            if (cacheData.getStatus() == DeviceAuthorizationStatus.PENDING) {
                redirectToAuthorization(cacheData);
            } else if (cacheData.getStatus() == DeviceAuthorizationStatus.DENIED) {
                message = languageBean.getMessage("device.authorization.access.denied.msg");
            } else {
                message = languageBean.getMessage("device.authorization.expired.code.msg");
            }
        } else {
            message = languageBean.getMessage("device.authorization.invalid.user.code");
        }

        if (message != null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN, message);
        }
    }

    private boolean preventBruteForcing() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttempt > 500 && attempts < 5) {
            lastAttempt = currentTime;
            attempts++;
            return true;
        } else {
            return false;
        }
    }

    private void redirectToAuthorization(DeviceAuthorizationCacheControl cacheData) {
        try {
            String authorizationEndpoint = appConfiguration.getAuthorizationEndpoint();
            String clientId = cacheData.getClient().getClientId();
            String responseType = "code";
            String scope = Util.listAsString(cacheData.getScopes());
            String state = UUID.randomUUID().toString();
            String nonce = UUID.randomUUID().toString();

            RedirectUri authRequest = new RedirectUri(authorizationEndpoint);
            authRequest.addResponseParameter(CLIENT_ID, clientId);
            authRequest.addResponseParameter(RESPONSE_TYPE, responseType);
            authRequest.addResponseParameter(SCOPE, scope);
            authRequest.addResponseParameter(STATE, state);
            authRequest.addResponseParameter(NONCE, nonce);
            authRequest.addResponseParameter(USER_CODE, cacheData.getUserCode());

            FacesContext.getCurrentInstance().getExternalContext().redirect(authRequest.toString());
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

    public boolean isNewRequest() {
        return StringUtils.isBlank(code) && StringUtils.isBlank(sessionId) && StringUtils.isBlank(state)
                && StringUtils.isBlank(error) && StringUtils.isBlank(errorDescription);
    }

    public boolean isErrorResponse() {
        return StringUtils.isNotBlank(error) && StringUtils.isNotBlank(error);
    }

    public boolean isCompleteVerificationMode() {
        return isNewRequest() && StringUtils.isNotBlank(userCode);
    }

    public boolean isDeviceAuthnCompleted() {
        return StringUtils.isNotBlank(code) && StringUtils.isNotBlank(state) && StringUtils.isBlank(error);
    }

    public String getUserCode1() {
        return userCode1;
    }

    public void setUserCode1(String userCode1) {
        this.userCode1 = userCode1;
    }

    public String getUserCode2() {
        return userCode2;
    }

    public void setUserCode2(String userCode2) {
        this.userCode2 = userCode2;
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
}

