package io.jans.ca.plugin.adminui.service.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.ca.plugin.adminui.model.auth.ApiTokenRequest;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.service.EncryptionService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.*;

@Singleton
public class OAuth2Service extends BaseService {
    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    EncryptionService encryptionService;

    /**
     * The function `getApiProtectionToken` retrieves an API protection token based on the provided parameters and handles
     * exceptions accordingly.
     *
     * @param apiTokenRequest The `getApiProtectionToken` method is responsible for obtaining an API protection token based
     * on the provided `ApiTokenRequest` and `appType`. The method first retrieves the necessary configuration from
     * `auiConfigurationService` based on the `appType`. It then constructs a `TokenRequest` object
     * @param appType The `appType` parameter in the `getApiProtectionToken` method is used to specify the type of the
     * application for which the API protection token is being requested. It is passed as a String parameter to the method.
     * The `appType` parameter helps in determining the specific configuration settings and endpoints
     * @throws ApplicationException
     * @return The method `getApiProtectionToken` returns a `TokenResponse` object containing the access token, id token,
     * refresh token, scopes, iat (issued at) timestamp, exp (expiration) timestamp, and issuer information.
     */
    public TokenResponse getApiProtectionToken(ApiTokenRequest apiTokenRequest, String appType) throws ApplicationException {
        try {
            log.debug("Getting api-protection token");

            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration(appType);

            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(auiConfiguration.getAuiBackendApiServerClientId());
            tokenRequest.setAuthPassword(encryptionService.decrypt(auiConfiguration.getAuiBackendApiServerClientSecret()));
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setRedirectUri(auiConfiguration.getAuiBackendApiServerRedirectUrl());

            io.jans.as.client.TokenResponse tokenResponse = null;
            if (apiTokenRequest == null) {
                tokenRequest.setScope(scopeAsString(Arrays.asList(OAuth2Resource.SCOPE_OPENID)));
                tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint());
            } else {
                if (Strings.isNullOrEmpty(apiTokenRequest.getUjwt())) {
                    log.warn(ErrorResponse.USER_INFO_JWT_BLANK.getDescription());
                    tokenRequest.setScope(scopeAsString(Arrays.asList(OAuth2Resource.SCOPE_OPENID)));
                }
                tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint(), apiTokenRequest.getUjwt(), apiTokenRequest.getPermissionTag());
            }

            Optional<Map<String, Object>> introspectionResponse = introspectToken(tokenResponse.getAccessToken(), auiConfiguration.getAuiBackendApiServerIntrospectionEndpoint());


            TokenResponse tokenResp = new TokenResponse();
            tokenResp.setAccessToken(tokenResponse.getAccessToken());
            tokenResp.setIdToken(tokenResponse.getIdToken());
            tokenResp.setRefreshToken(tokenResponse.getRefreshToken());

            if (!introspectionResponse.isPresent()) {
                return tokenResp;
            }
            final String SCOPE = "scope";
            Map<String, Object> claims = introspectionResponse.get();
            if (claims.get(SCOPE) != null) {
                if (claims.get(SCOPE) instanceof List) {
                    tokenResp.setScopes((List) claims.get(SCOPE));
                }
                if (claims.get(SCOPE) instanceof String) {
                    tokenResp.setScopes(Arrays.asList(((String) claims.get(SCOPE)).split(" ")));
                }
            }
            if (claims.get("iat") != null) {
                tokenResp.setIat(Long.valueOf(claims.get("iat").toString()));
            }

            if (claims.get("exp") != null) {
                tokenResp.setExp(Long.valueOf(claims.get("exp").toString()));
            }

            if (claims.get("iss") != null) {
                tokenResp.setIssuer(claims.get("iss").toString());
            }

            return tokenResp;
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription());
        }
    }

    private static String scopeAsString(List<String> scopes) throws UnsupportedEncodingException {
        Set<String> scope = Sets.newHashSet();
        scope.addAll(scopes);
        return CommonUtils.joinAndUrlEncode(scope);
    }
}
