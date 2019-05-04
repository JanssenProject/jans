/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.common.Display;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.CryptoProviderFactory;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJweException;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwe.JweDecrypterImpl;
import org.gluu.oxauth.model.jwt.JwtHeader;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.ClientService;
import org.gluu.service.cdi.util.CdiUtil;
import org.gluu.util.security.StringEncrypter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version November 20, 2018
 */
public class JwtAuthorizationRequest {

    // Header
    private String type;
    private String algorithm;
    private String encryptionAlgorithm;
    private String keyId;

    // Payload
    private List<ResponseType> responseTypes;
    private String clientId;
    private List<String> scopes;
    private String redirectUri;
    private String nonce;
    private String state;
    private Display display;
    private List<Prompt> prompts;
    private UserInfoMember userInfoMember;
    private IdTokenMember idTokenMember;

    private String encodedJwt;

    private AppConfiguration appConfiguration;

    public JwtAuthorizationRequest(AppConfiguration appConfiguration, String encodedJwt, Client client) throws InvalidJwtException, InvalidJweException {
        try {
            this.appConfiguration = appConfiguration;
            this.responseTypes = new ArrayList<ResponseType>();
            this.scopes = new ArrayList<String>();
            this.prompts = new ArrayList<Prompt>();
            this.encodedJwt = encodedJwt;

            if (encodedJwt != null && !encodedJwt.isEmpty()) {
                String[] parts = encodedJwt.split("\\.");

                if (parts.length == 5) {
                    String encodedHeader = parts[0];
                    String encodedEncryptedKey = parts[1];
                    String encodedInitializationVector = parts[2];
                    String encodedCipherText = parts[3];
                    String encodedIntegrityValue = parts[4];

                    JwtHeader jwtHeader = new JwtHeader(encodedHeader);

                    keyId = jwtHeader.getKeyId();
                    KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(
                            jwtHeader.getClaimAsString(JwtHeaderName.ALGORITHM));
                    BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(
                            jwtHeader.getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD));

                    JweDecrypterImpl jweDecrypter = null;
                    if ("RSA".equals(keyEncryptionAlgorithm.getFamily())) {
                        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(appConfiguration.getKeyStoreFile(),
                                appConfiguration.getKeyStoreSecret(), appConfiguration.getDnName());
                        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
                        jweDecrypter = new JweDecrypterImpl(privateKey);
                    } else {
                        ClientService clientService = CdiUtil.bean(ClientService.class);
                        jweDecrypter = new JweDecrypterImpl(clientService.decryptSecret(client.getClientSecret()).getBytes(Util.UTF8_STRING_ENCODING));
                    }
                    jweDecrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    jweDecrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

                    Jwe jwe = jweDecrypter.decrypt(encodedJwt);

                    loadHeader(jwe.getHeader().toJsonString());
                    loadPayload(jwe.getClaims().toJsonString());
                } else if (parts.length == 2 || parts.length == 3) {
                    String encodedHeader = parts[0];
                    String encodedClaim = parts[1];
                    String encodedSignature = StringUtils.EMPTY;
                    if (parts.length == 3) {
                        encodedSignature = parts[2];
                    }

                    String signingInput = encodedHeader + "." + encodedClaim;
                    String header = new String(Base64Util.base64urldecode(encodedHeader), Util.UTF8_STRING_ENCODING);
                    String payload = new String(Base64Util.base64urldecode(encodedClaim), Util.UTF8_STRING_ENCODING);
                    payload = payload.replace("\\", "");

                    JSONObject jsonHeader = new JSONObject(header);

                    if (jsonHeader.has("typ")) {
                        type = jsonHeader.getString("typ");
                    }
                    if (jsonHeader.has("alg")) {
                        algorithm = jsonHeader.getString("alg");
                    }
                    if (jsonHeader.has("kid")) {
                        keyId = jsonHeader.getString("kid");
                    }

                    SignatureAlgorithm sigAlg = SignatureAlgorithm.fromString(algorithm);
                    if (sigAlg != null) {
                        if (validateSignature(sigAlg, client, signingInput, encodedSignature)) {
                            JSONObject jsonPayload = new JSONObject(payload);

                            if (jsonPayload.has("response_type")) {
                                JSONArray responseTypeJsonArray = jsonPayload.optJSONArray("response_type");
                                if (responseTypeJsonArray != null) {
                                    for (int i = 0; i < responseTypeJsonArray.length(); i++) {
                                        ResponseType responseType = ResponseType.fromString(responseTypeJsonArray.getString(i));
                                        responseTypes.add(responseType);
                                    }
                                } else {
                                    responseTypes.addAll(ResponseType.fromString(jsonPayload.getString("response_type"), " "));
                                }
                            }
                            if (jsonPayload.has("client_id")) {
                                clientId = jsonPayload.getString("client_id");
                            }
                            if (jsonPayload.has("scope")) {
                                JSONArray scopesJsonArray = jsonPayload.optJSONArray("scope");
                                if (scopesJsonArray != null) {
                                    for (int i = 0; i < scopesJsonArray.length(); i++) {
                                        String scope = scopesJsonArray.getString(i);
                                        scopes.add(scope);
                                    }
                                } else {
                                    String scopeStringList = jsonPayload.getString("scope");
                                    scopes.addAll(Util.splittedStringAsList(scopeStringList, " "));
                                }
                            }
                            if (jsonPayload.has("redirect_uri")) {
                                redirectUri = URLDecoder.decode(jsonPayload.getString("redirect_uri"), "UTF-8");
                            }
                            if (jsonPayload.has("nonce")) {
                                nonce = jsonPayload.getString("nonce");
                            }
                            if (jsonPayload.has("state")) {
                                state = jsonPayload.getString("state");
                            }
                            if (jsonPayload.has("display")) {
                                display = Display.fromString(jsonPayload.getString("display"));
                            }
                            if (jsonPayload.has("prompt")) {
                                JSONArray promptJsonArray = jsonPayload.optJSONArray("prompt");
                                if (promptJsonArray != null) {
                                    for (int i = 0; i < promptJsonArray.length(); i++) {
                                        Prompt prompt = Prompt.fromString(promptJsonArray.getString(i));
                                        prompts.add(prompt);
                                    }
                                } else {
                                    prompts.addAll(Prompt.fromString(jsonPayload.getString("prompt"), " "));
                                }
                            }
                            if (jsonPayload.has("claims")) {
                                JSONObject claimsJsonObject = jsonPayload.getJSONObject("claims");

                                if (claimsJsonObject.has("userinfo")) {
                                    userInfoMember = new UserInfoMember(claimsJsonObject.getJSONObject("userinfo"));
                                }
                                if (claimsJsonObject.has("id_token")) {
                                    idTokenMember = new IdTokenMember(claimsJsonObject.getJSONObject("id_token"));
                                }
                            }
                        } else {
                            throw new InvalidJwtException("The JWT signature is not valid");
                        }
                    } else {
                        throw new InvalidJwtException("The JWT algorithm is not supported");
                    }
                } else {
                    throw new InvalidJwtException("The JWT is not well formed");
                }
            } else {
                throw new InvalidJwtException("The JWT is null or empty");
            }
        } catch (JSONException e) {
            throw new InvalidJwtException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJwtException(e);
        } catch (StringEncrypter.EncryptionException e) {
            throw new InvalidJwtException(e);
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }
    }

    public String getEncodedJwt() {
        return encodedJwt;
    }

    private void loadHeader(String header) throws JSONException {
        JSONObject jsonHeader = new JSONObject(header);

        if (jsonHeader.has("typ")) {
            type = jsonHeader.getString("typ");
        }
        if (jsonHeader.has("alg")) {
            algorithm = jsonHeader.getString("alg");
        }
        if (jsonHeader.has("enc")) {
            encryptionAlgorithm = jsonHeader.getString("enc");
        }
        if (jsonHeader.has("kid")) {
            keyId = jsonHeader.getString("kid");
        }
    }

    private void loadPayload(String payload) throws JSONException, UnsupportedEncodingException {
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("response_type")) {
            JSONArray responseTypeJsonArray = jsonPayload.optJSONArray("response_type");
            if (responseTypeJsonArray != null) {
                for (int i = 0; i < responseTypeJsonArray.length(); i++) {
                    ResponseType responseType = ResponseType.fromString(responseTypeJsonArray.getString(i));
                    responseTypes.add(responseType);
                }
            } else {
                responseTypes.addAll(ResponseType.fromString(jsonPayload.getString("response_type"), " "));
            }
        }
        if (jsonPayload.has("client_id")) {
            clientId = jsonPayload.getString("client_id");
        }
        if (jsonPayload.has("scope")) {
            JSONArray scopesJsonArray = jsonPayload.optJSONArray("scope");
            if (scopesJsonArray != null) {
                for (int i = 0; i < scopesJsonArray.length(); i++) {
                    String scope = scopesJsonArray.getString(i);
                    scopes.add(scope);
                }
            } else {
                String scopeStringList = jsonPayload.getString("scope");
                scopes.addAll(Util.splittedStringAsList(scopeStringList, " "));
            }
        }
        if (jsonPayload.has("redirect_uri")) {
            redirectUri = URLDecoder.decode(jsonPayload.getString("redirect_uri"), "UTF-8");
        }
        if (jsonPayload.has("nonce")) {
            nonce = jsonPayload.getString("nonce");
        }
        if (jsonPayload.has("state")) {
            state = jsonPayload.getString("state");
        }
        if (jsonPayload.has("display")) {
            display = Display.fromString(jsonPayload.getString("display"));
        }
        if (jsonPayload.has("prompt")) {
            JSONArray promptJsonArray = jsonPayload.optJSONArray("prompt");
            if (promptJsonArray != null) {
                for (int i = 0; i < promptJsonArray.length(); i++) {
                    Prompt prompt = Prompt.fromString(promptJsonArray.getString(i));
                    prompts.add(prompt);
                }
            } else {
                prompts.addAll(Prompt.fromString(jsonPayload.getString("prompt"), " "));
            }
        }
        if (jsonPayload.has("claims")) {
            JSONObject claimsJsonObject = jsonPayload.getJSONObject("claims");

            if (claimsJsonObject.has("userinfo")) {
                userInfoMember = new UserInfoMember(claimsJsonObject.getJSONObject("userinfo"));
            }
            if (claimsJsonObject.has("id_token")) {
                idTokenMember = new IdTokenMember(claimsJsonObject.getJSONObject("id_token"));
            }
        }
    }

    private boolean validateSignature(SignatureAlgorithm signatureAlgorithm, Client client, String signingInput, String signature) throws Exception {
        ClientService clientService = CdiUtil.bean(ClientService.class);
        String sharedSecret = clientService.decryptSecret(client.getClientSecret());
        JSONObject jwks = Strings.isNullOrEmpty(client.getJwks()) ?
                JwtUtil.getJSONWebKeys(client.getJwksUri()) :
                new JSONObject(client.getJwks());
        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(
                appConfiguration);
        boolean validSignature = cryptoProvider.verifySignature(signingInput, signature, keyId, jwks, sharedSecret, signatureAlgorithm);

        return validSignature;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
}