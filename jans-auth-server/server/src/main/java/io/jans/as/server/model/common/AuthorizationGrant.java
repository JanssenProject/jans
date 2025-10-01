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
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.model.token.IdTokenFactory;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.MetricService;
import io.jans.as.server.service.SectorIdentifierService;
import io.jans.as.server.service.external.ExternalIntrospectionService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalIntrospectionContext;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.service.logout.LogoutStatusJwtService;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.as.server.service.token.StatusListService;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.model.metric.MetricType;
import io.jans.model.token.TokenEntity;
import io.jans.model.token.TokenType;
import io.jans.service.CacheService;
import io.jans.util.security.StringEncrypter;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
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
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private SectorIdentifierService sectorIdentifierService;

    @Inject
    private MetricService metricService;

    @Inject
    private StatService statService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private StatusListService statusListService;

    @Inject
    private StatusListIndexService statusListIndexService;

    @Inject
    private LogoutStatusJwtService logoutStatusJwtService;

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

    private IdToken createIdTokenInternal(AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken, ExecutionContext executionContext) throws Exception {
        executionContext.initFromGrantIfNeeded(this);

        Integer statusListIndex = null;
        if (errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)) {
            statusListIndex = statusListIndexService.next();
            executionContext.setStatusListIndex(statusListIndex);
        }

        JsonWebResponse jwr = idTokenFactory.createJwr(this, authorizationCode, accessToken, refreshToken, executionContext);
        final IdToken idToken = new IdToken(jwr.toString(), jwr.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT),
                jwr.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME));
        idToken.setReferenceId(executionContext.getTokenReferenceId());
        idToken.setStatusListIndex(statusListIndex);
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

        token.getAttributes().setAuthorizationDetails(getAuthzDetailsAsString());
        token.getAttributes().setAuthorizationChallenge(isAuthorizationChallenge());
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
    public LogoutStatusJwt createLogoutStatusJwt(ExecutionContext context) {
        try {
            final LogoutStatusJwt logoutStatusJwt = logoutStatusJwtService.createLogoutStatusJwt(context, this);
            if (logoutStatusJwt == null) {
                log.error("Failed to create Logout Status JWT.");
                return null;
            }

            final TokenEntity tokenEntity = asToken(logoutStatusJwt);
            context.setLogoutStatusJwtEntity(tokenEntity);

            persist(tokenEntity);
            statService.reportLogoutStatusJwt(getGrantType());
            metricService.incCounter(MetricType.TOKEN_LOGOUT_STATUS_JWT_COUNT);

            log.debug("Logout Status JWT is successfully persisted, jti: {}", logoutStatusJwt.getJti());
            return logoutStatusJwt;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public AccessToken createAccessToken(ExecutionContext context) {
        try {
            context.initFromGrantIfNeeded(this);
            context.generateRandomTokenReferenceId();

            final AccessToken accessToken = super.createAccessToken(context);

            Integer statusListIndex = null;
            if (errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)) {
                statusListIndex = statusListIndexService.next();
                context.setStatusListIndex(statusListIndex);
                accessToken.setStatusListIndex(statusListIndex);
            }

            if (accessToken.getExpiresIn() < 0) {
                log.trace("Failed to create access token with negative expiration time");
                return null;
            }

            JwtSigner jwtSigner = null;
            if (getClient().isAccessTokenAsJwt()) {
                jwtSigner = createAccessTokenAsJwt(accessToken, context);
            }

            boolean externalOk = externalUpdateTokenService.modifyAccessToken(accessToken, ExternalUpdateTokenContext.of(context, jwtSigner));
            if (!externalOk) {
                final String reason = "External UpdateToken script forbids access token creation.";
                log.trace(reason);

                throw new WebApplicationException(Response
                        .status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .cacheControl(ServerUtil.cacheControl(true, false))
                        .header("Pragma", "no-cache")
                        .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.ACCESS_DENIED, reason))
                        .build());
            }

            if (getClient().isAccessTokenAsJwt() && jwtSigner != null) {
                final String accessTokenCode = jwtSigner.sign().toString();
                if (log.isTraceEnabled())
                    log.trace("Created access token JWT: {}", accessTokenCode + ", claims: " + jwtSigner.getJwt().getClaims().toJsonString());

                accessToken.setCode(accessTokenCode);
            }

            final TokenEntity tokenEntity = asToken(accessToken);
            context.setAccessTokenEntity(tokenEntity);

            persist(tokenEntity);
            statService.reportAccessToken(getGrantType());
            metricService.incCounter(MetricType.TOKEN_ACCESS_TOKEN_COUNT);

            if (log.isTraceEnabled())
                log.trace("Created plain access token: {}", accessToken.getCode());

            return accessToken;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public JwtSigner createAccessTokenAsJwt(AccessToken accessToken, ExecutionContext context) throws StringEncrypter.EncryptionException, CryptoProviderException {
        final Client client = getClient();

        context.initFromGrantIfNeeded(this);

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
                .fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getAccessTokenSigningAlg() != null
                && SignatureAlgorithm.fromString(client.getAccessTokenSigningAlg()) != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getAccessTokenSigningAlg());
        }

        final JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm,
                client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
        final Jwt jwt = jwtSigner.newJwt();
        fillPayloadOfAccessTokenJwt(jwt.getClaims(), accessToken, context);

        Audience.setAudience(jwt.getClaims(), getClient());
        statusListService.addStatusClaimWithIndex(jwt, context);

        if (isTrue(client.getAttributes().getRunIntrospectionScriptBeforeJwtCreation())) {
            runIntrospectionScriptAndInjectValuesIntoJwt(jwt, context);
        }

        return jwtSigner;
    }

    public void fillPayloadOfAccessTokenJwt(JwtClaims claims, AccessToken accessToken, ExecutionContext context) {
        final User user = getUser();

        claims.setClaim("scope", Lists.newArrayList(getScopes()));
        claims.setClaim("client_id", getClientId());
        claims.setClaim("username", user != null ? user.getAttribute("displayName") : null);
        claims.setClaim("token_type", accessToken.getTokenType().getName());
        claims.setClaim("acr", getAcrValues());
        claims.setClaim("auth_time", ServerUtil.dateToSeconds(getAuthenticationTime()));
        claims.setExpirationTime(accessToken.getExpirationDate());
        claims.setIat(accessToken.getCreationDate());
        claims.setNbf(accessToken.getCreationDate());
        claims.setSubjectIdentifier(getSub());
        claims.setClaim("x5t#S256", accessToken.getX5ts256());
        // jti also guarantees uniqueness : without it we can get race condition
        claims.setClaim("jti", context.getTokenReferenceId());

        final AuthzDetails authzDetails = getAuthzDetails();
        if (!AuthzDetails.isEmpty(authzDetails)) {
            claims.setClaim("authorization_details", authzDetails.asJsonArray());
        }

        // DPoP
        final String dpop = context.getDpop();
        if (StringUtils.isNotBlank(dpop)) {
            claims.setNotBefore(accessToken.getCreationDate());
            JSONObject cnf = new JSONObject();
            cnf.put("jkt", dpop);
            claims.setClaim("cnf", cnf);
        }
    }

    private void runIntrospectionScriptAndInjectValuesIntoJwt(Jwt jwt, ExecutionContext executionContext) {
        executionContext.initFromGrantIfNeeded(this);

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

    private RefreshToken saveRefreshToken(RefreshToken refreshToken, ExecutionContext executionContext) {
        try {
            executionContext.initFromGrantIfNeeded(this);

            if (refreshToken.getExpiresIn() > 0) {
                final TokenEntity entity = asToken(refreshToken);
                executionContext.setRefreshTokenEntity(entity);

                boolean externalOk = externalUpdateTokenService.modifyRefreshToken(refreshToken, ExternalUpdateTokenContext.of(executionContext));
                if (!externalOk) {
                    log.trace("External script forbids refresh token creation.");
                    return null;
                }

                if (executionContext.getScopes().contains(ScopeConstants.ONLINE_ACCESS)) {
                    entity.getAttributes().setOnlineAccess(true);
                }

                persist(entity);
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

    private RefreshToken saveRefreshToken(Supplier<RefreshToken> supplier, ExecutionContext executionContext) {
        try {
            return saveRefreshToken(supplier.get(), executionContext);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public RefreshToken createRefreshToken(ExecutionContext context) {
        context.initFromGrantIfNeeded(this);
        return saveRefreshToken(() -> super.createRefreshToken(context), context);
    }

    @Override
    public RefreshToken createRefreshToken(ExecutionContext context, int lifetime) {
        context.initFromGrantIfNeeded(this);
        return saveRefreshToken(() -> super.createRefreshToken(context, lifetime), context);
    }

    public RefreshToken createRefreshToken(ExecutionContext context, Date expirationDate) {
        return saveRefreshToken(() -> {
            RefreshToken refreshToken = new RefreshToken(HandleTokenFactory.generateHandleToken(), new Date(), expirationDate);
            refreshToken.setSessionDn(getSessionDn());
            refreshToken.setDpop(context.getDpop());
            return refreshToken;
        }, context);
    }

    @Override
    public IdToken createIdToken(
            String nonce, AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, ExecutionContext executionContext) {
        try {
            executionContext.initFromGrantIfNeeded(this);
            executionContext.setScopes(getScopes());
            executionContext.setClaimsAsString(getClaims());
            executionContext.setNonce(nonce);
            executionContext.setState(state);
            executionContext.generateRandomTokenReferenceId();

            final IdToken idToken = createIdTokenInternal(authorizationCode, accessToken, refreshToken, executionContext);
            final AuthorizationGrant grant = executionContext.getGrant();
            final String acrValues = grant.getAcrValues();
            final String sessionDn = grant.getSessionDn();
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
        } catch (WebApplicationException e) {
            throw e;
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

    public TokenEntity asToken(LogoutStatusJwt logoutStatusJwt) {
        final TokenEntity result = asTokenEntity(logoutStatusJwt);
        result.setTokenTypeEnum(TokenType.LOGOUT_STATUS_JWT);
        return result;
    }

    public TokenEntity asToken(TxToken txToken) {
        final TokenEntity result = asTokenEntity(txToken);
        result.setTokenTypeEnum(TokenType.TX_TOKEN);
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
        result.setReferenceId(token.getReferenceId());
        result.setJti(token.getJti());

        result.getAttributes().setStatusListIndex(token.getStatusListIndex());
        result.getAttributes().setX5cs256(token.getX5ts256());
        result.getAttributes().setDpopJkt(getDpopJkt());

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
        final String sub = sectorIdentifierService.getSub(this);
        if (StringUtils.isBlank(sub)) {
            final String clientId = getClientId();
            return StringUtils.isNotBlank(clientId) ? clientId : "";
        }
        return sub;
    }

    public boolean isCachedWithNoPersistence() {
        return isCachedWithNoPersistence;
    }

    public void setIsCachedWithNoPersistence(boolean isCachedWithNoPersistence) {
        this.isCachedWithNoPersistence = isCachedWithNoPersistence;
    }
}