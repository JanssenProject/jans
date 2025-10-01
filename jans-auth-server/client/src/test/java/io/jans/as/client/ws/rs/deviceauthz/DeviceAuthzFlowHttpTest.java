/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.deviceauthz;

import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.DeviceAuthzClient;
import io.jans.as.client.DeviceAuthzRequest;
import io.jans.as.client.DeviceAuthzResponse;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.page.DeviceAuthzPage;
import io.jans.as.client.page.LoginPage;
import io.jans.as.client.page.PageConfig;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Test cases for device authorization page.
 */
public class DeviceAuthzFlowHttpTest extends BaseTest {

    /**
     * Device authorization complete flow.
     */
    @Parameters({"userId", "userSecret"})
    @Test
    public void deviceAuthzFlow(final String userId, final String userSecret) throws Exception {
        showTitle("deviceAuthzFlow");

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Collections.singletonList(GrantType.DEVICE_CODE),
                null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        DeviceAuthzRequestRegistrationTest.validateSuccessfulResponse(response);

        // 3. Load device authz page, process user_code and authorization
        WebDriver currentDriver = initWebDriver(false, true);
        final PageConfig pageConfig = newPageConfig(currentDriver);
        processDeviceAuthzPutUserCodeAndPressContinue(response.getUserCode(), currentDriver, false, pageConfig);
        AuthorizationResponse authorizationResponse = processAuthorization(userId, userSecret, currentDriver);

        stopWebDriver(false, currentDriver);
        assertSuccessAuthzResponse(authorizationResponse);

        // 4. Token request
        TokenResponse tokenResponse1 = processTokens(clientId, clientSecret, response.getDeviceCode());
        validateTokenSuccessfulResponse(tokenResponse1);

        String refreshToken = tokenResponse1.getRefreshToken();
        String idToken = tokenResponse1.getIdToken();

        // 5. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullJansOpenIDConnectVersion()
                .check();

        // 6. Request new access token using the refresh token.
        TokenResponse tokenResponse2 = processNewTokenWithRefreshToken(StringUtils.implode(scopes, " "),
                refreshToken, clientId, clientSecret);
        validateTokenSuccessfulResponse(tokenResponse2);

        String accessToken = tokenResponse2.getAccessToken();

        // 7. Request user info
        processUserInfo(accessToken);
    }

