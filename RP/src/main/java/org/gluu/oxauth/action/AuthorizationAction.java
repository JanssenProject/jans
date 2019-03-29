/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.action;

import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.client.AuthorizationRequest;
import org.gluu.oxauth.client.AuthorizeClient;
import org.gluu.oxauth.client.model.authorize.Claim;
import org.gluu.oxauth.client.model.authorize.ClaimValue;
import org.gluu.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.AuthorizationMethod;
import org.gluu.oxauth.model.common.Display;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.model.util.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version March 15, 2019
 */
@Named
@SessionScoped
public class AuthorizationAction implements Serializable {

    private static final long serialVersionUID = -4131456982254169325L;

    @Inject
    private Logger log;

    private String authorizationEndpoint;
    private String jwksUri;
    private List<ResponseType> responseTypes;
    private String clientId;
    private List<String> scopes;
    private String redirectUri;
    private String state;
    private String nonce;
    private Display display;
    private List<Prompt> prompt;
    private Integer maxAge;
    private String uiLocales;
    private String claimsLocales;
    private String idTokenHint;
    private String loginHint;
    private String acrValues;
    private String claims;
    private String registration;
    private String requestUri;

    private boolean useOpenIdRequestObject;
    private String signOrEncryptRequestObject = "JWS";
    private String keyStoreFile;
    private String keyStoreSecret;
    private SignatureAlgorithm requestObjectSigningAlg = SignatureAlgorithm.NONE;
    private KeyEncryptionAlgorithm requestObjectEncryptionAlg = KeyEncryptionAlgorithm.RSA1_5;
    private BlockEncryptionAlgorithm requestObjectEncryptionEnc = BlockEncryptionAlgorithm.A128CBC_PLUS_HS256;
    private String keyId;
    private String clientSecret;
    private String openIdRequestObject;

    private boolean showResults;
    private String requestString;
    private String responseString;

