/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.model.authorize;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.client.AuthorizationRequest;
import org.gluu.oxauth.model.common.Display;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwe.JweEncrypterImpl;
import org.gluu.oxauth.model.jwt.JwtClaims;
import org.gluu.oxauth.model.jwt.JwtHeader;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version November 20, 2018
 */
public class JwtAuthorizationRequest {

    private static final Logger LOG = Logger.getLogger(JwtAuthorizationRequest.class);

    // Header
    private JwtType type;
    private SignatureAlgorithm signatureAlgorithm;
    private KeyEncryptionAlgorithm keyEncryptionAlgorithm;
    private BlockEncryptionAlgorithm blockEncryptionAlgorithm;
    private String keyId;

    // Payload
    private List<ResponseType> responseTypes;
    private String clientId;
    private List<String> scopes;
    private String redirectUri;
    private String state;
    private String nonce;
    private Display display;
    private List<Prompt> prompts;
    private Integer maxAge;
    private List<String> uiLocales;
    private List<String> claimsLocales;
    private String idTokenHint;
    private String loginHint;
    private List<String> acrValues;
    private String registration;
    private boolean requestUniqueId;

    private UserInfoMember userInfoMember;
    private IdTokenMember idTokenMember;

    // Signature/Encryption Keys
    private String sharedKey;
    private AbstractCryptoProvider cryptoProvider;

    public JwtAuthorizationRequest(AuthorizationRequest authorizationRequest, SignatureAlgorithm signatureAlgorithm,
                                   AbstractCryptoProvider cryptoProvider) {
        this(authorizationRequest, signatureAlgorithm, cryptoProvider, null, null, null);
    }

    public JwtAuthorizationRequest(AuthorizationRequest authorizationRequest, SignatureAlgorithm signatureAlgorithm,
                                   String sharedKey, AbstractCryptoProvider cryptoProvider) {
        this(authorizationRequest, signatureAlgorithm, cryptoProvider, null, null, sharedKey);
    }

    public JwtAuthorizationRequest(
            AuthorizationRequest authorizationRequest, KeyEncryptionAlgorithm keyEncryptionAlgorithm,
            BlockEncryptionAlgorithm blockEncryptionAlgorithm, AbstractCryptoProvider cryptoProvider) {
        this(authorizationRequest, null, cryptoProvider, keyEncryptionAlgorithm, blockEncryptionAlgorithm, null);
    }

    public JwtAuthorizationRequest(
            AuthorizationRequest authorizationRequest, KeyEncryptionAlgorithm keyEncryptionAlgorithm,
            BlockEncryptionAlgorithm blockEncryptionAlgorithm, String sharedKey) {
        this(authorizationRequest, null, null, keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedKey);
    }

    private JwtAuthorizationRequest(
            AuthorizationRequest authorizationRequest, SignatureAlgorithm signatureAlgorithm,
            AbstractCryptoProvider cryptoProvider, KeyEncryptionAlgorithm keyEncryptionAlgorithm,
            BlockEncryptionAlgorithm blockEncryptionAlgorithm, String sharedKey) {
        setAuthorizationRequestParams(authorizationRequest);

        this.type = JwtType.JWT;
        this.signatureAlgorithm = signatureAlgorithm;
        this.cryptoProvider = cryptoProvider;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.blockEncryptionAlgorithm = blockEncryptionAlgorithm;
        this.sharedKey = sharedKey;

        this.userInfoMember = new UserInfoMember();
        this.idTokenMember = new IdTokenMember();
    }

