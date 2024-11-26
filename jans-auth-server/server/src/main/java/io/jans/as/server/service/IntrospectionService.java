package io.jans.as.server.service;

import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwe.JweEncrypter;
import io.jans.as.model.jwe.JweEncrypterImpl;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.KeyOpsType;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.util.security.StringEncrypter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */

@Named
public class IntrospectionService {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Logger log;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    public void validateIntrospectionScopePresence(AuthorizationGrant authorizationGrant) {
        if (isTrue(appConfiguration.getIntrospectionAccessTokenMustHaveIntrospectionScope()) &&
                !authorizationGrant.getScopesAsString().contains(ScopeConstants.INTROSPECTION)) {
            final String reason = "access_token used to access introspection endpoint does not have 'introspection' scope, however in AS configuration 'introspectionAccessTokenMustHaveIntrospectionScope' is true";
            log.trace(reason);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, reason)).type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

    public boolean isJwtResponse(String responseAsJwt, String acceptHeader) {
        return Boolean.TRUE.toString().equalsIgnoreCase(responseAsJwt) ||
                Constants.APPLICATION_TOKEN_INTROSPECTION_JWT.equalsIgnoreCase(acceptHeader);
    }

    public String createResponseJwt(JSONObject response, AuthorizationGrant grant) throws Exception {
        final Client client = grant.getClient();

        KeyEncryptionAlgorithm keyAlgorithm = KeyEncryptionAlgorithm.fromName(client.getAttributes().getIntrospectionEncryptedResponseAlg());
        BlockEncryptionAlgorithm blockAlgorithm = BlockEncryptionAlgorithm.fromName(client.getAttributes().getIntrospectionEncryptedResponseEnc());

        if (keyAlgorithm != null && blockAlgorithm != null) {
            log.trace("Preparing encrypted introspection response with keyEncryptionAlgorithm {}, blockEncryptionAlgorithm: {}", keyAlgorithm, blockAlgorithm);
            Jwe jwe = new Jwe();

            // Header
            jwe.getHeader().setType(JwtType.JWT);
            jwe.getHeader().setAlgorithm(keyAlgorithm);
            jwe.getHeader().setEncryptionMethod(blockAlgorithm);

            // Claims
            fillPayload(jwe, response, grant);

            // nested signed jwt payload
            JwtSigner jwtSigner = newJwtSigner(client);

            Jwt jwt = jwtSigner.newJwt();
            jwt.setClaims(jwe.getClaims());
            jwe.setSignedJWTPayload(signJwt(jwt, client));

            if (keyAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP || keyAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
                JSONObject jsonWebKeys = CommonUtils.getJwks(client);
                String keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                        Algorithm.fromString(keyAlgorithm.getName()),
                        Use.ENCRYPTION, KeyOpsType.CONNECT);
                PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jsonWebKeys, null);
                jwe.getHeader().setKeyId(keyId);

                if (publicKey == null) {
                    throw new InvalidJweException("The public key is not valid");
                }

                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyAlgorithm, blockAlgorithm, publicKey);
                return jweEncrypter.encrypt(jwe).toString();
            }
            if (keyAlgorithm == KeyEncryptionAlgorithm.A128KW || keyAlgorithm == KeyEncryptionAlgorithm.A256KW) {
                byte[] sharedSymmetricKey = clientService.decryptSecret(client.getClientSecret()).getBytes(StandardCharsets.UTF_8);
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyAlgorithm, blockAlgorithm, sharedSymmetricKey);
                return jweEncrypter.encrypt(jwe).toString();
            }
        }

        log.trace("Preparing signed introspection response, client {}", client.getClientId());

        final JwtSigner jwtSigner = newJwtSigner(client);
        final Jwt jwt = jwtSigner.newJwt();
        fillPayload(jwt, response, grant);
        return jwtSigner.sign().toString();
    }

    private JwtSigner newJwtSigner(Client client) throws StringEncrypter.EncryptionException {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getAttributes().getIntrospectionSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getAttributes().getIntrospectionSignedResponseAlg());
        }

        return new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm, client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
    }

    private Jwt signJwt(Jwt jwt, Client client) throws Exception {
        JwtSigner jwtSigner = newJwtSigner(client);

        jwtSigner.setJwt(jwt);
        jwtSigner.sign();
        return jwt;
    }

    public void fillPayload(JsonWebResponse jwt, JSONObject response, AuthorizationGrant grant) throws InvalidJwtException {
        final Client client = grant.getClient();
        Audience.setAudience(jwt.getClaims(), client);
        jwt.getClaims().setIssuer(appConfiguration.getIssuer());
        jwt.getClaims().setIatNow();

        try {
            jwt.getClaims().setClaim("token_introspection", response);
        } catch (Exception e) {
            log.error("Failed to put claims into jwt. Key: token_introspection, response: " + response.toString(), e);
        }

        if (log.isTraceEnabled()) {
            log.trace("Response before signing: {}", jwt.getClaims().toJsonString());
        }
    }

    public String createResponseAsJwt(JSONObject response, AuthorizationGrant grant) throws Exception {
        return createResponseJwt(response, grant);
    }
}
