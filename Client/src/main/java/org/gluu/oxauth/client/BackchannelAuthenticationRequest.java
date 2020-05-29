/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.model.ciba.BackchannelAuthenticationRequestParam;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.token.ClientAssertionType;
import org.gluu.oxauth.model.util.Util;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Represents a CIBA backchannel authorization request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version May 28, 2020
 */
public class BackchannelAuthenticationRequest extends BaseRequest {

    private static final Logger LOG = Logger.getLogger(TokenRequest.class);

    private List<String> scope;
    private String clientNotificationToken;
    private List<String> acrValues;
    private String loginHintToken;
    private String idTokenHint;
    private String loginHint;
    private String bindingMessage;
    private String userCode;
    private Integer requestedExpiry;

    private SignatureAlgorithm algorithm;
    private String sharedKey;
    private AbstractCryptoProvider cryptoProvider;
    private String keyId;
    private String audience;

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

    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SignatureAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public AbstractCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getClientAssertion() {
        if (cryptoProvider == null) {
            LOG.error("Crypto provider is not specified");
            return null;
        }

        Jwt clientAssertion = new Jwt();

        if (algorithm == null) {
            algorithm = SignatureAlgorithm.HS256;
        }
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date expirationTime = calendar.getTime();

        // Header
        clientAssertion.getHeader().setType(JwtType.JWT);
        clientAssertion.getHeader().setAlgorithm(algorithm);
        if (StringUtils.isNotBlank(keyId)) {
            clientAssertion.getHeader().setKeyId(keyId);
        }

        // Claims
        clientAssertion.getClaims().setIssuer(getAuthUsername());
        clientAssertion.getClaims().setSubjectIdentifier(getAuthUsername());
        clientAssertion.getClaims().setAudience(audience);
        clientAssertion.getClaims().setJwtId(UUID.randomUUID());
        clientAssertion.getClaims().setExpirationTime(expirationTime);
        clientAssertion.getClaims().setIssuedAt(issuedAt);

        // Signature
        try {
            if (sharedKey == null) {
                sharedKey = getAuthPassword();
            }
            String signature = cryptoProvider.sign(clientAssertion.getSigningInput(), keyId, sharedKey, algorithm);
            clientAssertion.setEncodedSignature(signature);
        } catch (InvalidJwtException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return clientAssertion.toString();
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
            if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
                if (getAuthUsername() != null && !getAuthUsername().isEmpty()) {
                    queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.CLIENT_ID)
                            .append("=").append(URLEncoder.encode(getAuthUsername(), Util.UTF8_STRING_ENCODING));
                }
                if (getAuthPassword() != null && !getAuthPassword().isEmpty()) {
                    queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.CLIENT_SECRET)
                            .append("=").append(URLEncoder.encode(getAuthPassword(), Util.UTF8_STRING_ENCODING));
                }
            } else if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                    getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.CLIENT_ASSERTION_TYPE)
                        .append("=").append(URLEncoder.encode(ClientAssertionType.JWT_BEARER.toString(), Util.UTF8_STRING_ENCODING));
                queryStringBuilder.append("&").append(BackchannelAuthenticationRequestParam.CLIENT_ASSERTION)
                        .append("=").append(getClientAssertion());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}