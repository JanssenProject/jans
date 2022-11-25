package io.jans.as.server.authorize.ws.rs;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.Claim;
import io.jans.as.server.model.authorize.IdTokenMember;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.par.ws.rs.ParService;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.jans.as.model.util.StringUtils.implode;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class AuthzRequestService {

    public static final String INVALID_JWT_AUTHORIZATION_REQUEST = "Invalid JWT authorization request";
    private static final long ACR_TO_LEVEL_CACHE_LIFETIME_IN_MINUTES = 15;
    private static final String ACR_TO_LEVEL_KEY = "ACR_TO_LEVEL_KEY";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Inject
    private ParService parService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private RedirectionUriService redirectionUriService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    private final Cache<String, Map<String, Integer>> acrToLevelCache = CacheBuilder.newBuilder()
            .expireAfterWrite(ACR_TO_LEVEL_CACHE_LIFETIME_IN_MINUTES, TimeUnit.MINUTES).build();

    public Map<String, Integer> getAcrToLevelMap() {
        Map<String, Integer> map = acrToLevelCache.getIfPresent(ACR_TO_LEVEL_KEY);
        if (map != null) {
            return map;
        }
        map = externalAuthenticationService.acrToLevelMapping();
        acrToLevelCache.put(ACR_TO_LEVEL_KEY, map);
        return map;
    }

    public void addDeviceSecretToSession(AuthzRequest authzRequest, SessionId sessionId) {
        if (BooleanUtils.isFalse(appConfiguration.getReturnDeviceSecretFromAuthzEndpoint())) {
            return;
        }
        if (!Arrays.asList(authzRequest.getScope().split(" ")).contains(ScopeConstants.DEVICE_SSO)) {
            return;
        }
        if (!ArrayUtils.contains(authzRequest.getClient().getGrantTypes(), GrantType.TOKEN_EXCHANGE)) {
            log.debug("Skip device secret. Scope has {} value but client does not have Token Exchange Grant Type enabled ('urn:ietf:params:oauth:grant-type:token-exchange')", ScopeConstants.DEVICE_SSO);
            return;
        }

        final String newDeviceSecret = HandleTokenFactory.generateDeviceSecret();

        final List<String> deviceSecrets = sessionId.getDeviceSecrets();
        deviceSecrets.add(newDeviceSecret);

        authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.DEVICE_SECRET, newDeviceSecret);
    }


    public boolean processPar(AuthzRequest authzRequest) {
        boolean isPar = Util.isPar(authzRequest.getRequestUri());
        if (!isPar && isTrue(appConfiguration.getRequirePar())) {
            log.debug("Server configured for PAR only (via requirePar conf property). Failed to find PAR by request_uri (id): {}", authzRequest.getRequestUri());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), "Failed to find par by request_uri"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        if (!isPar) {
            return false;
        }

        final Par par = parService.getParAndValidateForAuthorizationRequest(authzRequest.getRequestUri(), authzRequest.getState(), authzRequest.getClientId());

        authzRequest.setRequestUri(null); // set it to null, we don't want to follow request uri for PAR
        authzRequest.setRequest(null); // request is validated and parameters parsed by PAR endpoint before PAR persistence

        log.debug("Setting request parameters from PAR - {}", par);

        authzRequest.setResponseType(par.getAttributes().getResponseType());
        authzRequest.setResponseMode(par.getAttributes().getResponseMode());
        authzRequest.setScope(par.getAttributes().getScope());
        authzRequest.setPrompt(par.getAttributes().getPrompt());
        authzRequest.setRedirectUri(par.getAttributes().getRedirectUri());
        authzRequest.setAcrValues(par.getAttributes().getAcrValuesStr());
        authzRequest.setAmrValues(par.getAttributes().getAmrValuesStr());
        authzRequest.setCodeChallenge(par.getAttributes().getCodeChallenge());
        authzRequest.setCodeChallengeMethod(par.getAttributes().getCodeChallengeMethod());

        authzRequest.setState(StringUtils.isNotBlank(par.getAttributes().getState()) ? par.getAttributes().getState() : "");

        if (StringUtils.isNotBlank(par.getAttributes().getNonce()))
            authzRequest.setNonce(par.getAttributes().getNonce());
        if (StringUtils.isNotBlank(par.getAttributes().getSessionId()))
            authzRequest.setSessionId(par.getAttributes().getSessionId());
        if (StringUtils.isNotBlank(par.getAttributes().getCustomResponseHeaders()))
            authzRequest.setCustomResponseHeaders(par.getAttributes().getCustomResponseHeaders());
        if (StringUtils.isNotBlank(par.getAttributes().getClaims()))
            authzRequest.setClaims(par.getAttributes().getClaims());
        if (StringUtils.isNotBlank(par.getAttributes().getOriginHeaders()))
            authzRequest.setOriginHeaders(par.getAttributes().getOriginHeaders());
        if (StringUtils.isNotBlank(par.getAttributes().getUiLocales()))
            authzRequest.setUiLocales(par.getAttributes().getUiLocales());
        if (!par.getAttributes().getCustomParameters().isEmpty()) {
            if (authzRequest.getCustomParameters() == null) {
                authzRequest.setCustomParameters(new HashMap<>());
            }
            authzRequest.getCustomParameters().putAll(par.getAttributes().getCustomParameters());
        }

        return isPar;
    }

    @SuppressWarnings("java:S3776")
    public void processRequestObject(AuthzRequest authzRequest, Client client, Set<String> scopes, User user, List<Prompt> prompts) {
        final RedirectUriResponse redirectUriResponse = authzRequest.getRedirectUriResponse();

        JwtAuthorizationRequest jwtRequest = null;
        if (StringUtils.isNotBlank(authzRequest.getRequest()) || StringUtils.isNotBlank(authzRequest.getRequestUri())) {
            try {
                jwtRequest = JwtAuthorizationRequest.createJwtRequest(authzRequest.getRequest(), authzRequest.getRequestUri(), client, redirectUriResponse, cryptoProvider, appConfiguration);

                if (jwtRequest == null) {
                    throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Failed to parse jwt.");
                }
                authzRequest.setJwtRequest(jwtRequest);

                if (StringUtils.isNotBlank(jwtRequest.getState())) {
                    authzRequest.setState(jwtRequest.getState());
                    redirectUriResponse.setState(authzRequest.getState());
                }
                if (appConfiguration.isFapi() && StringUtils.isBlank(jwtRequest.getState())) {
                    authzRequest.setState(""); // #1250 - FAPI : discard state if in JWT we don't have state
                    redirectUriResponse.setState("");
                }

                if (jwtRequest.getRedirectUri() != null) {
                    if (!jwtRequest.getRedirectUri().equals(authzRequest.getRedirectUri())) {
                        log.error("The redirect_uri parameter in url is not the same as in the JWT");
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The redirect_uri parameter in url is not the same as in the JWT");
                    }
                    if (StringUtils.isBlank(redirectionUriService.validateRedirectionUri(client, jwtRequest.getRedirectUri()))) {
                        log.error("redirect_uri in request object is not valid.");
                        throw new WebApplicationException(Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST_REDIRECT_URI, authzRequest.getState(), ""))
                                .build());
                    }
                    redirectUriResponse.getRedirectUri().setBaseRedirectUri(jwtRequest.getRedirectUri());
                }

                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(jwtRequest.getAlgorithm());
                if (Boolean.TRUE.equals(appConfiguration.getForceSignedRequestObject()) && signatureAlgorithm == SignatureAlgorithm.NONE) {
                    throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "A signed request object is required");
                }

                // JWT wins
                if (!jwtRequest.getScopes().isEmpty()) {
                    if (!scopes.contains("openid")) { // spec: Even if a scope parameter is present in the Request Object value, a scope parameter MUST always be passed using the OAuth 2.0 request syntax containing the openid scope value
                        throw new WebApplicationException(Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_SCOPE, authzRequest.getState(), "scope parameter does not contain openid value which is required."))
                                .build());
                    }

                    scopes.clear();
                    scopes.addAll(scopeChecker.checkScopesPolicy(client, Lists.newArrayList(jwtRequest.getScopes())));
                }
                if (StringUtils.isNotBlank(jwtRequest.getNonce())) {
                    authzRequest.setNonce(jwtRequest.getNonce());
                }
                if (StringUtils.isNotBlank(jwtRequest.getCodeChallenge())) {
                    authzRequest.setCodeChallenge(jwtRequest.getCodeChallenge());
                }
                if (StringUtils.isNotBlank(jwtRequest.getCodeChallengeMethod())) {
                    authzRequest.setCodeChallengeMethod(jwtRequest.getCodeChallengeMethod());
                }
                if (jwtRequest.getDisplay() != null && StringUtils.isNotBlank(jwtRequest.getDisplay().getParamName())) {
                    authzRequest.setDisplay(jwtRequest.getDisplay().getParamName());
                }
                if (!jwtRequest.getPrompts().isEmpty()) {
                    prompts.clear();
                    prompts.addAll(Lists.newArrayList(jwtRequest.getPrompts()));
                    authzRequest.setPrompt(implode(prompts, " "));
                    authzRequest.setPromptFromJwt(true);
                }
                if (jwtRequest.getResponseMode() != null) {
                    authzRequest.setResponseMode(jwtRequest.getResponseMode().getValue());
                    redirectUriResponse.getRedirectUri().setResponseMode(jwtRequest.getResponseMode());
                }

                checkIdTokenMember(authzRequest, redirectUriResponse, user, jwtRequest);
                requestParameterService.getCustomParameters(jwtRequest, authzRequest.getCustomParameters());
            } catch (WebApplicationException e) {
                JsonWebResponse jwr = parseRequestToJwr(authzRequest.getRequest());
                if (jwr != null) {
                    String checkForAlg = jwr.getClaims().getClaimAsString("alg"); // to handle Jans Issue#310
                    if ("none".equals(checkForAlg)) {
                        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                .entity(errorResponseFactory.getErrorAsJson(
                                        AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, "",
                                        "The None algorithm in nested JWT is not allowed for FAPI"))
                                .type(MediaType.APPLICATION_JSON_TYPE).build());
                    }
                    ResponseMode responseMode = ResponseMode.getByValue(jwr.getClaims().getClaimAsString("response_mode"));
                    if (responseMode == ResponseMode.JWT) {
                        authzRequest.setResponseMode(responseMode.getValue());
                        redirectUriResponse.getRedirectUri().setResponseMode(ResponseMode.JWT);
                        fillRedirectUriResponseforJARM(redirectUriResponse, jwr, client);
                        if (appConfiguration.isFapi()) {
                            authorizeRestWebServiceValidator.throwInvalidJwtRequestExceptionAsJwtMode(
                                    redirectUriResponse, INVALID_JWT_AUTHORIZATION_REQUEST,
                                    jwr.getClaims().getClaimAsString("state"), authzRequest.getHttpRequest());
                        }
                    }
                }
                throw e;
            } catch (Exception e) {
                log.error("Invalid JWT authorization request. Message : " + e.getMessage(), e);
                throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, INVALID_JWT_AUTHORIZATION_REQUEST);
            }
        }

        // JARM
        Set<ResponseMode> jwtResponseModes = Sets.newHashSet(ResponseMode.QUERY_JWT, ResponseMode.FRAGMENT_JWT, ResponseMode.JWT, ResponseMode.FORM_POST_JWT);
        if (jwtResponseModes.contains(authzRequest.getResponseModeEnum())) {
            JsonWebResponse jwe = parseRequestToJwr(authzRequest.getRequest());
            fillRedirectUriResponseforJARM(redirectUriResponse, jwe, client);
        }
        // Validate JWT request object after JARM check, because we want to return errors well formatted (JSON/JWT).
        if (jwtRequest != null) {
            authorizeRestWebServiceValidator.validateJwtRequest(authzRequest.getClientId(), authzRequest.getState(), authzRequest.getHttpRequest(), authzRequest.getResponseTypeList(), redirectUriResponse, jwtRequest);
        }
    }

    public void handleJwr(AuthzRequest authzRequest, Client client, RedirectUriResponse redirectUriResponse, JsonWebResponse jwr) {
        if (jwr == null) {
            return;
        }

        String checkForAlg = jwr.getClaims().getClaimAsString("alg"); // to handle Jans Issue#310
        if ("none".equals(checkForAlg)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(
                            AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, "",
                            "The None algorithm in nested JWT is not allowed for FAPI"))
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
        ResponseMode responseMode = ResponseMode.getByValue(jwr.getClaims().getClaimAsString("response_mode"));
        if (responseMode == ResponseMode.JWT) {
            authzRequest.setResponseMode(responseMode.getValue());
            redirectUriResponse.getRedirectUri().setResponseMode(ResponseMode.JWT);
            fillRedirectUriResponseforJARM(redirectUriResponse, jwr, client);
            if (appConfiguration.isFapi()) {
                authorizeRestWebServiceValidator.throwInvalidJwtRequestExceptionAsJwtMode(
                        redirectUriResponse, INVALID_JWT_AUTHORIZATION_REQUEST,
                        jwr.getClaims().getClaimAsString("state"), authzRequest.getHttpRequest());
            }
        }
    }

    public void checkIdTokenMember(AuthzRequest authzRequest, RedirectUriResponse redirectUriResponse, User user, JwtAuthorizationRequest jwtRequest) {
        final IdTokenMember idTokenMember = jwtRequest.getIdTokenMember();
        if (idTokenMember == null) {
            return;
        }

        if (idTokenMember.getMaxAge() != null) {
            authzRequest.setMaxAge(idTokenMember.getMaxAge());
        }
        final Claim acrClaim = idTokenMember.getClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
        if (acrClaim != null && acrClaim.getClaimValue() != null) {
            authzRequest.setAcrValues(acrClaim.getClaimValue().getValueAsString());
        }

        Claim userIdClaim = idTokenMember.getClaim(JwtClaimName.SUBJECT_IDENTIFIER);
        if (userIdClaim != null && userIdClaim.getClaimValue() != null
                && userIdClaim.getClaimValue().getValue() != null) {
            String userIdClaimValue = userIdClaim.getClaimValue().getValue();

            if (user != null) {
                String userId = user.getUserId();

                if (!userId.equalsIgnoreCase(userIdClaimValue)) {
                    throw new WebApplicationException(redirectUriResponse.createErrorBuilder(AuthorizeErrorResponseType.USER_MISMATCHED).build());
                }
            }
        }

    }

    @Nullable
    public JsonWebResponse parseRequestToJwr(String request) {
        if (request == null) {
            return null;
        }
        String[] parts = request.split("\\.");
        try {
            if (parts.length == 5) {
                String encodedHeader = parts[0];
                JwtHeader jwtHeader = new JwtHeader(encodedHeader);
                String keyId = jwtHeader.getKeyId();
                PrivateKey privateKey = null;
                KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm
                        .fromName(jwtHeader.getClaimAsString(JwtHeaderName.ALGORITHM));
                if (AlgorithmFamily.RSA.equals(keyEncryptionAlgorithm.getFamily())) {
                    privateKey = cryptoProvider.getPrivateKey(keyId);
                }
                return Jwe.parse(request, privateKey, null);
            }
            return Jwt.parseSilently(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public void fillRedirectUriResponseforJARM(RedirectUriResponse redirectUriResponse, JsonWebResponse jwr, Client client) {
        try {
            if (jwr != null) {
                String tempRedirectUri = jwr.getClaims().getClaimAsString("redirect_uri");
                if (StringUtils.isNotBlank(tempRedirectUri)) {
                    redirectUriResponse.getRedirectUri().setBaseRedirectUri(URLDecoder.decode(tempRedirectUri, "UTF-8"));
                }
            }
            String clientId = client.getClientId();
            redirectUriResponse.getRedirectUri().setIssuer(appConfiguration.getIssuer());
            redirectUriResponse.getRedirectUri().setAudience(clientId);
            redirectUriResponse.getRedirectUri().setAuthorizationCodeLifetime(appConfiguration.getAuthorizationCodeLifetime());
            redirectUriResponse.getRedirectUri().setSignatureAlgorithm(SignatureAlgorithm.fromString(client.getAttributes().getAuthorizationSignedResponseAlg()));
            redirectUriResponse.getRedirectUri().setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.fromName(client.getAttributes().getAuthorizationEncryptedResponseAlg()));
            redirectUriResponse.getRedirectUri().setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.fromName(client.getAttributes().getAuthorizationEncryptedResponseEnc()));
            redirectUriResponse.getRedirectUri().setCryptoProvider(cryptoProvider);

            String keyId = null;
            if (client.getAttributes().getAuthorizationEncryptedResponseAlg() != null
                    && client.getAttributes().getAuthorizationEncryptedResponseEnc() != null) {
                if (client.getAttributes().getAuthorizationSignedResponseAlg() != null) { // Signed then Encrypted
                    // response
                    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
                            .fromString(client.getAttributes().getAuthorizationSignedResponseAlg());

                    String nestedKeyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
                            Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);

                    JSONObject jsonWebKeys = CommonUtils.getJwks(client);
                    redirectUriResponse.getRedirectUri().setNestedJsonWebKeys(jsonWebKeys);

                    String clientSecret = clientService.decryptSecret(client.getClientSecret());
                    redirectUriResponse.getRedirectUri().setNestedSharedSecret(clientSecret);
                    redirectUriResponse.getRedirectUri().setNestedKeyId(nestedKeyId);
                }

                // Encrypted response
                JSONObject jsonWebKeys = CommonUtils.getJwks(client);
                if (jsonWebKeys != null) {
                    keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                            Algorithm.fromString(client.getAttributes().getAuthorizationEncryptedResponseAlg()),
                            Use.ENCRYPTION);
                }
                String sharedSecret = clientService.decryptSecret(client.getClientSecret());
                byte[] sharedSymmetricKey = sharedSecret.getBytes(StandardCharsets.UTF_8);
                redirectUriResponse.getRedirectUri().setSharedSymmetricKey(sharedSymmetricKey);
                redirectUriResponse.getRedirectUri().setJsonWebKeys(jsonWebKeys);
                redirectUriResponse.getRedirectUri().setKeyId(keyId);
            } else { // Signed response
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
                if (client.getAttributes().getAuthorizationSignedResponseAlg() != null) {
                    signatureAlgorithm = SignatureAlgorithm
                            .fromString(client.getAttributes().getAuthorizationSignedResponseAlg());
                }

                keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
                        Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);

                JSONObject jsonWebKeys = CommonUtils.getJwks(client);
                redirectUriResponse.getRedirectUri().setJsonWebKeys(jsonWebKeys);

                String clientSecret = clientService.decryptSecret(client.getClientSecret());
                redirectUriResponse.getRedirectUri().setSharedSecret(clientSecret);
                redirectUriResponse.getRedirectUri().setKeyId(keyId);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void setAcrsIfNeeded(AuthzRequest authzRequest) {
        Client client = authzRequest.getClient();

        // explicitly set acrs via getDefaultAcrValues()
        if (StringUtils.isBlank(authzRequest.getAcrValues())) {
            if (!ArrayUtils.isEmpty(client.getDefaultAcrValues())) {
                authzRequest.setAcrValues(implode(client.getDefaultAcrValues(), " "));
            }
            return;
        }

        final int currentMinAcrLevel = getCurrentMinAcrLevel(authzRequest);
        if (currentMinAcrLevel >= client.getAttributes().getMinimumAcrLevel()) {
            return; // do nothing -> current level is enough
        }

        if (BooleanUtils.isNotTrue(client.getAttributes().getMinimumAcrLevelAutoresolve())) {
            log.error("Current acr level is less then minimum required. currentMinAcrLevel: {}, clientMinAcrLevel: {}", currentMinAcrLevel, client.getAttributes().getMinimumAcrLevel());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), "Current acr level is less then minimum required by client"))
                    .build());
        }

        final Map<String, Integer> acrToLevelMap = getAcrToLevelMap();
        if (client.getAttributes().getMinimumAcrPriorityList().isEmpty()) { // no priority list -> pick up next higher then minimum
            for (Map.Entry<String, Integer> entry : acrToLevelMap.entrySet()) {
                if (currentMinAcrLevel < entry.getValue()) {
                    authzRequest.setAcrValues(entry.getKey());
                    return;
                }
            }
        }

        for (String acr : client.getAttributes().getMinimumAcrPriorityList()) {
            final Integer acrLevel = acrToLevelMap.get(acr);
            if (acrLevel != null && acrLevel >= currentMinAcrLevel) {
                authzRequest.setAcrValues(acr);
                return;
            }
        }

        log.error("Current acr level is less then minimum required by client. currentMinAcrLevel: {}, clientAttributes: {}", currentMinAcrLevel, client.getAttributes());
        throw new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), "Current acr level is less then minimum required by client:" + client.getClientId()))
                .build());
    }

    public int getCurrentMinAcrLevel(AuthzRequest authzRequest) {
        if (StringUtils.isBlank(authzRequest.getAcrValues())) {
            return -1;
        }

        Integer currentLevel = null;
        final Map<String, Integer> acrToLevelMap = getAcrToLevelMap();
        for (String acr : authzRequest.getAcrValuesList()) {
            Integer level = acrToLevelMap.get(acr);
            if (currentLevel == null) {
                currentLevel = level;
                continue;
            }
            if (level != null && level < currentLevel) {
                currentLevel = level;
            }
        }
        return currentLevel != null ? currentLevel : -1;
    }

    public void createRedirectUriResponse(AuthzRequest authzRequest) {
        RedirectUriResponse redirectUriResponse = new RedirectUriResponse(new RedirectUri(authzRequest.getRedirectUri(), authzRequest.getResponseTypeList(), authzRequest.getResponseModeEnum()), authzRequest.getState(), authzRequest.getHttpRequest(), errorResponseFactory);
        redirectUriResponse.setFapiCompatible(appConfiguration.isFapi());

        authzRequest.setRedirectUriResponse(redirectUriResponse);
    }

    public void createOauth2AuditLog(AuthzRequest authzRequest) {
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(authzRequest.getHttpRequest()), Action.USER_AUTHORIZATION);
        oAuth2AuditLog.setClientId(authzRequest.getClientId());
        oAuth2AuditLog.setScope(authzRequest.getScope());

        authzRequest.setAuditLog(oAuth2AuditLog);
    }
}