    /**
     * Device authorization with access denied.
     */
    @Parameters({"userId", "userSecret"})
    @Test
    public void deviceAuthzFlowAccessDenied(final String userId, final String userSecret) throws Exception {
        showTitle("deviceAuthzFlowAccessDenied");

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Collections.singletonList(GrantType.DEVICE_CODE),
                null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        DeviceAuthzRequestRegistrationTest.validateSuccessfulResponse(response);

        // 3. Load device authz page, process user_code and authorization
        WebDriver currentDriver = initWebDriver(false, true);
        final PageConfig pageConfig = newPageConfig(currentDriver);
        AuthorizationResponse authorizationResponse = processDeviceAuthzDenyAccess(userId, userSecret,
                response.getUserCode(), currentDriver, false, pageConfig);

        validateErrorResponse(authorizationResponse, AuthorizeErrorResponseType.ACCESS_DENIED);

        // 4. Token request
        TokenResponse tokenResponse = processTokens(clientId, clientSecret, response.getDeviceCode());
        assertNotNull(tokenResponse.getErrorType(), "Error expected, however no error was found");
        assertNotNull(tokenResponse.getErrorDescription(), "Error description expected, however no error was found");
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.ACCESS_DENIED, "Unexpected error");
    }

    /**
     * Validate server denies brute forcing
     */
    @Test
    public void preventBruteForcing() throws Exception {
        showTitle("deviceAuthzFlow");

        WebDriver currentDriver = initWebDriver(false, true);
        final PageConfig pageConfig = newPageConfig(currentDriver);
        List<WebElement> list = currentDriver.findElements(By.xpath("//*[contains(text(),'Too many failed attemps')]"));
        byte limit = 10;
        while (list.size() == 0 && limit > 0) {
            processDeviceAuthzPutUserCodeAndPressContinue("ABCD-ABCD", currentDriver, false, pageConfig);
            Thread.sleep(500);
            list = currentDriver.findElements(By.xpath("//*[contains(text(),'Too many failed attemps')]"));
            limit--;
        }
        stopWebDriver(false, currentDriver);
        assertTrue(list.size() > 0 && limit > 0, "Brute forcing prevention not working correctly.");
    }

    /**
     * Verifies that token endpoint should return slow down or authorization pending states when token is in process.
     */
    @Test
    public void checkSlowDownOrPendingState() throws Exception {
        showTitle("checkSlowDownOrPendingState");

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Collections.singletonList(GrantType.DEVICE_CODE),
                null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        DeviceAuthzRequestRegistrationTest.validateSuccessfulResponse(response);

        byte count = 3;
        while (count > 0) {
            TokenResponse tokenResponse = processTokens(clientId, clientSecret, response.getDeviceCode());
            assertNotNull(tokenResponse.getErrorType(), "Error expected, however no error was found");
            assertNotNull(tokenResponse.getErrorDescription(), "Error description expected, however no error was found");
            assertTrue(tokenResponse.getErrorType() == TokenErrorResponseType.AUTHORIZATION_PENDING
                    || tokenResponse.getErrorType() == TokenErrorResponseType.SLOW_DOWN, "Unexpected error");
            Thread.sleep(200);
            count--;
        }
    }

    /**
     * Attempts to get token with a wrong device_code, after that it attempts to get token twice,
     * second one should be rejected.
     */
    @Parameters({"userId", "userSecret"})
    @Test
    public void attemptDifferentFailedValuesToTokenEndpoint(final String userId, final String userSecret) throws Exception {
        showTitle("deviceAuthzFlow");

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Collections.singletonList(GrantType.DEVICE_CODE),
                null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        DeviceAuthzRequestRegistrationTest.validateSuccessfulResponse(response);

        // 3. Load device authz page, process user_code and authorization
        WebDriver currentDriver = initWebDriver(false, true);
        final PageConfig pageConfig = newPageConfig(currentDriver);
        processDeviceAuthzPutUserCodeAndPressContinue(response.getUserCode(), currentDriver, false, pageConfig);
        AuthorizationResponse authorizationResponse = processAuthorization(userId, userSecret, currentDriver);

        stopWebDriver(false, currentDriver);
        assertSuccessAuthzResponse(authorizationResponse);

        // 4. Token request with a wrong device code
        String wrongDeviceCode = "WRONG" + response.getDeviceCode();
        TokenResponse tokenResponse1 = processTokens(clientId, clientSecret, wrongDeviceCode);
        assertNotNull(tokenResponse1.getErrorType(), "Error expected, however no error was found");
        assertNotNull(tokenResponse1.getErrorDescription(), "Error description expected, however no error was found");
        assertEquals(tokenResponse1.getErrorType(), TokenErrorResponseType.EXPIRED_TOKEN, "Unexpected error");

        // 5. Token request with a right device code value
        tokenResponse1 = processTokens(clientId, clientSecret, response.getDeviceCode());
        validateTokenSuccessfulResponse(tokenResponse1);

        // 6. Try to get token again, however this should be rejected by the server
        tokenResponse1 = processTokens(clientId, clientSecret, response.getDeviceCode());
        assertNotNull(tokenResponse1.getErrorType(), "Error expected, however no error was found");
        assertNotNull(tokenResponse1.getErrorDescription(), "Error description expected, however no error was found");
        assertEquals(tokenResponse1.getErrorType(), TokenErrorResponseType.EXPIRED_TOKEN, "Unexpected error");
    }

    /**
     * Process a complete device authorization flow using verification_uri_complete
     */
    @Parameters({"userId", "userSecret"})
    @Test
    public void deviceAuthzFlowWithCompleteVerificationUri(final String userId, final String userSecret) throws Exception {
        showTitle("deviceAuthzFlow");

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Collections.singletonList(GrantType.DEVICE_CODE),
                null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        DeviceAuthzRequestRegistrationTest.validateSuccessfulResponse(response);

        // 3. Load device authz page, process user_code and authorization
        WebDriver currentDriver = initWebDriver(false, true);
        final PageConfig pageConfig = newPageConfig(currentDriver);
        processDeviceAuthzPutUserCodeAndPressContinue(response.getUserCode(), currentDriver, true, pageConfig);
        AuthorizationResponse authorizationResponse = processAuthorization(userId, userSecret, currentDriver);

        stopWebDriver(false, currentDriver);
        assertSuccessAuthzResponse(authorizationResponse);

        // 4. Token request
        TokenResponse tokenResponse1 = processTokens(clientId, clientSecret, response.getDeviceCode());
        validateTokenSuccessfulResponse(tokenResponse1);

        String refreshToken = tokenResponse1.getRefreshToken();
        String idToken = tokenResponse1.getIdToken();

        // 5. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullJansOpenIDConnectVersion()
                .check();

        // 6. Request new access token using the refresh token.
        TokenResponse tokenResponse2 = processNewTokenWithRefreshToken(StringUtils.implode(scopes, " "),
                refreshToken, clientId, clientSecret);
        validateTokenSuccessfulResponse(tokenResponse2);

        String accessToken = tokenResponse2.getAccessToken();

        // 7. Request user info
        processUserInfo(accessToken);
    }

    /**
     * Device authorization with access denied and using complete verification uri.
     */
    @Parameters({"userId", "userSecret"})
    @Test
    public void deviceAuthzFlowAccessDeniedWithCompleteVerificationUri(final String userId, final String userSecret) throws Exception {
        showTitle("deviceAuthzFlowAccessDenied");

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Collections.singletonList(GrantType.DEVICE_CODE),
                null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        DeviceAuthzRequestRegistrationTest.validateSuccessfulResponse(response);

        // 3. Load device authz page, process user_code and authorization
        WebDriver currentDriver = initWebDriver(false, true);
        final PageConfig pageConfig = newPageConfig(currentDriver);
        AuthorizationResponse authorizationResponse = processDeviceAuthzDenyAccess(userId, userSecret,
                response.getUserCode(), currentDriver, true, pageConfig);

        validateErrorResponse(authorizationResponse, AuthorizeErrorResponseType.ACCESS_DENIED);

        // 4. Token request
        TokenResponse tokenResponse = processTokens(clientId, clientSecret, response.getDeviceCode());
        assertNotNull(tokenResponse.getErrorType(), "Error expected, however no error was found");
        assertNotNull(tokenResponse.getErrorDescription(), "Error description expected, however no error was found");
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.ACCESS_DENIED, "Unexpected error");
    }

    private void processUserInfo(String accessToken) throws UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.BIRTHDATE, JwtClaimName.GENDER, JwtClaimName.MIDDLE_NAME)
                .claimsPresence(JwtClaimName.NICKNAME, JwtClaimName.PREFERRED_USERNAME, JwtClaimName.PROFILE)
                .claimsPresence(JwtClaimName.WEBSITE, JwtClaimName.EMAIL_VERIFIED, JwtClaimName.PHONE_NUMBER)
                .claimsPresence(JwtClaimName.PHONE_NUMBER_VERIFIED, JwtClaimName.ADDRESS, JwtClaimName.USER_NAME)
                .claimsNoPresence("org_name", "work_phone")
                .check();
    }

    private TokenResponse processNewTokenWithRefreshToken(String scopes, String refreshToken, String clientId,
                                                          String clientSecret) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        tokenClient2.setExecutor(clientEngine(true));
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scopes, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        AssertBuilder.tokenResponse(tokenResponse2)
                .notNullRefreshToken()
                .notNullScope()
                .check();
        return tokenResponse2;
    }

    private TokenResponse processTokens(String clientId, String clientSecret, String deviceCode) {
        TokenRequest tokenRequest = new TokenRequest(GrantType.DEVICE_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setDeviceCode(deviceCode);

        TokenClient tokenClient1 = newTokenClient(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);

        return tokenResponse1;
    }

    private void validateTokenSuccessfulResponse(TokenResponse tokenResponse) {
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    private void assertSuccessAuthzResponse(final AuthorizationResponse authorizationResponse) {
        assertNotNull(authorizationResponse.getCode());
        assertNotNull(authorizationResponse.getState());
        assertNull(authorizationResponse.getErrorType());
    }

    private void processDeviceAuthzPutUserCodeAndPressContinue(String userCode, WebDriver currentDriver,
                                                               boolean complete, final PageConfig pageConfig) {
        String deviceAuthzPageUrl = deviceAuthzEndpoint.replace("/restv1/device_authorization", "/device_authorization.htm")
                + (complete ? "?user_code=" + userCode : "");
        output("Device authz flow: page to navigate to put user_code:" + deviceAuthzPageUrl);

        navigateToAuhorizationUrl(currentDriver, deviceAuthzPageUrl);

        DeviceAuthzPage deviceAuthzPage = new DeviceAuthzPage(pageConfig);
        if (!complete) {
            deviceAuthzPage.fillUserCode(userCode);
        }

        deviceAuthzPage.clickContinueButton();
    }

    private AuthorizationResponse processAuthorization(String userId, String userSecret, WebDriver currentDriver) {
        Wait<WebDriver> wait = new FluentWait<>(currentDriver)
                .withTimeout(Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);

        if (userSecret != null) {
            final String previousUrl = currentDriver.getCurrentUrl();
            WebElement loginButton = wait.until(d -> d.findElement(By.id(loginFormLoginButton)));

            if (userId != null) {
                WebElement usernameElement = currentDriver.findElement(By.id(loginFormUsername));
                usernameElement.sendKeys(userId);
            }

            WebElement passwordElement = currentDriver.findElement(By.id(loginFormPassword));
            passwordElement.sendKeys(userSecret);

            loginButton.click();

            if (ENABLE_REDIRECT_TO_LOGIN_PAGE) {
                waitForPageSwitch(currentDriver, previousUrl);
            }
        }

        String authorizationResponseStr = acceptAuthorization(currentDriver, null);
        navigateToAuhorizationUrl(currentDriver, authorizationResponseStr);

        String deviceAuthzResponseStr = currentDriver.getCurrentUrl();

        output("Device authz redirection response url: " + deviceAuthzResponseStr);
        return new AuthorizationResponse(deviceAuthzResponseStr);
    }

    private AuthorizationResponse processDeviceAuthzDenyAccess(String userId, String userSecret, String userCode,
                                                               WebDriver currentDriver, boolean complete,
                                                               final PageConfig pageConfig) {
        String deviceAuthzPageUrl = deviceAuthzEndpoint.replace("/restv1/device_authorization", "/device_authorization.htm")
                + (complete ? "?user_code=" + userCode : "");
        output("Device authz flow: page to navigate to put user_code:" + deviceAuthzPageUrl);

        navigateToAuhorizationUrl(currentDriver, deviceAuthzPageUrl);

        DeviceAuthzPage deviceAuthzPage = new DeviceAuthzPage(pageConfig);

        if (!complete) {
            deviceAuthzPage.fillUserCode(userCode);
        }
        deviceAuthzPage.clickContinueButton();

        Wait<WebDriver> wait = new FluentWait<WebDriver>(currentDriver)
                .withTimeout(Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);

        if (userSecret != null) {
            final String previousUrl = currentDriver.getCurrentUrl();

            WebElement loginButton = wait.until(d -> currentDriver.findElement(By.id(loginFormLoginButton)));

            LoginPage loginPage = new LoginPage(pageConfig);
            loginPage.enterUsername(userId);
            loginPage.enterPassword(userSecret);

            loginButton.click();

            if (ENABLE_REDIRECT_TO_LOGIN_PAGE) {
                waitForPageSwitch(currentDriver, previousUrl);
            }
        }

        denyAuthorization(currentDriver);

        String deviceAuthzResponseStr = currentDriver.getCurrentUrl();
        stopWebDriver(false, currentDriver);

        output("Device authz redirection response url: " + deviceAuthzResponseStr);
        return new AuthorizationResponse(deviceAuthzResponseStr);
    }

    protected void denyAuthorization(WebDriver currentDriver) {
        String authorizationResponseStr = currentDriver.getCurrentUrl();

        // Check for authorization form if client has no persistent authorization
        if (!authorizationResponseStr.contains("#")) {
            Wait<WebDriver> wait = new FluentWait<>(currentDriver)
                    .withTimeout(Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT))
                    .pollingEvery(Duration.ofMillis(500))
                    .ignoring(NoSuchElementException.class);

            WebElement doNotAllowButton = wait.until(d -> currentDriver.findElement(By.id(authorizeFormDoNotAllowButton)));
            final String previousUrl2 = currentDriver.getCurrentUrl();
            doNotAllowButton.click();
            waitForPageSwitch(currentDriver, previousUrl2);
        } else {
            fail("The authorization form was expected to be shown.");
        }
    }

    protected void validateErrorResponse(AuthorizationResponse response, AuthorizeErrorResponseType errorType) {
        assertNotNull(response.getErrorType(), "Error expected, however no error was found");
        assertNotNull(response.getErrorDescription(), "Error description expected, however no error was found");
        assertEquals(response.getErrorType(), errorType, "Unexpected error");
    }

}