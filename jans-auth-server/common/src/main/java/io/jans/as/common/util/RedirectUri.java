/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.util;

import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwe.JweEncrypterImpl;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.util.*;

import static io.jans.as.model.authorize.AuthorizeResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version September 9, 2021
 */
public class RedirectUri {

    private String baseRedirectUri;
    private List<ResponseType> responseTypes;
    private ResponseMode responseMode;
    private final Map<String, String> responseParameters;

    // JARM
    private String issuer;
    private String audience;
    private int authorizationCodeLifetime;
    private SignatureAlgorithm signatureAlgorithm;
    private KeyEncryptionAlgorithm keyEncryptionAlgorithm;
    private BlockEncryptionAlgorithm blockEncryptionAlgorithm;
    private String keyId;
    private String nestedKeyId;
    private String sharedSecret;
    private JSONObject jsonWebKeys;
    private byte[] sharedSymmetricKey;
    private String nestedSharedSecret;
    private JSONObject nestedJsonWebKeys;
    private AbstractCryptoProvider cryptoProvider;

    public RedirectUri(String baseRedirectUri) {
        this.baseRedirectUri = baseRedirectUri;
        this.responseMode = ResponseMode.QUERY;

        this.responseParameters = new HashMap<>();
    }

    public RedirectUri(String baseRedirectUri, List<ResponseType> responseTypes, ResponseMode responseMode) {
        this(baseRedirectUri);
        this.responseTypes = responseTypes;
        this.responseMode = responseMode;
    }

    public String getBaseRedirectUri() {
        return baseRedirectUri;
    }

    public void setBaseRedirectUri(String baseRedirectUri) {
        this.baseRedirectUri = baseRedirectUri;
    }

    public ResponseMode getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(ResponseMode responseMode) {
        this.responseMode = responseMode;
    }

    public void addResponseParameter(String key, String value) {
        if (StringUtils.isNotBlank(key)) {
            responseParameters.put(key, value);
        }
    }

