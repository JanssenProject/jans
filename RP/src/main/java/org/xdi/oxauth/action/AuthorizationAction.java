/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.action;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesManager;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.model.authorize.Claim;
import org.xdi.oxauth.client.model.authorize.ClaimValue;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.common.Display;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.oxauth.model.util.Util;

/**
 * @author Javier Rojas Blum Date: 02.20.2012
 */
@Name("authorizationAction")
@Scope(ScopeType.SESSION)
@AutoCreate
public class AuthorizationAction {

    @Logger
    private Log log;

    private String authorizationEndpoint;
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
    private SignatureAlgorithm requestObjectSigningAlg = SignatureAlgorithm.NONE;
    private KeyEncryptionAlgorithm requestObjectEncryptionAlg = KeyEncryptionAlgorithm.RSA1_5;
    private BlockEncryptionAlgorithm requestObjectEncryptionEnc = BlockEncryptionAlgorithm.A128CBC_PLUS_HS256;
    private String keyId;
    private String clientSecret;
    private String modulus;
    private String privateExponent;
    private String publicExponent;
    private String d;
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
                JwtAuthorizationRequest jwtAuthorizationRequest;
                if (isJWSSelected()) {
                    if (isClientSecretRequired()) {
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(requestObjectSigningAlg, clientSecret);
                    } else if (isPrivateExponentRequired()) {
                        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(requestObjectSigningAlg, privateKey);
                        jwtAuthorizationRequest.setKeyId(keyId);
                    } else if (isDRequired()) {
                        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(requestObjectSigningAlg, privateKey);
                        jwtAuthorizationRequest.setKeyId(keyId);
                    } else {
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(requestObjectSigningAlg, (String) null);
                    }
                } else {
                    if (isPublicExponentRequired()) {
                        RSAPublicKey publicKey = new RSAPublicKey(modulus, publicExponent);
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(req, requestObjectEncryptionAlg,
                                requestObjectEncryptionEnc, publicKey);
                    } else {
                        jwtAuthorizationRequest = new JwtAuthorizationRequest(req, requestObjectEncryptionAlg,
                                requestObjectEncryptionEnc, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
                    }
                }

                if (jwtAuthorizationRequest != null) {
                    req.setRequest(jwtAuthorizationRequest.getEncodedJwt(openIdRequestObject));
                }
            }

            String authorizationRequest = authorizationEndpoint + "?" + req.getQueryString();
            FacesManager.instance().redirectToExternalURL(authorizationRequest);
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

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getPrivateExponent() {
        return privateExponent;
    }

    public void setPrivateExponent(String privateExponent) {
        this.privateExponent = privateExponent;
    }

    public String getPublicExponent() {
        return publicExponent;
    }

    public void setPublicExponent(String publicExponent) {
        this.publicExponent = publicExponent;
    }

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public boolean isJWSSelected() {
        return "JWS".equals(signOrEncryptRequestObject);
    }

    public boolean isJWESelected() {
        return "JWE".equals(signOrEncryptRequestObject);
    }

    public boolean isKeyIdRequired() {
        return isPrivateExponentRequired() || isDRequired();
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

    public boolean isModulusRequired() {
        if (isJWSSelected()) {
            return requestObjectSigningAlg == SignatureAlgorithm.RS256
                    || requestObjectSigningAlg == SignatureAlgorithm.RS384
                    || requestObjectSigningAlg == SignatureAlgorithm.RS512;
        } else {
            return requestObjectEncryptionAlg == KeyEncryptionAlgorithm.RSA_OAEP
                    || requestObjectEncryptionAlg == KeyEncryptionAlgorithm.RSA1_5;
        }
    }

    public boolean isPublicExponentRequired() {
        if (isJWESelected()) {
            return requestObjectEncryptionAlg == KeyEncryptionAlgorithm.RSA_OAEP
                    || requestObjectEncryptionAlg == KeyEncryptionAlgorithm.RSA1_5;
        } else {
            return false;
        }
    }

    public boolean isPrivateExponentRequired() {
        if (isJWSSelected()) {
            return requestObjectSigningAlg == SignatureAlgorithm.RS256
                    || requestObjectSigningAlg == SignatureAlgorithm.RS384
                    || requestObjectSigningAlg == SignatureAlgorithm.RS512;
        } else {
            return false;
        }
    }

    public boolean isDRequired() {
        if (isJWSSelected()) {
            return requestObjectSigningAlg == SignatureAlgorithm.ES256
                    || requestObjectSigningAlg == SignatureAlgorithm.ES384
                    || requestObjectSigningAlg == SignatureAlgorithm.ES512;
        } else {
            return false;
        }
    }

    public String getOpenIdRequestObject() {
        openIdRequestObject = "";

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

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(req, SignatureAlgorithm.NONE, (String) null);
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