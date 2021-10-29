/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import com.google.common.collect.Lists;
import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.as.server.model.ldap.TokenType;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.model.token.IdTokenFactory;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.MetricService;
import io.jans.as.server.service.SectorIdentifierService;
import io.jans.as.server.service.external.ExternalIntrospectionService;
import io.jans.as.server.service.external.context.ExternalIntrospectionContext;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.model.metric.MetricType;
import io.jans.service.CacheService;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Base class for all the types of authorization grant.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2021
 */
public abstract class AuthorizationGrant extends AbstractAuthorizationGrant {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationGrant.class);

    @Inject
    private CacheService cacheService;

    @Inject
    private GrantService grantService;

    @Inject
    private IdTokenFactory idTokenFactory;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private ExternalIntrospectionService externalIntrospectionService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private SectorIdentifierService sectorIdentifierService;

    @Inject
    private MetricService metricService;

    @Inject
    private StatService statService;

    private boolean isCachedWithNoPersistence = false;

    protected AuthorizationGrant() {
    }

    protected AuthorizationGrant(User user, AuthorizationGrantType authorizationGrantType, Client client, Date authenticationTime) {
        super(user, authorizationGrantType, client, authenticationTime);
    }

    @Override
    public void init(User user, AuthorizationGrantType authorizationGrantType, Client client, Date authenticationTime) {
        super.init(user, authorizationGrantType, client, authenticationTime);
    }

    public IdToken createIdToken(
            IAuthorizationGrant grant, String nonce,
            AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse,
            Void> preProcessing, Function<JsonWebResponse, Void> postProcessing, String claims) throws Exception {
        JsonWebResponse jwr = idTokenFactory.createJwr(grant, nonce, authorizationCode, accessToken, refreshToken,
                state, scopes, includeIdTokenClaims, preProcessing, postProcessing, claims);
        final IdToken idToken = new IdToken(jwr.toString(), jwr.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT),
                jwr.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME));
        if (log.isTraceEnabled())
            log.trace("Created id_token: {}", idToken.getCode());
        return idToken;
    }

    @Override
    public String checkScopesPolicy(String scope) {
        if (StringUtils.isBlank(scope)) {
            return scope;
        }

        final String result = super.checkScopesPolicy(scope);
        save();
        return result;
    }

    @Override
    public void save() {
        if (isCachedWithNoPersistence) {
            if (getAuthorizationGrantType() == AuthorizationGrantType.AUTHORIZATION_CODE) {
                saveInCache();
            } else if (getAuthorizationGrantType() == AuthorizationGrantType.CIBA) {
                saveInCache();
            } else {
                throw new UnsupportedOperationException(
                        "Grant caching is not supported for : " + getAuthorizationGrantType());
            }
        } else {
            saveImpl();
        }
    }

    private void saveInCache() {
        CacheGrant cachedGrant = new CacheGrant(this, appConfiguration);
        cacheService.put(cachedGrant.getExpiresIn(), cachedGrant.cacheKey(), cachedGrant);
    }

    public boolean isImplicitFlow() {
        return getAuthorizationGrantType() == null || getAuthorizationGrantType() == AuthorizationGrantType.IMPLICIT;
    }

    private void saveImpl() {
        String grantId = getGrantId();
        if (StringUtils.isNotBlank(grantId)) {
            final List<TokenEntity> grants = grantService.getGrantsByGrantId(grantId);
            if (grants != null && !grants.isEmpty()) {
                for (TokenEntity t : grants) {
                    initTokenFromGrant(t);
                    log.debug("Saving grant: {}, code_challenge: {}", grantId, getCodeChallenge());
                    grantService.mergeSilently(t);
                }
            }
        }
    }

    private void initTokenFromGrant(TokenEntity token) {
        final String nonce = getNonce();
        if (nonce != null) {
            token.setNonce(nonce);
        }
        token.setScope(getScopesAsString());
        token.setAuthMode(getAcrValues());
        token.setSessionDn(getSessionDn());
        token.setAuthenticationTime(getAuthenticationTime());
        token.setCodeChallenge(getCodeChallenge());
        token.setCodeChallengeMethod(getCodeChallengeMethod());
        token.setClaims(getClaims());

        final JwtAuthorizationRequest jwtRequest = getJwtAuthorizationRequest();
        if (jwtRequest != null && StringUtils.isNotBlank(jwtRequest.getEncodedJwt())) {
            token.setJwtRequest(jwtRequest.getEncodedJwt());
        }
    }

    @Override
    public AccessToken createAccessToken(String dpop, String certAsPem, ExecutionContext context) {
        try {
            final AccessToken accessToken = super.createAccessToken(dpop, certAsPem, context);
            if (getClient().isAccessTokenAsJwt()) {
                accessToken.setCode(createAccessTokenAsJwt(accessToken, dpop, context));
            }
            if (accessToken.getExpiresIn() > 0) {
                persist(asToken(accessToken));
            }

            statService.reportAccessToken(getGrantType());
            metricService.incCounter(MetricType.TOKEN_ACCESS_TOKEN_COUNT);

            if (log.isTraceEnabled())
                log.trace("Created plain access token: {}", accessToken.getCode());

            return accessToken;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private String createAccessTokenAsJwt(AccessToken accessToken, String dpop, ExecutionContext context) throws Exception {
        final User user = getUser();
        final Client client = getClient();

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
                .fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getAccessTokenSigningAlg() != null
                && SignatureAlgorithm.fromString(client.getAccessTokenSigningAlg()) != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getAccessTokenSigningAlg());
        }

        final JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm,
                client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
        final Jwt jwt = jwtSigner.newJwt();
        jwt.getClaims().setClaim("scope", Lists.newArrayList(getScopes()));
        jwt.getClaims().setClaim("client_id", getClientId());
        jwt.getClaims().setClaim("username", user != null ? user.getAttribute("displayName") : null);
        jwt.getClaims().setClaim("token_type", accessToken.getTokenType().getName());
        jwt.getClaims().setClaim("code", accessToken.getCode()); // guarantee uniqueness : without it we can get race condition
        jwt.getClaims().setExpirationTime(accessToken.getExpirationDate());
        jwt.getClaims().setIssuedAt(accessToken.getCreationDate());
        jwt.getClaims().setSubjectIdentifier(getSub());
        jwt.getClaims().setClaim("x5t#S256", accessToken.getX5ts256());

        // DPoP
        if (StringUtils.isNotBlank(dpop)) {
            jwt.getClaims().setNotBefore(accessToken.getCreationDate());
            JSONObject cnf = new JSONObject();
            cnf.put("jkt", dpop);
            jwt.getClaims().setClaim("cnf", cnf);
        }

        Audience.setAudience(jwt.getClaims(), getClient());

        if (isTrue(client.getAttributes().getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims())) {
            runIntrospectionScriptAndInjectValuesIntoJwt(jwt, context);
        }

        final String accessTokenCode = jwtSigner.sign().toString();
        if (log.isTraceEnabled())
            log.trace("Created access token JWT: {}", accessTokenCode + ", claims: " + jwt.getClaims().toJsonString());

        return accessTokenCode;
    }

    private void runIntrospectionScriptAndInjectValuesIntoJwt(Jwt jwt, ExecutionContext executionContext) {
        JSONObject responseAsJsonObject = new JSONObject();

        ExternalIntrospectionContext context = new ExternalIntrospectionContext(this, executionContext.getHttpRequest(), executionContext.getHttpResponse(), appConfiguration, attributeService);
        context.setAccessTokenAsJwt(jwt);
        if (externalIntrospectionService.executeExternalModifyResponse(responseAsJsonObject, context)) {
            log.trace("Successfully run external introspection scripts.");

            if (context.isTranferIntrospectionPropertiesIntoJwtClaims()) {
                log.trace("Transfering claims into jwt ...");
                JwtUtil.transferIntoJwtClaims(responseAsJsonObject, jwt);
                log.trace("Transfered.");
            }
        }
    }

    private RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        try {
            if (refreshToken.getExpiresIn() > 0) {
                persist(asToken(refreshToken));
                statService.reportRefreshToken(getGrantType());
                metricService.incCounter(MetricType.TOKEN_REFRESH_TOKEN_COUNT);

                if (log.isTraceEnabled()) {
                    log.trace("Created refresh token: {}", refreshToken.getCode());
                }

                return refreshToken;
            }

            log.debug("Token expiration date is in the past. Skip refresh_token creation.");
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private RefreshToken saveRefreshToken(Supplier<RefreshToken> supplier) {
        try {
            return saveRefreshToken(supplier.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public RefreshToken createRefreshToken(String dpop) {
        return saveRefreshToken(() -> super.createRefreshToken(dpop));
    }

    @Override
    public RefreshToken createRefreshToken(String dpop, int lifetime) {
        return saveRefreshToken(() -> super.createRefreshToken(dpop, lifetime));
    }

    public RefreshToken createRefreshToken(String dpop, Date expirationDate) {
        return saveRefreshToken(() -> {
            RefreshToken refreshToken = new RefreshToken(HandleTokenFactory.generateHandleToken(), new Date(), expirationDate);
            refreshToken.setSessionDn(getSessionDn());
            refreshToken.setDpop(dpop);
            return refreshToken;
        });
    }

    @Override
    public IdToken createIdToken(
            String nonce, AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, AuthorizationGrant authorizationGrant, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing, Function<JsonWebResponse, Void> postProcessing) {
        try {
            final IdToken idToken = createIdToken(this, nonce, authorizationCode, accessToken, refreshToken,
                    state, getScopes(), includeIdTokenClaims, preProcessing, postProcessing, this.getClaims());
            final String acrValues = authorizationGrant.getAcrValues();
            final String sessionDn = authorizationGrant.getSessionDn();
            if (idToken.getExpiresIn() > 0) {
                final TokenEntity tokenEntity = asToken(idToken);
                tokenEntity.setAuthMode(acrValues);
                tokenEntity.setSessionDn(sessionDn);
                persist(tokenEntity);
            }

            setAcrValues(acrValues);
            setSessionDn(sessionDn);

            statService.reportIdToken(getGrantType());
            metricService.incCounter(MetricType.TOKEN_ID_TOKEN_COUNT);

            return idToken;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public void persist(TokenEntity token) {
        grantService.persist(token);
    }

    public void persist(AuthorizationCode code) {
        persist(asToken(code));
    }

    public TokenEntity asToken(IdToken token) {
        final TokenEntity result = asTokenEntity(token);
        result.setTokenTypeEnum(TokenType.ID_TOKEN);
        return result;
    }

    public TokenEntity asToken(RefreshToken token) {
        final TokenEntity result = asTokenEntity(token);
        result.setTokenTypeEnum(TokenType.REFRESH_TOKEN);
        return result;
    }

    public TokenEntity asToken(AuthorizationCode authorizationCode) {
        final TokenEntity result = asTokenEntity(authorizationCode);
        result.setTokenTypeEnum(TokenType.AUTHORIZATION_CODE);
        return result;
    }

    public TokenEntity asToken(AccessToken accessToken) {
        final TokenEntity result = asTokenEntity(accessToken);
        result.setTokenTypeEnum(TokenType.ACCESS_TOKEN);
        return result;
    }

    public String getScopesAsString() {
        final StringBuilder scopes = new StringBuilder();
        for (String s : getScopes()) {
            scopes.append(s).append(" ");
        }
        return scopes.toString().trim();
    }

    public TokenEntity asTokenEntity(AbstractToken token) {

        final TokenEntity result = new TokenEntity();
        final String hashedCode = TokenHashUtil.hash(token.getCode());

        result.setDn(grantService.buildDn(hashedCode));
        result.setGrantId(getGrantId());
        result.setCreationDate(token.getCreationDate());
        result.setExpirationDate(token.getExpirationDate());
        result.setTtl(token.getTtl());
        result.setTokenCode(hashedCode);
        result.setUserId(getUserId());
        result.setUserDn(getUserDn());
        result.setClientId(getClientId());

        result.getAttributes().setX5cs256(token.getX5ts256());

        result.setDpop(token.getDpop());

        final AuthorizationGrantType grantType = getAuthorizationGrantType();
        if (grantType != null) {
            result.setGrantType(grantType.getParamName());
        }

        final AuthorizationCode authorizationCode = getAuthorizationCode();
        if (authorizationCode != null) {
            result.setAuthorizationCode(TokenHashUtil.hash(authorizationCode.getCode()));
        }

        initTokenFromGrant(result);

        return result;
    }

    @Override
    public void revokeAllTokens() {
        final TokenEntity tokenEntity = getTokenEntity();
        if (tokenEntity != null && StringUtils.isNotBlank(tokenEntity.getGrantId())) {
            grantService.removeAllByGrantId(tokenEntity.getGrantId());
        }
    }

    @Override
    public void checkExpiredTokens() {
        // do nothing, clean up is made via grant service:
        // io.jans.as.server.service.GrantService.cleanUp()
    }

    public String getSub() {
        return sectorIdentifierService.getSub(this);
    }

    public boolean isCachedWithNoPersistence() {
        return isCachedWithNoPersistence;
    }

    public void setIsCachedWithNoPersistence(boolean isCachedWithNoPersistence) {
        this.isCachedWithNoPersistence = isCachedWithNoPersistence;
    }
}