    private void setAuthorizationRequestParams(AuthorizationRequest authorizationRequest) {
        if (authorizationRequest != null) {
            this.responseTypes = authorizationRequest.getResponseTypes();
            this.clientId = authorizationRequest.getClientId();
            this.scopes = authorizationRequest.getScopes();
            this.redirectUri = authorizationRequest.getRedirectUri();
            this.state = authorizationRequest.getState();
            this.nonce = authorizationRequest.getNonce();
            this.display = authorizationRequest.getDisplay();
            this.prompts = authorizationRequest.getPrompts();
            this.maxAge = authorizationRequest.getMaxAge();
            this.uiLocales = authorizationRequest.getUiLocales();
            this.claimsLocales = authorizationRequest.getClaimsLocales();
            this.idTokenHint = authorizationRequest.getIdTokenHint();
            this.loginHint = authorizationRequest.getLoginHint();
            this.acrValues = authorizationRequest.getAcrValues();
            this.registration = authorizationRequest.getRegistration();
            this.requestUniqueId = authorizationRequest.isRequestSessionId();
        }
    }

    public JwtType getType() {
        return type;
    }

    public void setType(JwtType type) {
        this.type = type;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public KeyEncryptionAlgorithm getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    public void setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    }

    public BlockEncryptionAlgorithm getBlockEncryptionAlgorithm() {
        return blockEncryptionAlgorithm;
    }