    public void addResponseParameterIfNotBlank(String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            addResponseParameter(key, value);
        }
    }

    @Nullable
    public String getResponseParameter(@NotNull String key) {
        return responseParameters.get(key);
    }

    public int getResponseParamentersSize() {
        return responseParameters != null ? responseParameters.size() : 0;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public int getAuthorizationCodeLifetime() {
        return authorizationCodeLifetime;
    }

    public void setAuthorizationCodeLifetime(int authorizationCodeLifetime) {
        this.authorizationCodeLifetime = authorizationCodeLifetime;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
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

    public String getNestedKeyId() {
        return nestedKeyId;
    }

    public void setNestedKeyId(String nestedKeyId) {
        this.nestedKeyId = nestedKeyId;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public JSONObject getJsonWebKeys() {
        return jsonWebKeys;
    }

    public void setJsonWebKeys(JSONObject jsonWebKeys) {
        this.jsonWebKeys = jsonWebKeys;
    }

    public byte[] getSharedSymmetricKey() {
        return sharedSymmetricKey;
    }

    public void setSharedSymmetricKey(byte[] sharedSymmetricKey) {
        this.sharedSymmetricKey = sharedSymmetricKey;
    }

    public String getNestedSharedSecret() {
        return nestedSharedSecret;
    }

    public void setNestedSharedSecret(String nestedSharedSecret) {
        this.nestedSharedSecret = nestedSharedSecret;
    }

    public JSONObject getNestedJsonWebKeys() {
        return nestedJsonWebKeys;
    }

    public void setNestedJsonWebKeys(JSONObject nestedJsonWebKeys) {
        this.nestedJsonWebKeys = nestedJsonWebKeys;
    }

    public AbstractCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public void parseQueryString(String queryString) {
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&", false);
            while (st.hasMoreElements()) {
                String nameValueToken = st.nextElement().toString();

                StringTokenizer stParamValue = new StringTokenizer(nameValueToken, "=", false);

                if (stParamValue.countTokens() == 1) {
                    String paramName = stParamValue.nextElement().toString();
                    responseParameters.put(paramName, null);
                } else if (stParamValue.countTokens() == 2) {
                    try {
                        String paramName = stParamValue.nextElement().toString();
                        String paramValue = URLDecoder.decode(stParamValue.nextElement().toString(), Util.UTF8_STRING_ENCODING);
                        responseParameters.put(paramName, paramValue);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getQueryString() {
        StringBuilder sb = new StringBuilder();

        try {
            if (responseMode == ResponseMode.JWT || responseMode == ResponseMode.QUERY_JWT || responseMode == ResponseMode.FRAGMENT_JWT) {
                final String responseJwt = getJarmResponse();
                sb.append(URLEncoder.encode(RESPONSE, Util.UTF8_STRING_ENCODING));
                sb.append('=').append(URLEncoder.encode(responseJwt, Util.UTF8_STRING_ENCODING));
            } else if (responseMode == ResponseMode.FORM_POST_JWT) {
                final String responseJwt = getJarmResponse();
                sb.append(responseJwt);
            } else {
                for (Map.Entry<String, String> entry : responseParameters.entrySet()) {

                    if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())) {
                        if (sb.length() > 0) {
                            sb.append('&');
                        }
                        sb.append(URLEncoder.encode(entry.getKey(), Util.UTF8_STRING_ENCODING));
                        sb.append('=').append(URLEncoder.encode(entry.getValue(), Util.UTF8_STRING_ENCODING));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String getJarmResponse() throws InvalidJweException, InvalidJwtException, CryptoProviderException {
        if (keyEncryptionAlgorithm != null && blockEncryptionAlgorithm != null) {
            if (signatureAlgorithm != null) {
                String jws = getJwsResponse(true);
                return getJweResponse(jws);
            } else {
                return getJweResponse(null);
            }
        } else {
            if (signatureAlgorithm == null) {
                signatureAlgorithm = SignatureAlgorithm.RS256;
            }

            return getJwsResponse(false);
        }
    }

    private String getJwsResponse(boolean nested) throws InvalidJwtException, CryptoProviderException {
        Jwt jwt = new Jwt();

        // Header
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        if (nested) {
            if (nestedKeyId != null) {
                jwt.getHeader().setKeyId(nestedKeyId);
            }
        } else if (keyId != null) {
            jwt.getHeader().setKeyId(keyId);
        }

        // Claims
        jwt.getClaims().setClaim(ISS, issuer);
        jwt.getClaims().setClaim(AUD, audience);
        if (responseParameters.containsKey(EXPIRES_IN)) {
            jwt.getClaims().setClaim(EXP, responseParameters.get(EXPIRES_IN));
        } else {
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, authorizationCodeLifetime);
            jwt.getClaims().setClaim(EXP, calendar.getTime());
        }
        for (Map.Entry<String, String> entry : responseParameters.entrySet()) {
            jwt.getClaims().setClaim(entry.getKey(), entry.getValue());
        }

        // Signature
        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), nested ? nestedSharedSecret : sharedSecret, signatureAlgorithm);
        jwt.setEncodedSignature(signature);
        return jwt.toString();
    }

    private String getJweResponse(String nestedJws) throws InvalidJweException, InvalidJwtException, CryptoProviderException {
        Jwe jwe = new Jwe();

        // Header
        jwe.getHeader().setType(JwtType.JWT);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);

        // Claims
        if (nestedJws == null) {
            jwe.getClaims().setClaim(ISS, issuer);
            jwe.getClaims().setClaim(AUD, audience);
            if (responseParameters.containsKey(EXPIRES_IN)) {
                jwe.getClaims().setClaim(EXP, responseParameters.get(EXPIRES_IN));
            } else {
                final Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, authorizationCodeLifetime);
                jwe.getClaims().setClaim(EXP, calendar.getTime());
            }
            for (Map.Entry<String, String> entry : responseParameters.entrySet()) {
                jwe.getClaims().setClaim(entry.getKey(), entry.getValue());
            }
        } else {
            Jwt jwt = Jwt.parse(nestedJws);
            jwe.setSignedJWTPayload(jwt);
        }

        //Encryption
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
            PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jsonWebKeys, null);
            if (publicKey == null) {
                throw new InvalidJweException("The public key is not valid");
            }

            JweEncrypterImpl jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, publicKey);
            jwe = jweEncrypter.encrypt(jwe);
        } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            JweEncrypterImpl jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedSymmetricKey);
            jwe = jweEncrypter.encrypt(jwe);
        }

        return jwe.toString();
    }

    private void appendQuerySymbol(StringBuilder sb) {
        if (!sb.toString().contains("?")) {
            sb.append("?");
        } else {
            sb.append("&");
        }
    }

    private void appendFragmentSymbol(StringBuilder sb) {
        if (!sb.toString().contains("#")) {
            sb.append("#");
        } else {
            sb.append("&");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(baseRedirectUri);
        if (responseParameters.isEmpty()) {
            return sb.toString();
        }
        if (responseMode == ResponseMode.FORM_POST) {
            sb = new StringBuilder();
            sb.append("<html>");
            sb.append("<head><title>Submit This Form</title></head>");
            sb.append("<body onload=\"javascript:document.forms[0].submit()\">");
            sb.append("<form method=\"post\" action=\"").append(baseRedirectUri).append("\">");
            for (Map.Entry<String, String> entry : responseParameters.entrySet()) {
                String entryValue = StringEscapeUtils.escapeHtml(entry.getValue());
                sb.append("<input type=\"hidden\" name=\"").append(entry.getKey()).append("\" value=\"").append(entryValue).append("\"/>");
            }
            sb.append("</form>");
            sb.append("</body>");
            sb.append("</html>");
        } else if (responseMode == ResponseMode.FORM_POST_JWT) {
            sb = new StringBuilder();
            sb.append("<html>");
            sb.append("<head><title>Submit This Form</title></head>");
            sb.append("<body onload=\"javascript:document.forms[0].submit()\">");
            sb.append("<form method=\"post\" action=\"").append(baseRedirectUri).append("\">");
            sb.append("<input type=\"hidden\" name=\"response\"").append(" value=\"").append(getQueryString()).append("\"/>");
            sb.append("</form>");
            sb.append("</body>");
            sb.append("</html>");
        } else if (responseMode == ResponseMode.QUERY || responseMode == ResponseMode.QUERY_JWT) {
            appendQuerySymbol(sb);
            sb.append(getQueryString());
        } else if (responseMode == ResponseMode.FRAGMENT || responseMode == ResponseMode.FRAGMENT_JWT) {
            appendFragmentSymbol(sb);
            sb.append(getQueryString());
        } else if (responseTypes != null && responseMode == ResponseMode.JWT) {
            if (responseTypes.contains(ResponseType.TOKEN)) {
                appendFragmentSymbol(sb);
            } else if (responseTypes.contains(ResponseType.CODE)) {
                appendQuerySymbol(sb);
            }
            sb.append(getQueryString());
        } else {
            appendDefaultToString(sb);
        }
        return sb.toString();
    }

    private void appendDefaultToString(StringBuilder sb) {
        if (responseMode == null) {
            if (responseTypes != null && (responseTypes.contains(ResponseType.TOKEN) || responseTypes.contains(ResponseType.ID_TOKEN))) {
                appendFragmentSymbol(sb);
            } else {
                appendQuerySymbol(sb);
            }
        }
        sb.append(getQueryString());
    }
}