    public void exec() {
        try {
            AuthorizationRequest req = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            req.setState(state);
            req.setRequestUri(requestUri);
            req.setMaxAge(maxAge);
            req.setUiLocales(StringUtils.spaceSeparatedToList(uiLocales));
            req.setClaimsLocales(StringUtils.spaceSeparatedToList(claimsLocales));
            req.setIdTokenHint(idTokenHint);
            req.setLoginHint(loginHint);
            req.setAcrValues(StringUtils.spaceSeparatedToList(acrValues));
            if (org.apache.commons.lang.StringUtils.isNotBlank(claims)) {
                req.setClaims(new JSONObject(claims));
            }
            req.setRegistration(registration);
            req.setDisplay(display);
            req.getPrompts().addAll(prompt);

            if (useOpenIdRequestObject) {
                JwtAuthorizationRequest jwtAuthorizationRequest = null;
                if (isJWSSelected()) {
                    if (isKeyIdRequired()) {
                        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(
                                req, requestObjectSigningAlg, cryptoProvider);
                        jwtAuthorizationRequest.setKeyId(keyId);
                    } else {
                        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(
                                req, requestObjectSigningAlg, clientSecret, cryptoProvider);
                    }

                    req.setRequest(jwtAuthorizationRequest.getEncodedJwt());
                } else {
                    if (isKeyIdRequired()) {
                        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
                        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(
                                req, requestObjectEncryptionAlg, requestObjectEncryptionEnc, cryptoProvider);
                        jwtAuthorizationRequest.setKeyId(keyId);

                        req.setRequest(jwtAuthorizationRequest.getEncodedJwt(jwks));
                    } else {
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(
                                req, requestObjectEncryptionAlg, requestObjectEncryptionEnc, clientSecret);

                        req.setRequest(jwtAuthorizationRequest.getEncodedJwt());
                    }
                }
            }

            req.setAuthorizationMethod(AuthorizationMethod.URL_QUERY_PARAMETER);
            AuthorizeClient client = new AuthorizeClient(authorizationEndpoint);
            client.setRequest(req);
            String authorizationRequest = authorizationEndpoint + "?" + req.getQueryString();

            showResults = true;
            requestString = client.getRequestAsString();

            FacesContext.getCurrentInstance().getExternalContext().redirect(authorizationRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public List<ResponseType> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<ResponseType> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Display getDisplay() {
        return display;
    }

    public void setDisplay(Display display) {
        this.display = display;
    }

    public List<Prompt> getPrompt() {
        return prompt;
    }

    public void setPrompt(List<Prompt> prompt) {
        this.prompt = prompt;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public String getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(String uiLocales) {
        this.uiLocales = uiLocales;
    }

    public String getClaimsLocales() {
        return claimsLocales;
    }

    public void setClaimsLocales(String claimsLocales) {
        this.claimsLocales = claimsLocales;
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

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public boolean isUseOpenIdRequestObject() {
        return useOpenIdRequestObject;
    }

    public void setUseOpenIdRequestObject(boolean useOpenIdRequestObject) {
        this.useOpenIdRequestObject = useOpenIdRequestObject;
    }

    public String getSignOrEncryptRequestObject() {
        return signOrEncryptRequestObject;
    }

    public void setSignOrEncryptRequestObject(String signOrEncryptRequestObject) {
        this.signOrEncryptRequestObject = signOrEncryptRequestObject;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getKeyStoreSecret() {
        return keyStoreSecret;
    }

    public void setKeyStoreSecret(String keyStoreSecret) {
        this.keyStoreSecret = keyStoreSecret;
    }

    public SignatureAlgorithm getRequestObjectSigningAlg() {
        return requestObjectSigningAlg;
    }

    public void setRequestObjectSigningAlg(SignatureAlgorithm requestObjectSigningAlg) {
        this.requestObjectSigningAlg = requestObjectSigningAlg;
    }

    public KeyEncryptionAlgorithm getRequestObjectEncryptionAlg() {
        return requestObjectEncryptionAlg;
    }

    public void setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm requestObjectEncryptionAlg) {
        this.requestObjectEncryptionAlg = requestObjectEncryptionAlg;
    }

    public BlockEncryptionAlgorithm getRequestObjectEncryptionEnc() {
        return requestObjectEncryptionEnc;
    }

    public void setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm requestObjectEncryptionEnc) {
        this.requestObjectEncryptionEnc = requestObjectEncryptionEnc;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isJWSSelected() {
        return "JWS".equals(signOrEncryptRequestObject);
    }

    public boolean isJWESelected() {
        return "JWE".equals(signOrEncryptRequestObject);
    }

    public boolean isKeyIdRequired() {
        if (isJWSSelected()) {
            return requestObjectSigningAlg == SignatureAlgorithm.RS256
                    || requestObjectSigningAlg == SignatureAlgorithm.RS384
                    || requestObjectSigningAlg == SignatureAlgorithm.RS512
                    || requestObjectSigningAlg == SignatureAlgorithm.ES256
                    || requestObjectSigningAlg == SignatureAlgorithm.ES384
                    || requestObjectSigningAlg == SignatureAlgorithm.ES512;
        } else {
            return requestObjectEncryptionAlg == KeyEncryptionAlgorithm.RSA1_5
                    || requestObjectEncryptionAlg == KeyEncryptionAlgorithm.RSA_OAEP;
        }
    }

    public boolean isKeyStoreRequired() {
        if (isJWSSelected()) {
            return requestObjectSigningAlg == SignatureAlgorithm.RS256
                    || requestObjectSigningAlg == SignatureAlgorithm.RS384
                    || requestObjectSigningAlg == SignatureAlgorithm.RS512
                    || requestObjectSigningAlg == SignatureAlgorithm.ES256
                    || requestObjectSigningAlg == SignatureAlgorithm.ES384
                    || requestObjectSigningAlg == SignatureAlgorithm.ES512;
        } else {
            return false;
        }
    }

    public boolean isClientSecretRequired() {
        if (isJWSSelected()) {
            return requestObjectSigningAlg == SignatureAlgorithm.HS256
                    || requestObjectSigningAlg == SignatureAlgorithm.HS384
                    || requestObjectSigningAlg == SignatureAlgorithm.HS512;
        } else {
            return requestObjectEncryptionAlg == KeyEncryptionAlgorithm.A128KW
                    || requestObjectEncryptionAlg == KeyEncryptionAlgorithm.A256KW;
        }
    }

    public String getOpenIdRequestObject() {
        openIdRequestObject = "";

        try {
            if (useOpenIdRequestObject) {
                AuthorizationRequest req = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
                req.setState(state);
                req.setRequestUri(requestUri);
                req.setMaxAge(maxAge);
                req.setUiLocales(StringUtils.spaceSeparatedToList(uiLocales));
                req.setClaimsLocales(StringUtils.spaceSeparatedToList(claimsLocales));
                req.setIdTokenHint(idTokenHint);
                req.setLoginHint(loginHint);
                req.setAcrValues(StringUtils.spaceSeparatedToList(acrValues));
                req.setRegistration(registration);
                req.setDisplay(display);
                req.getPrompts().addAll(prompt);

                OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();
                JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                        req, SignatureAlgorithm.NONE, (String) null, cryptoProvider);
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
                openIdRequestObject = jwtAuthorizationRequest.getDecodedJwt();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return openIdRequestObject;
    }

    public void setOpenIdRequestObject(String openIdRequestObject) {
        this.openIdRequestObject = openIdRequestObject;
    }

    public boolean isShowResults() {
        return showResults;
    }

    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }

    public String getRequestString() {
        return requestString;
    }

    public void setRequestString(String requestString) {
        this.requestString = requestString;
    }

    public String getResponseString() {
        return responseString;
    }

    public void setResponseString(String responseString) {
        this.responseString = responseString;
    }
}