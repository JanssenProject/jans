package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetTokensByCodeParams;
import io.jans.ca.common.response.GetTokensByCodeResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.*;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import org.python.jline.internal.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetTokensByCodeOperation extends BaseOperation<GetTokensByCodeParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetTokensByCodeOperation.class);
    private StateService stateService;
    private DiscoveryService discoveryService;
    private RpService rpService;
    private KeyGeneratorService keyGeneratorService;
    private PublicOpKeyService publicOpKeyService;
    private MainPersistenceService jansConfigurationService;
    private OpClientFactoryImpl opClientFactory;
    private HttpService httpService;

    public GetTokensByCodeOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetTokensByCodeParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
        this.stateService = serviceProvider.getStateService();
        this.rpService = serviceProvider.getRpService();
        this.keyGeneratorService = serviceProvider.getKeyGeneratorService();
        this.httpService = discoveryService.getHttpService();
        this.opClientFactory = discoveryService.getOpClientFactory();
        this.jansConfigurationService = stateService.getConfigurationService();
        this.publicOpKeyService = serviceProvider.getPublicOpKeyService();
    }

    @Override
    public IOpResponse execute(GetTokensByCodeParams params) throws Exception {
        validate(params);

        final Rp rp = getRp();
        OpenIdConfigurationResponse discoveryResponse = discoveryService.getConnectDiscoveryResponse(rp);

        final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(params.getCode());
        tokenRequest.setRedirectUri(rp.getRedirectUri());
        tokenRequest.setAuthUsername(rp.getClientId());
        AuthenticationMethod authenticationMethod = Strings.isNullOrEmpty(params.getAuthenticationMethod()) ? AuthenticationMethod.fromString(rp.getTokenEndpointAuthMethod()) : AuthenticationMethod.fromString(params.getAuthenticationMethod());

        if (authenticationMethod == null) {
            LOG.debug("TokenEndpointAuthMethod is either not set or not valid. Setting `client_secret_basic` as AuthenticationMethod. TokenEndpointAuthMethod : {} ", rp.getTokenEndpointAuthMethod());
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        } else {
            tokenRequest.setAuthenticationMethod(authenticationMethod);
        }

        if (Lists.newArrayList(AuthenticationMethod.PRIVATE_KEY_JWT, AuthenticationMethod.TLS_CLIENT_AUTH, AuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH).contains(authenticationMethod)) {

            Algorithm algorithm = Strings.isNullOrEmpty(params.getAlgorithm()) ? Algorithm.fromString(rp.getTokenEndpointAuthSigningAlg()) : Algorithm.fromString(params.getAlgorithm());
            if (algorithm == null) {
                LOG.error("TokenEndpointAuthSigningAlg is either not set or not valid. TokenEndpointAuthSigningAlg : {} ", rp.getTokenEndpointAuthSigningAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }

            tokenRequest.setAlgorithm(SignatureAlgorithm.fromString(rp.getTokenEndpointAuthSigningAlg()));

            if (!jansConfigurationService.find().getEnableJwksGeneration()) {
                LOG.error("The Token Authentication Method is {}. Please set `enable_jwks_generation` (to `true`), `crypt_provider_key_store_path` and `crypt_provider_key_store_password` in `client-api-server.yml` to enable RP-jwks generation in jans-client-api.", authenticationMethod.toString());
                throw new HttpException(ErrorResponseCode.JWKS_GENERATION_DISABLE);
            }

            tokenRequest.setCryptoProvider(keyGeneratorService.getCryptoProvider());
            tokenRequest.setKeyId(keyGeneratorService.getCryptoProvider().getKeyId(keyGeneratorService.getKeys(), algorithm, Use.SIGNATURE));
            tokenRequest.setAudience(discoveryResponse.getTokenEndpoint());
        } else {
            tokenRequest.setAuthPassword(rp.getClientSecret());
        }

        final TokenClient tokenClient = opClientFactory.createTokenClient(discoveryResponse.getTokenEndpoint());
        tokenClient.setExecutor(httpService.getClientEngine());
        tokenClient.setRequest(tokenRequest);
        final TokenResponse response = tokenClient.exec();

        if (response.getStatus() == 200 || response.getStatus() == 302) { // success or redirect

            if (Strings.isNullOrEmpty(response.getIdToken())) {
                LOG.error("id_token is not returned. Please check: 1) OP log file for error (oxauth.log) 2) whether 'openid' scope is present for 'get_authorization_url' command");
                LOG.error("Entity: " + response.getEntity());
                throw new HttpException(ErrorResponseCode.NO_ID_TOKEN_RETURNED);
            }

            if (Strings.isNullOrEmpty(response.getAccessToken())) {
                LOG.error("access_token is not returned");
                throw new HttpException(ErrorResponseCode.NO_ACCESS_TOKEN_RETURNED);
            }

            final Jwt idToken = Jwt.parse(response.getIdToken());

            final Validator validator = new Validator.Builder()
                    .discoveryResponse(discoveryResponse)
                    .idToken(idToken)
                    .keyService(publicOpKeyService)
                    .opClientFactory(opClientFactory)
                    .rpServerConfiguration(jansConfigurationService.find())
                    .rp(rp)
                    .build();

            String state = stateService.encodeExpiredObject(params.getState(), ExpiredObjectType.STATE);

            validator.validateNonce(stateService);
            validator.validateIdToken();
            validator.validateAccessToken(response.getAccessToken());
            validator.validateState(state);
            // persist tokens
            rp.setIdToken(response.getIdToken());
            rp.setAccessToken(response.getAccessToken());
            rpService.update(rp);
            stateService.deleteExpiredObjectsByKey(state);

            LOG.trace("Scope: " + response.getScope());

            final GetTokensByCodeResponse opResponse = new GetTokensByCodeResponse();
            opResponse.setAccessToken(response.getAccessToken());
            opResponse.setIdToken(response.getIdToken());
            opResponse.setRefreshToken(response.getRefreshToken());
            opResponse.setExpiresIn(response.getExpiresIn() != null ? response.getExpiresIn() : -1);
            opResponse.setIdTokenClaims(Jackson2.createJsonMapper().readTree(idToken.getClaims().toJsonString()));
            return opResponse;
        } else {
            if (response.getStatus() == 400) {
                throw new HttpException(ErrorResponseCode.BAD_REQUEST_INVALID_CODE);
            }
            LOG.error("Failed to get tokens because response code is: " + response.getScope());
        }
        return null;
    }

    private void validate(GetTokensByCodeParams params) {

        if (Strings.isNullOrEmpty(params.getCode())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_CODE);
        }
        if (Strings.isNullOrEmpty(params.getState())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_STATE);
        }
        try {
            String keyExpiredObject = stateService.encodeExpiredObject(params.getState(), ExpiredObjectType.STATE);
            if (!stateService.isExpiredObjectPresent(keyExpiredObject)) {
                throw new HttpException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
            }
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
        }
    }
}
