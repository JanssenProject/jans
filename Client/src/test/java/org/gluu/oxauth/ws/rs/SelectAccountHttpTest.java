package org.gluu.oxauth.ws.rs;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.util.Strings;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.oxauth.page.LoginPage;
import org.gluu.oxauth.page.PageConfig;
import org.gluu.oxauth.page.SelectPage;
import org.json.JSONArray;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SelectAccountHttpTest extends BaseTest {

    private PageConfig pageConfig;

    @BeforeTest
    public void setUp() {
        driver = new HtmlUnitDriver(true);
        pageConfig = newPageConfig(driver);
    }

    @AfterTest
    public void tearDown() {
        driver.quit();
        driver = null;
        pageConfig = null;
    }

    @Parameters({"userId", "userSecret", "userId2", "userSecret2", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void selectAccountTest(final String userId, final String userSecret,
                              final String userId2, final String userSecret2,
                              final String redirectUris, final String redirectUri, String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlow");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        output("1. Account1 : Request authorization and receive the code and id_token");
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, registerResponse.getClientId(), randomUUID());

        assertNotNull(authorizationResponse, "The authorization response is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertIdToken(authorizationResponse.getIdToken());
        String account1SessionId = assertSessionIdCookie();

        output("2. Account2 : Request authorization with prompt=select_account and receive the code and id_token");
        AuthorizationResponse responseFromSelectAccount = selectAccount(userId2, userSecret2, redirectUri, responseTypes, scopes, registerResponse.getClientId(), randomUUID());

        assertNotNull(responseFromSelectAccount, "The authorization response is null");
        assertNotNull(responseFromSelectAccount.getCode(), "The code is null");
        assertIdToken(responseFromSelectAccount.getIdToken());
        String account2SessionId = assertSessionIdCookie();
        assertNotEquals(account1SessionId, account2SessionId);

        output("3. Go again to Select Accounts : we should have 2 accounts");
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes, redirectUri, randomUUID());
        authorizationRequest.setState(randomUUID());
        authorizationRequest.setPrompts(Lists.newArrayList(Prompt.SELECT_ACCOUNT));

        output("4. both Account 1 and Account 2 sessions must be in current_sessions cookie");
        assertEquals(account2SessionId, assertSessionIdCookie());
        List<Object> currentSessions = new JSONArray(driver.manage().getCookieNamed("current_sessions").getValue()).toList();
        assertTrue(currentSessions.contains(account1SessionId));
        assertTrue(currentSessions.contains(account2SessionId));

        output("5. Check that we have 2 buttons for Account 1 and Account 2");
        final SelectPage selectPage = SelectPage.navigate(pageConfig, authorizationEndpoint + "?" + authorizationRequest.getQueryString());
        assertNotNull(selectPage.getAccountButton("oxAuth Test User"));
        assertNotNull(selectPage.getAccountButton("oxAuth Test User2"));

        output("6. Switch back to Account 1");
        selectPage.switchAccount(selectPage.getAccountButton("oxAuth Test User"));
        assertEquals(account1SessionId, assertSessionIdCookie()); // check session_id really corresponds to Account 1
    }

    private String assertSessionIdCookie() {
        final String value = driver.manage().getCookieNamed("session_id").getValue();
        assertTrue(Strings.isNotBlank(value), "The session_id is blank");
        output("Cookie session_id: " + value);
        return value;
    }

    private void assertIdToken(String idToken) throws InvalidJwtException {
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
    }

    private AuthorizationResponse selectAccount(final String userId, final String userSecret, final String redirectUri,
                                                List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setPrompts(Lists.newArrayList(Prompt.SELECT_ACCOUNT));

        String authorizationRequestUrl = authorizationEndpoint + "?" + authorizationRequest.getQueryString();

        final SelectPage selectPage = SelectPage.navigate(pageConfig, authorizationRequestUrl);

        final String currentUrl = driver.getCurrentUrl();
        final LoginPage loginPage = selectPage.clickOnLoginAsAnotherUser();

        loginPage.enterUsername(userId);
        loginPage.enterPassword(userSecret);
        loginPage.getLoginButton().click();
        loginPage.waitForPageSwitch(currentUrl);

        String authorizationResponseStr = acceptAuthorization(driver, authorizationRequest.getRedirectUri());


        AuthorizationResponse authorizationResponse = buildAuthorizationResponse(authorizationRequest, driver, authorizationResponseStr);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        return authorizationResponse;
    }

    public AuthorizationResponse authorize(AuthorizationRequest authorizationRequest, String userId, String userSecret, int authzSteps) {
        AuthorizeClient authorizeClient = processAuthentication(driver, authorizationEndpoint, authorizationRequest,
                userId, userSecret);

        int remainAuthzSteps = authzSteps;

        String authorizationResponseStr = null;
        do {
            authorizationResponseStr = acceptAuthorization(driver, authorizationRequest.getRedirectUri());
            remainAuthzSteps--;
        } while (remainAuthzSteps >= 1);

        return buildAuthorizationResponse(authorizationRequest, driver, authorizeClient, authorizationResponseStr);
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(randomUUID());

        AuthorizationResponse authorizationResponse = authorize(authorizationRequest, userId, userSecret, 1);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        return authorizationResponse;
    }

    private RegisterResponse registerClient(String redirectUris, List<ResponseType> responseTypes, List<String> scopes, String sectorIdentifierUri) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth select accounts test app", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
        return registerResponse;
    }
}
