/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.authorize.ws.rs;

import org.gluu.jsf2.message.FacesMessages;
import org.gluu.oxauth.i18n.LanguageBean;
import org.gluu.oxauth.model.common.DeviceAuthorizationCacheControl;
import org.gluu.oxauth.model.common.DeviceAuthorizationStatus;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.DeviceAuthorizationService;
import org.gluu.oxauth.util.RedirectUri;
import org.slf4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.UUID;

import static org.gluu.oxauth.model.authorize.AuthorizeRequestParam.*;

@RequestScoped
@Named
public class DeviceAuthorizationAction {

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

    private String userCode1;
    private String userCode2;

    public void processUserCodeVerification() {
        String userCode = userCode1 + '-' + userCode2;
        DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthorizationCacheData(null, userCode);
        String message = null;
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
}
