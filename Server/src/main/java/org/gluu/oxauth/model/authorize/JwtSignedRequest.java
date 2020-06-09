package org.gluu.oxauth.model.authorize;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwe.JweDecrypterImpl;
import org.gluu.oxauth.model.jwt.JwtHeader;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.service.ClientService;
import org.gluu.service.cdi.util.CdiUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.List;

/**
 * Abstract class used to load JWT request data.
 */
public abstract class JwtSignedRequest {

    // Header
    protected String type;
    protected String algorithm;
    protected String encryptionAlgorithm;
    protected String keyId;

    // Payload
    protected List<String> scopes = Lists.newArrayList();
    protected List<String> aud = Lists.newArrayList();
    protected Integer exp;

    protected String encodedJwt;
    protected String payload;

    public JwtSignedRequest(AppConfiguration appConfiguration,
                            AbstractCryptoProvider cryptoProvider,
                            String encodedJwt,
                            Client client) throws InvalidJwtException {
        this.encodedJwt = encodedJwt;
        try {
            if (StringUtils.isEmpty(encodedJwt)) {
                throw new InvalidJwtException("The JWT is null or empty");
            }

            String[] parts = encodedJwt.split("\\.");
            if (parts.length == 5) {
                processEncryptedRequest(parts, cryptoProvider, client);
            } else if (parts.length == 2 || parts.length == 3) {
                processSignedRequest(parts, appConfiguration, cryptoProvider, client);
            } else {
                throw new InvalidJwtException("The JWT is not well formed");
            }

        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }
    }

    /**
     * Process the encrypted request and load all data related to the request.
     * @param parts Sections of the JWT.
     * @param cryptoProvider Service used to decrypt.
     * @param client Client that sent the request.
     */
    private void processEncryptedRequest(String[] parts, AbstractCryptoProvider cryptoProvider, Client client) throws Exception {
        String encodedHeader = parts[0];

        JwtHeader jwtHeader = new JwtHeader(encodedHeader);

        keyId = jwtHeader.getKeyId();
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(
                jwtHeader.getClaimAsString(JwtHeaderName.ALGORITHM));
        BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(
                jwtHeader.getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD));

        JweDecrypterImpl jweDecrypter = null;
        if ("RSA".equals(keyEncryptionAlgorithm.getFamily())) {
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
            jweDecrypter = new JweDecrypterImpl(privateKey);
        } else {
            ClientService clientService = CdiUtil.bean(ClientService.class);
            jweDecrypter = new JweDecrypterImpl(clientService.decryptSecret(client.getClientSecret())
                    .getBytes(StandardCharsets.UTF_8));
        }
        jweDecrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
        jweDecrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

        Jwe jwe = jweDecrypter.decrypt(encodedJwt);

        loadHeader(jwe.getHeader().toJsonString());
        loadPayload(jwe.getClaims().toJsonString());
    }

    /**
     * Process the signed request and load all data related to the request.
     * @param parts Sections of the JWT.
     * @param appConfiguration Service used to get all configuration.
     * @param cryptoProvider Service used to decrypt.
     * @param client Client that sent the request.
     */
    private void processSignedRequest(String[] parts, AppConfiguration appConfiguration,
                                      AbstractCryptoProvider cryptoProvider, Client client) throws Exception {
        String encodedHeader = parts[0];
        String encodedClaim = parts[1];
        String encodedSignature = StringUtils.EMPTY;
        if (parts.length == 3) {
            encodedSignature = parts[2];
        }

        String signingInput = encodedHeader + "." + encodedClaim;
        String header = new String(Base64Util.base64urldecode(encodedHeader), StandardCharsets.UTF_8);
        String payload = new String(Base64Util.base64urldecode(encodedClaim), StandardCharsets.UTF_8);
        payload = payload.replace("\\", "");

        loadHeader(header);

        SignatureAlgorithm sigAlg = SignatureAlgorithm.fromString(algorithm);
        if (sigAlg == null) {
            throw new InvalidJwtException("The JWT algorithm is not supported");
        }
        if (sigAlg == SignatureAlgorithm.NONE && appConfiguration.getFapiCompatibility()) {
            throw new InvalidJwtException("None algorithm is not allowed for FAPI");
        }
        if (!validateSignature(cryptoProvider, sigAlg, client, signingInput, encodedSignature)) {
            throw new InvalidJwtException("The JWT signature is not valid");
        }

        loadPayload(payload);
    }

    /**
     * Method responsible to load corresponding data according to the type of request.
     * @param payload Payload containing custom data related to every type of request.
     */
    abstract void loadPayload(String payload) throws JSONException, UnsupportedEncodingException;

    /**
     * Load header data from the request object.
     * @param header Header section.
     */
    protected void loadHeader(String header) throws JSONException {
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

    /**
     * Verifies that the signature in the JWT request is correct using client data.
     * @param cryptoProvider Service used to validate the signature
     * @param signatureAlgorithm Algorithm used in the signature
     * @param client Client which is going to be used to get the secret.
     * @param signingInput Sections used to verify the signture
     * @param signature Signature sent in the request.
     */
    protected boolean validateSignature(AbstractCryptoProvider cryptoProvider,
                                      SignatureAlgorithm signatureAlgorithm,
                                      Client client,
                                      String signingInput,
                                      String signature) throws Exception {
        ClientService clientService = CdiUtil.bean(ClientService.class);
        String sharedSecret = clientService.decryptSecret(client.getClientSecret());
        JSONObject jwks = Strings.isNullOrEmpty(client.getJwks()) ?
                JwtUtil.getJSONWebKeys(client.getJwksUri()) :
                new JSONObject(client.getJwks());
        return cryptoProvider.verifySignature(signingInput, signature, keyId, jwks, sharedSecret, signatureAlgorithm);
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

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getEncodedJwt() {
        return encodedJwt;
    }

    public void setEncodedJwt(String encodedJwt) {
        this.encodedJwt = encodedJwt;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getAud() {
        if (aud == null) aud = Lists.newArrayList();
        return aud;
    }

    public void setAud(List<String> aud) {
        this.aud = aud;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }
}
