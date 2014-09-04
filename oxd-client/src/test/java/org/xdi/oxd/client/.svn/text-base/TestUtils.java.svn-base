package org.xdi.oxd.client;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.Assert;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.ClientUtils;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.client.uma.RequesterPermissionTokenService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.RequesterPermissionTokenResponse;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.ObtainAatParams;
import org.xdi.oxd.common.params.ObtainPatParams;
import org.xdi.oxd.common.params.RegisterPermissionTicketParams;
import org.xdi.oxd.common.params.RegisterResourceParams;
import org.xdi.oxd.common.response.ObtainAatOpResponse;
import org.xdi.oxd.common.response.ObtainPatOpResponse;
import org.xdi.oxd.common.response.RegisterPermissionTicketOpResponse;
import org.xdi.oxd.common.response.RegisterResourceOpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class TestUtils {
    private TestUtils() {
    }


    public static TokenResponse obtainAccessToken(String userId, String userSecret, String clientId, String clientSecret, String redirectUrl,
                                            String p_authorizationEndpoint, String p_tokenEndpoint) {
        // 1. Request authorization and receive the authorization code.
        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);
        final List<String> scopes = new ArrayList<String>();
        scopes.add("openid");

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUrl, null);
        request.setState("af0ifjsldkj");
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        final AuthorizeClient authorizeClient = new AuthorizeClient(p_authorizationEndpoint);
        authorizeClient.setRequest(request);
        final AuthorizationResponse response1 = authorizeClient.exec();

        ClientUtils.showClient(authorizeClient);

        final String scope = response1.getScope();
        final String authorizationCode = response1.getCode();

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUrl);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(scope);

            final TokenClient tokenClient1 = new TokenClient(p_tokenEndpoint);
            tokenClient1.setRequest(tokenRequest);
            final TokenResponse response2 = tokenClient1.exec();
            ClientUtils.showClient(authorizeClient);
            if (response2.getStatus() == 200) {
                return response2;
            }
        }
        return null;
    }

    public static String obtainRpt(String p_aat, String p_umaDiscoveryUrl, String p_amHost) {
        Assert.assertNotNull(p_aat);

        final MetadataConfiguration discovery = UmaClientFactory.instance().createMetaDataConfigurationService(p_umaDiscoveryUrl).getMetadataConfiguration();
        final RequesterPermissionTokenService requesterPermissionTokenService = UmaClientFactory.instance().createRequesterPermissionTokenService(discovery);

        // Get requester permission token
        RequesterPermissionTokenResponse requesterPermissionTokenResponse = null;
        try {
            requesterPermissionTokenResponse = requesterPermissionTokenService.getRequesterPermissionToken("Bearer " + p_aat, p_amHost);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        Assert.assertNotNull(requesterPermissionTokenResponse);

        return requesterPermissionTokenResponse.getToken();
    }

    public static ObtainPatOpResponse obtainPat(CommandClient p_commandClient,
                                                String p_discoveryUrl, String p_umaDiscoveryUrl, String p_redirectUrl,
                                                String p_clientId, String p_clientSecret) {
        return obtainPat(p_commandClient, p_discoveryUrl, p_umaDiscoveryUrl, p_redirectUrl, p_clientId, p_clientSecret, "", "");
    }

    public static ObtainPatOpResponse obtainPat(CommandClient p_commandClient,
                                                String p_discoveryUrl, String p_umaDiscoveryUrl, String p_redirectUrl,
                                                String p_clientId, String p_clientSecret,
                                                String p_userId, String p_userSecret) {
        final ObtainPatParams params = new ObtainPatParams();
        params.setDiscoveryUrl(p_discoveryUrl);
        params.setUmaDiscoveryUrl(p_umaDiscoveryUrl);
        params.setRedirectUrl(p_redirectUrl);
        params.setClientId(p_clientId);
        params.setClientSecret(p_clientSecret);
        params.setUserId(p_userId);
        params.setUserSecret(p_userSecret);

        final Command command = new Command(CommandType.OBTAIN_PAT);
        command.setParamsObject(params);

        final CommandResponse response = p_commandClient.send(command);
        junit.framework.Assert.assertNotNull(response);
        System.out.println(response);

        return response.dataAsResponse(ObtainPatOpResponse.class);
    }

    public static ObtainAatOpResponse obtainAat(CommandClient p_commandClient,
                                   String p_discoveryUrl, String p_umaDiscoveryUrl, String p_redirectUrl,
                                   String p_clientId, String p_clientSecret,
                                   String p_userId, String p_userSecret) {
        final ObtainAatParams params = new ObtainAatParams();
        params.setDiscoveryUrl(p_discoveryUrl);
        params.setUmaDiscoveryUrl(p_umaDiscoveryUrl);
        params.setRedirectUrl(p_redirectUrl);
        params.setClientId(p_clientId);
        params.setClientSecret(p_clientSecret);
        params.setUserId(p_userId);
        params.setUserSecret(p_userSecret);

        final Command command = new Command(CommandType.OBTAIN_AAT);
        command.setParamsObject(params);

        final CommandResponse response = p_commandClient.send(command);
        junit.framework.Assert.assertNotNull(response);
        System.out.println(response);

        return response.dataAsResponse(ObtainAatOpResponse.class);
    }

    public static RegisterResourceOpResponse registerResource(CommandClient p_client, String umaDiscoveryUrl, String patToken, List<String> p_scopes) {
        final RegisterResourceParams params = new RegisterResourceParams();
        params.setUmaDiscoveryUrl(umaDiscoveryUrl);
        params.setPatToken(patToken);
        params.setName("oxd test resource");
        params.setScopes(p_scopes);

        final Command command = new Command(CommandType.REGISTER_RESOURCE);
        command.setParamsObject(params);

        final CommandResponse response = p_client.send(command);
        junit.framework.Assert.assertNotNull(response);
        System.out.println(response);

        final RegisterResourceOpResponse r = response.dataAsResponse(RegisterResourceOpResponse.class);
        junit.framework.Assert.assertNotNull(r);
        return r;
    }

    public static RegisterPermissionTicketOpResponse registerTicket(CommandClient p_client, String umaDiscoveryUrl,
                                                                    String patToken, String resourceId, String amHost, String rsHost,
                                                                    List<String> p_scopes, String requestHttpMethod, String requestUrl) {
        final RegisterPermissionTicketParams params = new RegisterPermissionTicketParams();
        params.setUmaDiscoveryUrl(umaDiscoveryUrl);
        params.setPatToken(patToken);
        params.setAmHost(amHost);
        params.setRsHost(rsHost);
        params.setResourceSetId(resourceId);
        params.setScopes(p_scopes);
        params.setRequestHttpMethod(requestHttpMethod);
        params.setRequestUrl(requestUrl);

        final Command command = new Command(CommandType.REGISTER_TICKET);
        command.setParamsObject(params);

        final CommandResponse response = p_client.send(command);
        junit.framework.Assert.assertNotNull(response);
        System.out.println(response);

        final RegisterPermissionTicketOpResponse r = response.dataAsResponse(RegisterPermissionTicketOpResponse.class);
        junit.framework.Assert.assertNotNull(r);
        return r;
    }
}
