package io.jans.as.server.token.ws.rs;

import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.common.ExchangeTokenType;
import io.jans.as.model.common.SubjectTokenType;
import io.jans.as.model.common.TokenType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJweException;
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
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.token.TokenRequestParam;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.*;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ServerCryptoProvider;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.token.TokenEntity;
import io.jans.util.security.StringEncrypter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TxTokenService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private TxTokenValidator txTokenValidator;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ClientService clientService;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    public Response processTxToken(ExecutionContext executionContext) throws Exception {
        final JSONObject responseJson = process(executionContext);
        final String entity = responseJson.toString();
        log.trace("Created TxToken: {}", entity);
        return response(Response.ok().entity(entity), executionContext.getAuditLog());
    }

    private JSONObject process(ExecutionContext executionContext) throws Exception {
        final String requestedTokenType = executionContext.getHttpRequest().getParameter("requested_token_type");
        final String subjectToken = executionContext.getHttpRequest().getParameter("subject_token");
        final String subjectTokenType = executionContext.getHttpRequest().getParameter("subject_token_type");

        txTokenValidator.validateRequestedTokenType(requestedTokenType, executionContext.getAuditLog());
        SubjectTokenType subjectTokenTypeEnum = txTokenValidator.validateSubjectTokenType(subjectTokenType, executionContext.getAuditLog());
        AuthorizationGrant subjectGrant = txTokenValidator.validateSubjectToken(subjectToken, subjectTokenTypeEnum, executionContext.getAuditLog());

        TxToken txToken = createTxToken(executionContext, subjectGrant);

        return createResponse(txToken.getCode());
    }

    public static JSONObject createResponse(String txToken) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("issued_token_type", ExchangeTokenType.TX_TOKEN.getName());
        responseJson.put("token_type", TokenType.N_A.getName());
        responseJson.put("access_token", txToken);
        return responseJson;
    }

    private TxToken createTxToken(ExecutionContext executionContext, AuthorizationGrant subjectGrant) throws Exception {
        final String audience = executionContext.getHttpRequest().getParameter(TokenRequestParam.AUDIENCE);
        final String requestContext = executionContext.getHttpRequest().getParameter(TokenRequestParam.REQUEST_CONTEXT);
        final String requestDetails = executionContext.getHttpRequest().getParameter(TokenRequestParam.REQUEST_DETAILS);
        final String scope = executionContext.getHttpRequest().getParameter(TokenRequestParam.SCOPE);

        final Client client = executionContext.getClient();

        TokenExchangeGrant txTokenGrant = authorizationGrantList.createTokenExchangeGrant(new User(), client);
        txTokenGrant.checkScopesPolicy(scope);

        executionContext.setGrant(txTokenGrant);

        final JsonWebResponse jwr = createTxTokenJwr(audience, requestContext, requestDetails, executionContext, subjectGrant);
        final String jwrString = jwr.toString();

        final int txTokenLifetime = getTxTokenLifetime(client);
        TxToken txToken = new TxToken(txTokenLifetime);
        txToken.setCode(jwrString);

        final TokenEntity tokenEntity = txTokenGrant.asToken(txToken);
        txTokenGrant.persist(tokenEntity);
        return txToken;
    }

    private void fillPayload(JsonWebResponse jwr, String audience, String requestContext, String requestDetails, ExecutionContext executionContext, AuthorizationGrant authorizationGrant) {
        Client client = executionContext.getClient();

        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, getTxTokenLifetime(client));
        Date expiration = calendar.getTime();


        jwr.getClaims().setIssuer(appConfiguration.getIssuer());
        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIat(issuedAt);
        jwr.getClaims().setNbf(issuedAt);
        jwr.setClaim("txn", UUID.randomUUID().toString());
        jwr.setClaim("sub", UUID.randomUUID().toString());
        jwr.setClaim("purp", JwtType.TX_TOKEN.toString());

        Audience.setAudience(jwr.getClaims(), client);
        if (StringUtils.isNotBlank(audience)) {
            jwr.getClaims().addAudience(audience);
        }

        JSONObject requestContextObj = decodeJson(requestContext);
        if (requestContextObj != null) {
            jwr.getClaims().setClaim("rctx", requestContextObj);
        }

        if (authorizationGrant != null) {
            jwr.setClaim("sub", authorizationGrant.getSub());
        }

        JSONObject azd = decodeJson(requestDetails);
        if (azd == null) {
            azd = new JSONObject();
        }
        azd.put("client_id", client.getClientId());

        jwr.getClaims().setClaim("azd", azd);
    }

    private static JSONObject decodeJson(String jsonString) {
        if (StringUtils.isBlank(jsonString)) {
            return null;
        }
        try {
           return new JSONObject(jsonString);
        } catch (JSONException e) {
            String decoded = Base64Util.base64urldecodeToString(jsonString);
            return new JSONObject(decoded);
        }
    }

    private int getTxTokenLifetime(Client client) {
        if (client.getAttributes().getTxTokenLifetime() != null && client.getAttributes().getTxTokenLifetime() > 0) {
            log.trace("Override TxToken lifetime with value {} from client: {}", client.getAttributes().getTxTokenLifetime(), client.getClientId());
            return client.getAttributes().getTxTokenLifetime();
        }
        return appConfiguration.getTxTokenLifetime();
    }

    private Jwt signJwt(Jwt jwt, Client client) throws Exception {
        JwtSigner jwtSigner = newJwtSigner(client);

        jwtSigner.setJwt(jwt);
        jwtSigner.sign();
        return jwt;
    }

    private JwtSigner newJwtSigner(Client client) throws StringEncrypter.EncryptionException {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getAttributes().getTxTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getAttributes().getTxTokenSignedResponseAlg());
        }

        return new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm, client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    public boolean isTxTokenFlow(HttpServletRequest httpRequest) {
        return isTxTokenFlow(httpRequest.getParameter("requested_token_type"));
    }

    public static boolean isTxTokenFlow(String requestedTokenType) {
        final ExchangeTokenType exchangeTokenType = ExchangeTokenType.fromString(requestedTokenType);
        return exchangeTokenType == ExchangeTokenType.TX_TOKEN;
    }

    public Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

    private JsonWebResponse createTxTokenJwr(String audience, String requestContext, String requestDetails,
                                             ExecutionContext executionContext, AuthorizationGrant authorizationGrant) throws Exception {
        final Client client = executionContext.getClient();

        KeyEncryptionAlgorithm keyAlgorithm = KeyEncryptionAlgorithm.fromName(client.getAttributes().getTxTokenEncryptedResponseAlg());
        BlockEncryptionAlgorithm blockAlgorithm = BlockEncryptionAlgorithm.fromName(client.getAttributes().getTxTokenEncryptedResponseEnc());

        if (keyAlgorithm != null && blockAlgorithm != null) {
            log.trace("Preparing encrypted TxToken with keyEncryptionAlgorithm {}, blockEncryptionAlgorithm: {}", keyAlgorithm, blockAlgorithm);
            Jwe jwe = new Jwe();

            // Header
            jwe.getHeader().setType(JwtType.TX_TOKEN);
            jwe.getHeader().setAlgorithm(keyAlgorithm);
            jwe.getHeader().setEncryptionMethod(blockAlgorithm);

            // Claims
            fillPayload(jwe, audience, requestContext, requestDetails, executionContext, authorizationGrant);

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
                return jweEncrypter.encrypt(jwe);
            }
            if (keyAlgorithm == KeyEncryptionAlgorithm.A128KW || keyAlgorithm == KeyEncryptionAlgorithm.A256KW) {
                byte[] sharedSymmetricKey = clientService.decryptSecret(client.getClientSecret()).getBytes(StandardCharsets.UTF_8);
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyAlgorithm, blockAlgorithm, sharedSymmetricKey);
                return jweEncrypter.encrypt(jwe);
            }
        }

        log.trace("Preparing signed TxToken, client {}", client.getClientId());

        final JwtSigner jwtSigner = newJwtSigner(client);
        final Jwt jwt = jwtSigner.newJwt();
        jwt.getHeader().setType(JwtType.TX_TOKEN); // override value which is set in jwt signer
        fillPayload(jwt, audience, requestContext, requestDetails, executionContext, authorizationGrant);
        return jwtSigner.sign();
    }
}
