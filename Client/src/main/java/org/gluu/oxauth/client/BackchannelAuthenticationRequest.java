/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.ciba.BackchannelAuthenticationRequestParam;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.util.Util;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Represents a CIBA backchannel authorization request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class BackchannelAuthenticationRequest extends BaseRequest {

    private List<String> scope;
    private String clientNotificationToken;
    private List<String> acrValues;
    private String loginHintToken;
    private String idTokenHint;
    private String loginHint;
    private String bindingMessage;
    private String userCode;
    private Integer requestedExpiry;

    public BackchannelAuthenticationRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public String getLoginHintToken() {
        return loginHintToken;
    }

    public void setLoginHintToken(String loginHintToken) {
        this.loginHintToken = loginHintToken;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Integer getRequestedExpiry() {
        return requestedExpiry;
    }

    public void setRequestedExpiry(Integer requestedExpiry) {
        this.requestedExpiry = requestedExpiry;
    }

    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            final String scopesAsString = Util.listAsString(scope);
            final String acrValuesAsString = Util.listAsString(acrValues);

            if (StringUtils.isNotBlank(scopesAsString)) {
                queryStringBuilder.append(BackchannelAuthenticationRequestParam.SCOPE)
                        .append("=").append(URLEncoder.encode(scopesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(clientNotificationToken)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.CLIENT_NOTIFICATION_TOKEN)
                        .append("=").append(URLEncoder.encode(clientNotificationToken, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(acrValuesAsString)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.ACR_VALUES)
                        .append("=").append(URLEncoder.encode(acrValuesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(loginHintToken)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.LOGIN_HINT_TOKEN)
                        .append("=").append(URLEncoder.encode(loginHintToken, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(idTokenHint)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.ID_TOKEN_HINT)
                        .append("=").append(URLEncoder.encode(idTokenHint, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(loginHint)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.LOGIN_HINT)
                        .append("=").append(URLEncoder.encode(loginHint, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(bindingMessage)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.BINDING_MESSAGE)
                        .append("=").append(URLEncoder.encode(bindingMessage, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(userCode)) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.USER_CODE)
                        .append("=").append(URLEncoder.encode(userCode, Util.UTF8_STRING_ENCODING));
            }
            if (requestedExpiry != null) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.REQUESTED_EXPIRY)
                        .append("=").append(URLEncoder.encode(requestedExpiry.toString(), Util.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}