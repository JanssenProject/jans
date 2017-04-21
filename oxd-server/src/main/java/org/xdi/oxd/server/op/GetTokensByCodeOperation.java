package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.ClientUtils;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetTokensByCodeOperation extends BaseOperation<GetTokensByCodeParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetTokensByCodeOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetTokensByCodeOperation(Command command, final Injector injector) {
        super(command, injector, GetTokensByCodeParams.class);
    }

    @Override
    public CommandResponse execute(GetTokensByCodeParams params) throws Exception {
        validate(params);

        final SiteConfiguration site = getSite();
        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponse(site.getOpHost());

        final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(params.getCode());
        tokenRequest.setRedirectUri(site.getAuthorizationRedirectUri());
        tokenRequest.setAuthUsername(site.getClientId());
        tokenRequest.setAuthPassword(site.getClientSecret());
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);


        final TokenClient tokenClient = new TokenClient(discoveryResponse.getTokenEndpoint());
        tokenClient.setExecutor(getHttpService().getClientExecutor());
        tokenClient.setRequest(tokenRequest);
        final TokenResponse response = tokenClient.exec();
        ClientUtils.showClient(tokenClient);

        if (response.getStatus() == 200 || response.getStatus() == 302) { // success or redirect

            if (Strings.isNullOrEmpty(response.getIdToken())) {
                LOG.error("id_token is not returned. Please check whether 'openid' scope is present for 'get_authorization_url' command");
                throw new ErrorResponseException(ErrorResponseCode.NO_ID_TOKEN_RETURNED);
            }

            if (Strings.isNullOrEmpty(response.getAccessToken())) {
                LOG.error("access_token is not returned");
                throw new ErrorResponseException(ErrorResponseCode.NO_ACCESS_TOKEN_RETURNED);
            }

            final Jwt idToken = Jwt.parse(response.getIdToken());

            final Validator validator = new Validator(idToken, discoveryResponse, getKeyService());
            validator.validateNonce(getStateService());
            validator.validateIdToken(site.getClientId());
            validator.validateAccessToken(response.getAccessToken());

            // persist tokens
            site.setIdToken(response.getIdToken());
            site.setAccessToken(response.getAccessToken());
            getSiteService().update(site);
            getStateService().invalidateState(params.getState());

            final Map<String, List<String>> claims = idToken.getClaims() != null ? idToken.getClaims().toMap() : new HashMap<String, List<String>>();

            final GetTokensByCodeResponse opResponse = new GetTokensByCodeResponse();
            opResponse.setAccessToken(response.getAccessToken());
            opResponse.setIdToken(response.getIdToken());
            opResponse.setRefreshToken(response.getRefreshToken());
            opResponse.setExpiresIn(response.getExpiresIn());
            opResponse.setIdTokenClaims(claims);
            return okResponse(opResponse);
        } else {
            LOG.error("Failed to get tokens because response code is: " + response.getScope());
        }
        return null;
    }

    private void validate(GetTokensByCodeParams params) {
        if (Strings.isNullOrEmpty(params.getCode())) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_CODE);
        }
        if (Strings.isNullOrEmpty(params.getState())) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_STATE);
        }
        if (!getStateService().isStateValid(params.getState())) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
        }
    }
}
