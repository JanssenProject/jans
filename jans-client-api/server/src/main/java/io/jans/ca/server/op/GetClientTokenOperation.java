package io.jans.ca.server.op;

import com.google.common.collect.Sets;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.Util;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetClientTokenParams;
import io.jans.ca.common.response.GetClientTokenResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.HttpService;
import io.jans.ca.server.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

public class GetClientTokenOperation extends BaseOperation<GetClientTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetClientTokenOperation.class);

    private DiscoveryService discoveryService;

    private HttpService httpService;

    private OpClientFactoryImpl opClientFactory;

    /**
     * Base constructor
     *
     * @param command command
     */
    public GetClientTokenOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetClientTokenParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
        this.httpService = discoveryService.getHttpService();
        this.opClientFactory = discoveryService.getOpClientFactory();
    }

    @Override
    public IOpResponse execute(GetClientTokenParams params) {
        try {
            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getAuthenticationMethod());
            final String tokenEndpoint = discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath()).getTokenEndpoint();
            final TokenClient tokenClient = opClientFactory.createTokenClient(tokenEndpoint);
            tokenClient.setExecutor(httpService.getClientEngine());

            final TokenResponse tokenResponse;
            if (authenticationMethod == AuthenticationMethod.PRIVATE_KEY_JWT) {
                LOG.trace("Getting client token with private_key_jwt client authentication ...");

                SignatureAlgorithm algorithm = SignatureAlgorithm.fromString(params.getAlgorithm());
                if (algorithm == null) {
                    throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
                }

                TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
                tokenRequest.setScope(scopeAsString(params));
                tokenRequest.setAuthUsername(params.getClientId());
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
                tokenRequest.setAlgorithm(algorithm);
                tokenRequest.setCryptoProvider(getCryptoProvider());
                tokenRequest.setKeyId(params.getKeyId());
                tokenRequest.setAudience(tokenEndpoint);

                tokenClient.setRequest(tokenRequest);
                tokenResponse = tokenClient.exec();
            } else {
                tokenResponse = tokenClient.execClientCredentialsGrant(scopeAsString(params), params.getClientId(), params.getClientSecret());
            }
            if (tokenResponse != null) {
                if (Util.allNotBlank(tokenResponse.getAccessToken())) {
                    GetClientTokenResponse response = new GetClientTokenResponse();
                    response.setAccessToken(tokenResponse.getAccessToken());
                    response.setExpiresIn(tokenResponse.getExpiresIn());
                    response.setRefreshToken(tokenResponse.getRefreshToken());
                    response.setScope(Utils.stringToList(tokenResponse.getScope()));

                    return response;
                } else {
                    LOG.error("access_token is blank in response, params: " + params + ", response: " + tokenResponse);
                    LOG.error("Please check AS logs for more details (oxauth.log for CE).");
                }
            } else {
                LOG.error("No response from TokenClient");
                LOG.error("Please check AS logs for more details (oxauth.log for CE).");
            }
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        throw HttpException.internalError();
    }

    private String scopeAsString(GetClientTokenParams params) throws UnsupportedEncodingException {
        Set<String> scope = Sets.newHashSet();

        scope.add("openid");
        if (params.getScope() != null) {
            scope.addAll(params.getScope());
        }
        return Utils.joinAndUrlEncode(scope);
    }
}
