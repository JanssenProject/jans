package org.xdi.oxd.server.op;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.GetClientTokenParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.server.Utils;

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
    public CommandResponse execute(GetClientTokenParams params) throws Exception {
        try {
            final TokenClient tokenClient = new TokenClient(getDiscoveryService().getConnectDiscoveryResponse(params.getOpHost()).getTokenEndpoint());
            tokenClient.setExecutor(getHttpService().getClientExecutor());
            final TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(scopeAsString(params), params.getClientId(), params.getClientSecret());
            if (tokenResponse != null) {
                if (Util.allNotBlank(tokenResponse.getAccessToken())) {
                    GetClientTokenResponse response = new GetClientTokenResponse();
                    response.setAccessToken(tokenResponse.getAccessToken());
                    response.setExpiresIn(tokenResponse.getExpiresIn());
                    response.setRefreshToken(tokenResponse.getRefreshToken());
                    response.setScope(tokenResponse.getScope());

                    return okResponse(response);
                } else {
                    LOG.error("access_token is blank in response, params: " + params + ", response: " + tokenResponse);
                }
            } else {
                LOG.error("No response from TokenClient");
            }
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
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
