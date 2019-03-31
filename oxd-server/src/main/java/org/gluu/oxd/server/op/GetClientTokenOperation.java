package org.gluu.oxd.server.op;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.TokenClient;
import org.gluu.oxauth.client.TokenRequest;
import org.gluu.oxauth.client.TokenResponse;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.GetClientTokenParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;

import java.io.UnsupportedEncodingException;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

public class GetClientTokenOperation extends BaseOperation<GetClientTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetClientTokenOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetClientTokenOperation(Command command, final Injector injector) {
        super(command, injector, GetClientTokenParams.class);
    }

    @Override
    public IOpResponse execute(GetClientTokenParams params) {
        try {
            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getAuthenticationMethod());
            final String tokenEndpoint = getDiscoveryService().getConnectDiscoveryResponse(params.getOpHost(), params.getOpDiscoveryPath()).getTokenEndpoint();
            final TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setExecutor(getHttpService().getClientExecutor());

            final TokenResponse tokenResponse;
            if (authenticationMethod == AuthenticationMethod.PRIVATE_KEY_JWT) {
                LOG.trace("Getting client token with private_key_jwt client authentication ...");

                SignatureAlgorithm algorithm = SignatureAlgorithm.fromString(params.getAlgorithm());
                if (algorithm == null) {
                    throw new HttpException(ErrorResponseCode.INVALID_ALGORITHM);
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