    public void setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm blockEncryptionAlgorithm) {
        this.blockEncryptionAlgorithm = blockEncryptionAlgorithm;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public boolean isRequestUniqueId() {
        return requestUniqueId;
    }

    public void setRequestUniqueId(boolean p_requestUniqueId) {
        requestUniqueId = p_requestUniqueId;
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

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public List<String> getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(List<String> uiLocales) {
        this.uiLocales = uiLocales;
    }

    public List<String> getClaimsLocales() {
        return claimsLocales;
    }

    public void setClaimsLocales(List<String> claimsLocales) {
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

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    public UserInfoMember getUserInfoMember() {
        return userInfoMember;
    }

    public void setUserInfoMember(UserInfoMember userInfoMember) {
        this.userInfoMember = userInfoMember;
    }

    public IdTokenMember getIdTokenMember() {
        return idTokenMember;
    }

    public void setIdTokenMember(IdTokenMember idTokenMember) {
        this.idTokenMember = idTokenMember;
    }

    public void addUserInfoClaim(Claim claim) {
        userInfoMember.getClaims().add(claim);
    }

    public void addIdTokenClaim(Claim claim) {
        idTokenMember.getClaims().add(claim);
    }

    public String getEncodedJwt(JSONObject jwks) throws Exception {
        String encodedJwt = null;

        if (keyEncryptionAlgorithm != null && blockEncryptionAlgorithm != null) {
            JweEncrypterImpl jweEncrypter;
            if (cryptoProvider != null && jwks != null) {
                PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jwks);
                jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, publicKey);
            } else {
                jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
            }

            String header = toPrettyJson(headerToJSONObject());
            String encodedHeader = Base64Util.base64urlencode(header.getBytes(Util.UTF8_STRING_ENCODING));

            String claims = toPrettyJson(payloadToJSONObject());
            String encodedClaims = Base64Util.base64urlencode(claims.getBytes(Util.UTF8_STRING_ENCODING));

            Jwe jwe = new Jwe();
            jwe.setHeader(new JwtHeader(encodedHeader));
            jwe.setClaims(new JwtClaims(encodedClaims));
            jweEncrypter.encrypt(jwe);

            encodedJwt = jwe.toString();
        } else {
            if (cryptoProvider == null) {
                throw new Exception("The Crypto Provider cannot be null.");
            }

            JSONObject headerJsonObject = headerToJSONObject();
            JSONObject payloadJsonObject = payloadToJSONObject();
            String headerString = headerJsonObject.toString();
            String payloadString = payloadJsonObject.toString();
            String encodedHeader = Base64Util.base64urlencode(headerString.getBytes(Util.UTF8_STRING_ENCODING));
            String encodedPayload = Base64Util.base64urlencode(payloadString.getBytes(Util.UTF8_STRING_ENCODING));
            String signingInput = encodedHeader + "." + encodedPayload;
            String encodedSignature = cryptoProvider.sign(signingInput, keyId, sharedKey, signatureAlgorithm);

            encodedJwt = encodedHeader + "." + encodedPayload + "." + encodedSignature;
        }

        return encodedJwt;
    }

    public String getEncodedJwt() throws Exception {
        return getEncodedJwt(null);
    }

    public String getDecodedJwt() {
        String decodedJwt = null;
        try {
            decodedJwt = toPrettyJson(payloadToJSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
			e.printStackTrace();
		}

        return decodedJwt;
    }

    protected JSONObject headerToJSONObject() throws InvalidJwtException {
        JwtHeader jwtHeader = new JwtHeader();

        jwtHeader.setType(type);
        if (keyEncryptionAlgorithm != null && blockEncryptionAlgorithm != null) {
            jwtHeader.setAlgorithm(keyEncryptionAlgorithm);
            jwtHeader.setEncryptionMethod(blockEncryptionAlgorithm);
        } else {
            jwtHeader.setAlgorithm(signatureAlgorithm);
        }
        jwtHeader.setKeyId(keyId);

        return jwtHeader.toJsonObject();
    }

    protected JSONObject payloadToJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();

        try {
            if (responseTypes != null && !responseTypes.isEmpty()) {
                if (responseTypes.size() == 1) {
                    ResponseType responseType = responseTypes.get(0);
                    obj.put("response_type", responseType);
                } else {
                    JSONArray responseTypeJsonArray = new JSONArray();
                    for (ResponseType responseType : responseTypes) {
                        responseTypeJsonArray.put(responseType);
                    }
                    obj.put("response_type", responseTypeJsonArray);
                }
            }
            if (StringUtils.isNotBlank(clientId)) {
                obj.put("client_id", clientId);
            }
            if (scopes != null && !scopes.isEmpty()) {
                if (scopes.size() == 1) {
                    String scope = scopes.get(0);
                    obj.put("scope", scope);
                } else {
                    JSONArray scopeJsonArray = new JSONArray();
                    for (String scope : scopes) {
                        scopeJsonArray.put(scope);
                    }
                    obj.put("scope", scopeJsonArray);
                }
            }
            if (StringUtils.isNotBlank(redirectUri)) {
                obj.put("redirect_uri", URLEncoder.encode(redirectUri, "UTF-8"));
            }
            if (StringUtils.isNotBlank(state)) {
                obj.put("state", state);
            }
            if (StringUtils.isNotBlank(nonce)) {
                obj.put("nonce", nonce);
            }
            if (display != null) {
                obj.put("display", display);
            }
            if (prompts != null && !prompts.isEmpty()) {
                JSONArray promptJsonArray = new JSONArray();
                for (Prompt prompt : prompts) {
                    promptJsonArray.put(prompt);
                }
                obj.put("prompt", promptJsonArray);
            }
            if (maxAge != null) {
                obj.put("max_age", maxAge);
            }
            if (uiLocales != null && !uiLocales.isEmpty()) {
                JSONArray uiLocalesJsonArray = new JSONArray(uiLocales);
                obj.put("ui_locales", uiLocalesJsonArray);
            }
            if (claimsLocales != null && !claimsLocales.isEmpty()) {
                JSONArray claimsLocalesJsonArray = new JSONArray(claimsLocales);
                obj.put("claims_locales", claimsLocalesJsonArray);
            }
            if (StringUtils.isNotBlank(idTokenHint)) {
                obj.put("id_token_hint", idTokenHint);
            }
            if (StringUtils.isNotBlank(loginHint)) {
                obj.put("login_hint", loginHint);
            }
            if (acrValues != null && !acrValues.isEmpty()) {
                JSONArray acrValuesJsonArray = new JSONArray(acrValues);
                obj.put("acr_values", acrValues);
            }
            if (StringUtils.isNotBlank(registration)) {
                obj.put("registration", registration);
            }

            if (userInfoMember != null || idTokenMember != null) {
                JSONObject claimsObj = new JSONObject();

                if (userInfoMember != null) {
                    claimsObj.put("userinfo", userInfoMember.toJSONObject());
                }
                if (idTokenMember != null) {
                    claimsObj.put("id_token", idTokenMember.toJSONObject());
                }

                obj.put("claims", claimsObj);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return obj;
    }

	public String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JsonOrgModule());
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
	}

}