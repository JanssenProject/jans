package org.gluu.oxd.resources;

import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.gluu.oxd.OxdHttpsApplication;
import org.gluu.oxd.OxdHttpsConfiguration;
import org.gluu.oxd.RestResource;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.client.GetTokensByCodeTest;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.common.response.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;


/**
 * @author yuriyz
 */
public class StressTest {

    private static RegisterSiteParams registerSiteParams;
    private static String userId = null;
    private static String userSecret = null;
    private static int oxdPort = 0;
    private static String oxdHost = null;
    private static String accessToken = null;
    private static Client client;
    private static String oxdId = null;

    public static final DropwizardTestSupport<OxdHttpsConfiguration> SUPPORT =
            new DropwizardTestSupport<>(OxdHttpsApplication.class,
                    ResourceHelpers.resourceFilePath("oxd-https-extension-ce-dev3.yml"),
                    ConfigOverride.config("server.applicationConnectors[0].port", "0")
            );

    @BeforeClass
    public static void beforeClass() throws Exception {
        SUPPORT.before();

        client = ClientBuilder.newClient();
        OxdHttpsConfiguration configuration = new OxdHttpsConfiguration();

        registerSiteParams = new RegisterSiteParams();
        registerSiteParams.setOpHost(configuration.getOpHost()); // your locally hosted gluu server can work
        registerSiteParams.setAuthorizationRedirectUri(configuration.getAuthorizationRedirectUrl());//Your client application auth redirect url
        registerSiteParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));  //Scope
        registerSiteParams.setTrustedClient(true);
        registerSiteParams.setGrantType(Lists.newArrayList("authorization_code", "client_credentials"));

        userId = configuration.getUserID();
        userSecret = configuration.getUserSecret();
        oxdPort = Integer.parseInt(configuration.getOxdPort());
        oxdHost = configuration.getOxdHost();

        //Get AccessToken
        SetupClientResponse setupClientResponse = httpClient("setup-client", registerSiteParams, SetupClientResponse.class);

        GetClientTokenParams clientTokenParams = new GetClientTokenParams();
        clientTokenParams.setClientId(setupClientResponse.getClientId());
        clientTokenParams.setClientSecret(setupClientResponse.getClientSecret());
        clientTokenParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));
        clientTokenParams.setOpHost(configuration.getOpHost());

        GetClientTokenResponse clientTokenResponse = httpClient("get-client-token", clientTokenParams, GetClientTokenResponse.class);
        accessToken = clientTokenResponse.getAccessToken();

        oxdId = setupClientResponse.getOxdId();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SUPPORT.after();
        client.close();
    }

    @Test(invocationCount = 500, threadPoolSize = 500, enabled = true)
    public void testGetAuthorizationUrl() throws IOException {
        Assert.assertNotNull(accessToken);
        Assert.assertNotNull(oxdId);

        GetAuthorizationUrlParams params = new GetAuthorizationUrlParams();
        params.setOxdId(oxdId);

        GetAuthorizationUrlResponse getAuthorizationUrlResponse = httpClient("get-authorization-url", params, GetAuthorizationUrlResponse.class);;
        Assert.assertNotNull(getAuthorizationUrlResponse);
        RestResourceTest.output("GET AUTHORIZATION URL", getAuthorizationUrlResponse);
    }

    @Test(invocationCount = 1, threadPoolSize = 1, enabled = true)
    public void testGetTokenByCode() throws IOException {
        assertNotNull(accessToken);
        assertNotNull(oxdId);

        String state = CoreUtils.secureRandomString();
        String code = codeRequest(oxdId, state);
        assertNotNull(code);

        GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setCode(code);
        params.setOxdId(oxdId);
        params.setState(state);

        GetTokensByCodeResponse getTokenByCodeResponse = httpClient("get-tokens-by-code", params, GetTokensByCodeResponse.class);
        assertNotNull(getTokenByCodeResponse);

        RestResourceTest.output("GET TOKEN BY CODE", getTokenByCodeResponse);
    }

    public static CommandResponse httpClient(String endpoint, IParams params) throws IOException {
        final String entity = client.target("http://localhost:" + SUPPORT.getLocalPort() + "/" + endpoint)
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.json(RestResourceTest.getParameterJson(params)))
                .readEntity(String.class);

        System.out.println("Plain string: " + entity);
        return RestResource.read(entity, CommandResponse.class);
    }

    public static <T extends IOpResponse> T httpClient(String endpoint, IParams params, Class<T> responseClazz) throws IOException {
        CommandResponse commandResponse = httpClient(endpoint, params);
        return RestResource.read(commandResponse.getData().toString(), responseClazz);
    }

    public static String codeRequest(String oxdId, String state) {
        CommandClient client = null;
        try {
            String nonce = CoreUtils.secureRandomString();

            client = new CommandClient(oxdHost, oxdPort);
            return GetTokensByCodeTest.codeRequest(client, oxdId, userId, userSecret, state, nonce);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            CommandClient.closeQuietly(client);
        }
        return null;
    }
}
