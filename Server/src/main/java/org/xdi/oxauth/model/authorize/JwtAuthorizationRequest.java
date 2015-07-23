/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.authorize;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.Display;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.JweDecrypterImpl;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.util.security.StringEncrypter;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version 0.9 May 18, 2015
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

    public JwtAuthorizationRequest(String encodedJwt, Client client) throws InvalidJwtException, InvalidJweException {
        try {
            responseTypes = new ArrayList<ResponseType>();
            scopes = new ArrayList<String>();
            prompts = new ArrayList<Prompt>();
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
                        JSONWebKeySet jwks = ConfigurationFactory.instance().getWebKeys();
                        JSONWebKey jwk = jwks.getKey(keyId);
                        RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(
                                jwk.getPrivateKey().getModulus(),
                                jwk.getPrivateKey().getPrivateExponent());
                        jweDecrypter = new JweDecrypterImpl(rsaPrivateKey);
                    } else {
                        jweDecrypter = new JweDecrypterImpl(client.getClientSecret().getBytes(Util.UTF8_STRING_ENCODING));
                    }
                    jweDecrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    jweDecrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

                    byte[] contentMasterKey = jweDecrypter.decryptEncryptionKey(encodedEncryptedKey);
                    byte[] initializationVector = JwtUtil.base64urldecode(encodedInitializationVector);
                    byte[] authenticationTag = JwtUtil.base64urldecode(encodedIntegrityValue);
                    String additionalAuthenticatedData = encodedHeader + "."
                            + encodedEncryptedKey + "."
                            + encodedInitializationVector;

                    String encodedClaim = jweDecrypter.decryptCipherText(encodedCipherText, contentMasterKey, initializationVector,
                            authenticationTag, additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING));
                    String header = new String(JwtUtil.base64urldecode(encodedHeader), Util.UTF8_STRING_ENCODING);
                    String payload = new String(JwtUtil.base64urldecode(encodedClaim), Util.UTF8_STRING_ENCODING);
                    payload = payload.replace("\\", "");

                    loadHeader(header);
                    loadPayload(payload);
                } else if (parts.length == 2 || parts.length == 3) {
                    String encodedHeader = parts[0];
                    String encodedClaim = parts[1];
                    String encodedSignature = StringUtils.EMPTY;
                    if (parts.length == 3) {
                        encodedSignature = parts[2];
                    }

                    String signingInput = encodedHeader + "." + encodedClaim;
                    String header = new String(JwtUtil.base64urldecode(encodedHeader), Util.UTF8_STRING_ENCODING);
                    String payload = new String(JwtUtil.base64urldecode(encodedClaim), Util.UTF8_STRING_ENCODING);
                    payload = payload.replace("\\", "");
                    byte[] signature = JwtUtil.base64urldecode(encodedSignature);

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

                    SignatureAlgorithm sigAlg = SignatureAlgorithm.fromName(algorithm);
                    if (sigAlg != null) {
                        if (validateSignature(sigAlg, client, signingInput, signature)) {
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

    private boolean validateSignature(SignatureAlgorithm signatureAlgorithm, Client client, String signingInput, byte[] signature) throws InvalidJwtException {
        boolean validSignature = false;

        try {
            if (StringUtils.isNotBlank(client.getRequestObjectSigningAlg())) {
                SignatureAlgorithm clientSignatureAlgorithm = SignatureAlgorithm.fromName(client.getRequestObjectSigningAlg());
                if (signatureAlgorithm != clientSignatureAlgorithm) {
                    return false;
                }
            }

            if (signatureAlgorithm == SignatureAlgorithm.NONE) {
                return true;
            }
            if (signatureAlgorithm == SignatureAlgorithm.HS256 || signatureAlgorithm == SignatureAlgorithm.HS384 || signatureAlgorithm == SignatureAlgorithm.HS512) {
                // Shared key
                String sharedKey = client.getClientSecret();

                // Validate the crypto segment
                byte[] signature2 = null;
                switch (signatureAlgorithm) {
                    case HS256:
                        signature2 = JwtUtil.getSignatureHS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
                        validSignature = Arrays.equals(signature, signature2);
                        break;
                    case HS384:
                        signature2 = JwtUtil.getSignatureHS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
                        validSignature = Arrays.equals(signature, signature2);
                        break;
                    case HS512:
                        signature2 = JwtUtil.getSignatureHS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
                        validSignature = Arrays.equals(signature, signature2);
                        break;
                    default:
                        throw new InvalidJwtException("The algorithm is not supported");
                }
            } else {
                if (client.getJwksUri() != null) {
                    // Public Key
                    PublicKey publicKey = JwtUtil.getPublicKey(client.getJwksUri(), null, signatureAlgorithm, keyId);
                    if (publicKey == null) {
                        throw new InvalidJwtException("Cannot retrieve the JWK file");
                    }

                    // Validate the crypto segment
                    if (publicKey.getCertificate() != null) {
                        Certificate cert = publicKey.getCertificate();
                        byte[] signature2 = null;
                        switch (signatureAlgorithm) {
                            case RS256:
                                validSignature = JwtUtil.verifySignatureRS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, cert.getRsaPublicKey());
                                break;
                            case RS384:
                                validSignature = JwtUtil.verifySignatureRS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, cert.getRsaPublicKey());
                                break;
                            case RS512:
                                validSignature = JwtUtil.verifySignatureRS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, cert.getRsaPublicKey());
                                break;
                            case ES256:
                                validSignature = JwtUtil.verifySignatureES256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, cert.getEcdsaPublicKey());
                                break;
                            case ES384:
                                validSignature = JwtUtil.verifySignatureES384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, cert.getEcdsaPublicKey());
                                break;
                            case ES512:
                                validSignature = JwtUtil.verifySignatureES512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, cert.getEcdsaPublicKey());
                                break;
                            default:
                                throw new InvalidJwtException("The algorithm is not supported");
                        }
                    } else {
                        byte[] signature2 = null;
                        switch (signatureAlgorithm) {
                            case RS256:
                                validSignature = JwtUtil.verifySignatureRS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, (RSAPublicKey) publicKey);
                                break;
                            case RS384:
                                validSignature = JwtUtil.verifySignatureRS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, (RSAPublicKey) publicKey);
                                break;
                            case RS512:
                                validSignature = JwtUtil.verifySignatureRS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, (RSAPublicKey) publicKey);
                                break;
                            case ES256:
                                validSignature = JwtUtil.verifySignatureES256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, (ECDSAPublicKey) publicKey);
                                break;
                            case ES384:
                                validSignature = JwtUtil.verifySignatureES384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, (ECDSAPublicKey) publicKey);
                                break;
                            case ES512:
                                validSignature = JwtUtil.verifySignatureES512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, (ECDSAPublicKey) publicKey);
                                break;
                            default:
                                throw new InvalidJwtException("The algorithm is not supported");
                        }
                    }
                }
            }
        } catch (StringEncrypter.EncryptionException e) {
            throw new InvalidJwtException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidJwtException(e);
        } catch (SignatureException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchProviderException e) {
            throw new InvalidJwtException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJwtException(e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidJwtException(e);
        } catch (IllegalBlockSizeException e) {
            throw new InvalidJwtException(e);
        } catch (BadPaddingException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchPaddingException e) {
            throw new InvalidJwtException(e);
        } catch (IOException e) {
            throw new InvalidJwtException(e);
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }

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