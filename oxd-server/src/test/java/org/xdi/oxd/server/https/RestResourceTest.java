package org.xdi.oxd.server.https;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.server.OxdServerApplication;
import org.xdi.oxd.server.OxdServerConfiguration;

import javax.ws.rs.client.Client;

public class RestResourceTest {

    public static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("oxd-server-ce-dev3.yml");

    private static RegisterSiteParams registerSiteParams;
    private static String userId = null;
    private static String userSecret = null;
    private static int oxdPort = 0;
    private static String oxdHost = null;
    private static String accessToken = null;
    private static Client client;
    private static String oxdId = null;

    @ClassRule
    public static final DropwizardAppRule<OxdServerConfiguration> RULE = new DropwizardAppRule<>(OxdServerApplication.class, CONFIG_PATH);
    @ClassRule
    public static final ResourceTestRule RESOURCES = ResourceTestRule.builder().build();

//    @BeforeClass
//    public static void setUp() throws Exception {
//        client = ClientBuilder.newClient();
//        OxdServerConfiguration configuration = new OxdServerConfiguration();
//
//        registerSiteParams = new RegisterSiteParams();
//        registerSiteParams.setOpHost(configuration.getOpHost()); // your locally hosted gluu server can work
//        registerSiteParams.setAuthorizationRedirectUri(configuration.getAuthorizationRedirectUrl());//Your client application auth redirect url
//        registerSiteParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));  //Scope
//        registerSiteParams.setTrustedClient(true);
//        registerSiteParams.setGrantType(Lists.newArrayList("authorization_code", "client_credentials"));
//
//        userId = configuration.getUserID();
//        userSecret = configuration.getUserSecret();
//        oxdPort = Integer.parseInt(configuration.getOxdPort());
//        oxdHost = configuration.getOxdHost();
//
//        //Get AccessToken
//        SetupClientResponse setupClientResponse = setupClient(registerSiteParams);
//
//        GetClientTokenParams clientTokenParams = new GetClientTokenParams();
//        clientTokenParams.setClientId(setupClientResponse.getClientId());
//        clientTokenParams.setClientSecret(setupClientResponse.getClientSecret());
//        clientTokenParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));
//        clientTokenParams.setOpHost(configuration.getOpHost());
//
//        GetClientTokenResponse clientTokenResponse = getClientToken(clientTokenParams);
//        accessToken = clientTokenResponse.getAccessToken();
//
//        oxdId = setupClientResponse.getOxdId();
//    }
//
//    @AfterClass
//    public static void tearDown() throws Exception {
//        client.close();
//    }
//
//    @Test
//    public void testSetupClient() throws IOException {
//        SetupClientResponse setupclientResponse = setupClient(registerSiteParams);
//        assertNotNull(setupclientResponse);
//        output("SETUP CLIENT", setupclientResponse);
//    }
//
//    @Test
//    public void testGetClientToken() throws IOException {
//        SetupClientResponse setupclientResponse = setupClient(registerSiteParams);
//        assertNotNull(setupclientResponse);
//        output("SETUP CLIENT", setupclientResponse);
//
//        GetClientTokenParams clientTokenParams = new GetClientTokenParams();
//        clientTokenParams.setClientId(setupclientResponse.getClientId());
//        clientTokenParams.setClientSecret(setupclientResponse.getClientSecret());
//        clientTokenParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));
//        clientTokenParams.setOpHost(registerSiteParams.getOpHost());
//
//        GetClientTokenResponse clientTokenResponse = getClientToken(clientTokenParams);
//        assertNotNull(clientTokenResponse);
//        output("GET CLIENT TOKEN", clientTokenResponse);
//    }
//
//    @Test
//    public void testRegisterSite() throws IOException {
//        assertNotNull(accessToken);
//
//        RegisterSiteResponse registerSiteResponse = registerSite(registerSiteParams);
//        assertNotNull(registerSiteResponse);
//        output("REGISTER SITE", registerSiteResponse);
//    }
//
//    @Test
//    public void testUpdateSite() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        UpdateSiteParams updateSiteParams = new UpdateSiteParams();
//        updateSiteParams.setOxdId(oxdId);
//        updateSiteParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));
//
//        CommandResponse commandResponse = updateSite(updateSiteParams);
//        assertNotNull(commandResponse);
//        output("UPDATE SITE", commandResponse);
//    }
//
//    @Test
//    public void testGetAuthorizationUrl() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        GetAuthorizationUrlParams getAuthorizationUrlParams = new GetAuthorizationUrlParams();
//        getAuthorizationUrlParams.setOxdId(oxdId);
//
//        GetAuthorizationUrlResponse getAuthorizationUrlResponse = getAuthorizationUrl(getAuthorizationUrlParams);
//        assertNotNull(getAuthorizationUrlResponse);
//        output("GET AUTHORIZATION URL", getAuthorizationUrlResponse);
//    }
//
//    @Test
//    public void testGetTokenByCode() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        String state = CoreUtils.secureRandomString();
//        String code = codeRequest(oxdId, state);
//        assertNotNull(code);
//
//        GetTokensByCodeParams params = new GetTokensByCodeParams();
//        params.setCode(code);
//        params.setOxdId(oxdId);
//        params.setState(state);
//
//        GetTokensByCodeResponse getTokenByCodeResponse = getTokenByCode(params);
//        assertNotNull(getTokenByCodeResponse);
//
//        output("GET TOKEN BY CODE", getTokenByCodeResponse);
//    }
//
//    @Test
//    public void testGetUserInfo() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        String state = CoreUtils.secureRandomString();
//        String code = codeRequest(oxdId, state);
//        assertNotNull(code);
//
//        GetTokensByCodeParams params = new GetTokensByCodeParams();
//        params.setCode(code);
//        params.setOxdId(oxdId);
//        params.setState(state);
//
//        GetTokensByCodeResponse getTokenByCodeResponse = getTokenByCode(params);
//        assertNotNull(getTokenByCodeResponse);
//
//        GetUserInfoParams getUserInfoParams = new GetUserInfoParams();
//        getUserInfoParams.setOxdId(oxdId);
//        getUserInfoParams.setAccessToken(getTokenByCodeResponse.getAccessToken());
//
//        GetUserInfoResponse getUserInfoResponse = getUserInfo(getUserInfoParams);
//        assertNotNull(getUserInfoResponse);
//        output("GET USER INFO", getUserInfoResponse);
//    }
//
//    @Test
//    public void testGetLogoutUri() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        String state = CoreUtils.secureRandomString();
//
//        String code = codeRequest(oxdId, state);
//        assertNotNull(code);
//
//        GetTokensByCodeParams params = new GetTokensByCodeParams();
//        params.setCode(code);
//        params.setOxdId(oxdId);
//        params.setState(state);
//
//        GetTokensByCodeResponse getTokenBuCodeResponse = getTokenByCode(params);
//        assertNotNull(getTokenBuCodeResponse);
//
//        GetLogoutUrlParams getLogoutUrlParams = new GetLogoutUrlParams();
//        getLogoutUrlParams.setOxdId(oxdId);
//        getLogoutUrlParams.setIdTokenHint(getTokenBuCodeResponse.getIdToken());
//
//        LogoutResponse logoutResponse = getLogoutUri(getLogoutUrlParams);
//        assertNotNull(logoutResponse);
//        output("LOGOUT URI", logoutResponse);
//    }
//
//    @Test
//    public void testgetAccessTokenByRefreshToken() throws IOException {
//        assertNotNull(oxdId);
//
//        String state = CoreUtils.secureRandomString();
//        String code = codeRequest(oxdId, state);
//        assertNotNull(code);
//
//        GetTokensByCodeParams params = new GetTokensByCodeParams();
//        params.setCode(code);
//        params.setOxdId(oxdId);
//        params.setState(state);
//
//        GetTokensByCodeResponse getTokenByCodeResponse = getTokenByCode(params);
//        assertNotNull(getTokenByCodeResponse);
//
//        GetAccessTokenByRefreshTokenParams getAccessTokenByRefreshTokenParams = new GetAccessTokenByRefreshTokenParams();
//        getAccessTokenByRefreshTokenParams.setOxdId(oxdId);
//        getAccessTokenByRefreshTokenParams.setRefreshToken(getTokenByCodeResponse.getRefreshToken());
//        getAccessTokenByRefreshTokenParams.setScope(Lists.newArrayList("openid", "profile", "email", "uma_protection"));
//
//        GetClientTokenResponse getClientTokenResponse = getAccessTokenByRefreshToken(getAccessTokenByRefreshTokenParams);
//        assertNotNull(getClientTokenResponse);
//        output("GET ACCESSTOKEN BY REFRESHTOKEN", getClientTokenResponse);
//    }
//
//
//    @Test
//    public void testUmaRsProtect() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        String rsProtect = "{\"resources\":[{\"path\":\"/scim\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1\"],\"ticketScopes\":[\"https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1\"]}]}]}";
//
//        RsProtectParams rsProtectParams = new RsProtectParams();
//        rsProtectParams.setOxdId(oxdId);
//        rsProtectParams.setResources(resourceList(rsProtect).getResources());
//
//        RsProtectResponse rsProtectResponse = umaRsProtect(rsProtectParams);
//        assertNotNull(rsProtectResponse);
//        output("UMA Rs Resource Protection", rsProtectResponse);
//    }
//
//
//    @Test
//    public void testUmaRsCheckAccess() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        RsCheckAccessParams rsCheckAccessParams = new RsCheckAccessParams();
//        rsCheckAccessParams.setOxdId(oxdId);
//        rsCheckAccessParams.setRpt(" ");
//        rsCheckAccessParams.setPath("/scim");
//        rsCheckAccessParams.setHttpMethod("GET");
//
//        RsCheckAccessResponse rsCheckAccessResponse = umaRsCheckAccess(rsCheckAccessParams);
//        assertNotNull(rsCheckAccessResponse);
//        output("UMA Rs check Access", rsCheckAccessResponse);
//    }
//
//    @Test
//    public void testUmaRpGetRpt() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        RsCheckAccessParams rsCheckAccessParams = new RsCheckAccessParams();
//        rsCheckAccessParams.setOxdId(oxdId);
//        rsCheckAccessParams.setRpt(" ");
//        rsCheckAccessParams.setPath("/scim");
//        rsCheckAccessParams.setHttpMethod("GET");
//
//        RsCheckAccessResponse rsCheckAccessResponse = umaRsCheckAccess(rsCheckAccessParams);
//        assertNotNull(rsCheckAccessResponse);
//
//
//        RpGetRptParams rpGetRptParams = new RpGetRptParams();
//        rpGetRptParams.setOxdId(oxdId);
//        rpGetRptParams.setTicket(rsCheckAccessResponse.getTicket());
//
//        RpGetRptResponse rpGetRptResponse = umaRpGetRpt(rpGetRptParams);
//        assertNotNull(rpGetRptResponse);
//        output("UMA RP GET RPT", rpGetRptResponse);
//    }
//
//    @Test
//    public void testUmaRpGetClaimsGatheringUrl() throws IOException {
//        assertNotNull(accessToken);
//        assertNotNull(oxdId);
//
//        RsCheckAccessParams rsCheckAccessParams = new RsCheckAccessParams();
//        rsCheckAccessParams.setOxdId(oxdId);
//        rsCheckAccessParams.setRpt(" ");
//        rsCheckAccessParams.setPath("/scim");
//        rsCheckAccessParams.setHttpMethod("GET");
//
//        RsCheckAccessResponse rsCheckAccessResponse = umaRsCheckAccess(rsCheckAccessParams);
//        assertNotNull(rsCheckAccessResponse);
//
//        RpGetRptParams rpGetRptParams = new RpGetRptParams();
//        rpGetRptParams.setOxdId(oxdId);
//        rpGetRptParams.setTicket(rsCheckAccessResponse.getTicket());
//
//        RpGetRptResponse rpGetRptResponse = umaRpGetRpt(rpGetRptParams);
//        assertNotNull(rpGetRptResponse);
//
//        RpGetClaimsGatheringUrlParams rpGetClaimsGatheringUrlParams = new RpGetClaimsGatheringUrlParams();
//        rpGetClaimsGatheringUrlParams.setOxdId(oxdId);
//        rpGetClaimsGatheringUrlParams.setTicket(rsCheckAccessResponse.getTicket());
//        rpGetClaimsGatheringUrlParams.setClaimsRedirectUri("https://client.example.com/cb");
//        rpGetClaimsGatheringUrlParams.setProtectionAccessToken(accessToken);
//
//        RpGetClaimsGatheringUrlResponse rpGetClaimsGatheringUrlResponse = umaRpGetClaimsGatheringUrl(rpGetClaimsGatheringUrlParams);
//        assertNotNull(rpGetRptResponse);
//        output("UMA RP GET CLAIMS GATHERING URL", rpGetClaimsGatheringUrlResponse);
//    }
//
//    private String codeRequest(String oxdId, String state) {
//        CommandClient client = null;
//        try {
//            String nonce = CoreUtils.secureRandomString();
//
//            client = new CommandClient(oxdHost, oxdPort);
//            return GetTokensByCodeTest.codeRequest(client, oxdId, userId, userSecret, state, nonce);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            CommandClient.closeQuietly(client);
//        }
//        return null;
//    }
//
//    private static String getParameterJson(Object para) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        return mapper.writeValueAsString(para);
//    }
//
//    private static RsResourceList resourceList(String rsProtect) throws IOException {
//        rsProtect = StringUtil.replace(rsProtect, "'", "\"");
//        return Jackson.createJsonMapper().readValue(rsProtect, RsResourceList.class);
//    }
//
//    private static SetupClientResponse setupClient(RegisterSiteParams params) throws IOException {
//        return httpClient("setup-client", params, SetupClientResponse.class);
//    }
//
//    private static GetClientTokenResponse getClientToken(GetClientTokenParams params) throws IOException {
//        return httpClient("get-client-token", params, GetClientTokenResponse.class);
//    }
//
//    private RegisterSiteResponse registerSite(RegisterSiteParams params) throws IOException {
//        return httpClient("register-site", params, RegisterSiteResponse.class);
//    }
//
//    private CommandResponse updateSite(UpdateSiteParams siteParams) throws IOException {
//        return httpClient("update-site", siteParams);
//    }
//
//    private GetAuthorizationUrlResponse getAuthorizationUrl(GetAuthorizationUrlParams params) throws IOException {
//        return httpClient("get-authorization-url", params, GetAuthorizationUrlResponse.class);
//    }
//
//    private GetTokensByCodeResponse getTokenByCode(GetTokensByCodeParams params) throws IOException {
//        return httpClient("get-tokens-by-code", params, GetTokensByCodeResponse.class);
//    }
//
//    private GetUserInfoResponse getUserInfo(GetUserInfoParams params) throws IOException {
//        return httpClient("get-user-info", params, GetUserInfoResponse.class);
//    }
//
//    private LogoutResponse getLogoutUri(GetLogoutUrlParams params) throws IOException {
//        return httpClient("get-logout-uri", params, LogoutResponse.class);
//    }
//
//    private GetClientTokenResponse getAccessTokenByRefreshToken(GetAccessTokenByRefreshTokenParams params) throws IOException {
//        return httpClient("get-access-token-by-refresh-token", params, GetClientTokenResponse.class);
//    }
//
//    private RsProtectResponse umaRsProtect(RsProtectParams params) throws IOException {
//        return httpClient("uma-rs-protect", params, RsProtectResponse.class);
//    }
//
//    private RsCheckAccessResponse umaRsCheckAccess(RsCheckAccessParams params) throws IOException {
//        return httpClient("uma-rs-check-access", params, RsCheckAccessResponse.class);
//    }
//
//    private RpGetRptResponse umaRpGetRpt(RpGetRptParams params) throws IOException {
//        return httpClient("uma-rp-get-rpt", params, RpGetRptResponse.class);
//    }
//
//    private RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(RpGetClaimsGatheringUrlParams params) throws IOException {
//        return httpClient("uma-rp-get-claims-gathering-url", params, RpGetClaimsGatheringUrlResponse.class);
//    }
//
//    public static <T extends IOpResponse> T httpClient(String endpoint, IParams params, Class<T> responseClazz) throws IOException {
//        CommandResponse commandResponse = httpClient(endpoint, params);
//        return RestResource.read(commandResponse.getData().toString(), responseClazz);
//    }
//
//    private static CommandResponse httpClient(String endpoint, IParams params) throws IOException {
//        final String entity = client.target("http://localhost:" + RULE.getLocalPort() + "/" + endpoint)
//                .request()
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                .post(Entity.json(getParameterJson(params)))
//                .readEntity(String.class);
//
//        System.out.println("Plain string: " + entity);
//        return RestResource.read(entity, CommandResponse.class);
//    }
//
//    private static void output(String testCase, Object response) {
//        System.out.println("[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------");
//        System.out.println("[INFO] TEST CASE : " + testCase);
//        System.out.println("[INFO] ----------------------------------------------------------------------------------------------------------------------------------------------------");
//        System.out.println("[INFO] RESPONSE   : " + response);
//        System.out.println("[INFO] ----------------------------------------------------------------------------------------------------------------------------------------------------");
//        System.out.println(" ");
//    }
}