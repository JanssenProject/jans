package io.jans.as.client.ws.rs.acr;

import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.page.DeviceAuthzPage;
import io.jans.as.client.page.PageConfig;
import io.jans.as.client.ws.rs.deviceauthz.DeviceAuthzRequestRegistrationTest;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.model.util.StringUtils.EASY_TO_READ_CHARACTERS;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
public class AcrChangedHttpTest extends BaseTest {

    /**
     * Acr change complete flow.
     */
    @Parameters({"userId", "userSecret"})
    @Test
    public void authzFlow(final String userId, final String userSecret) throws Exception {
        showTitle("AcrChangedHttpTest.authzFlow");

        final String redirectUri = this.deviceAuthzEndpoint;

        // 1. Init device authz request from WS
        RegisterResponse registerResponse = DeviceAuthzRequestRegistrationTest.registerClientForDeviceAuthz(
                AuthenticationMethod.CLIENT_SECRET_BASIC, Lists.newArrayList(GrantType.DEVICE_CODE, GrantType.AUTHORIZATION_CODE),
                redirectUri, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Device request registration
        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scopes);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(this.deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(deviceAuthzRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateSuccessfulResponse(response);

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

        String idToken = tokenResponse1.getIdToken();

        // 5. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullJansOpenIDConnectVersion()
                .check();

        // 6. CHANGE ACR to basic
        String nonce = UUID.randomUUID().toString();
        authorizationResponse = requestBasicAuthorization(userId, userSecret, redirectUri, Lists.newArrayList(ResponseType.CODE), scopes, clientId, nonce);

        String authorizationCode = authorizationResponse.getCode();

        // 7. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = newTokenClient(tokenRequest);
        tokenClient1.setRequest(tokenRequest);
        tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .check();
    }

    private AuthorizationResponse requestBasicAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAcrValues(Lists.newArrayList("basic"));

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
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
            output("filled user code successfully");
        }

        deviceAuthzPage.clickContinueButton();
        output("Clicked continue button");
    }

    private AuthorizationResponse processAuthorization(String userId, String userSecret, WebDriver currentDriver) {
        try {
            Wait<WebDriver> wait = new FluentWait<>(currentDriver)
                    .withTimeout(Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT))
                    .pollingEvery(Duration.ofMillis(1500))
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
        } catch (TimeoutException e) {
            output("currentUrl: " + currentDriver.getCurrentUrl());
            output("sourceCode: " + currentDriver.getPageSource());
            throw e;
        }
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
                .check();
    }

    private void assertSuccessAuthzResponse(final AuthorizationResponse authorizationResponse) {
        assertNotNull(authorizationResponse.getCode());
        assertNotNull(authorizationResponse.getState());
        assertNull(authorizationResponse.getErrorType());
    }

    protected static void validateSuccessfulResponse(DeviceAuthzResponse response) {
        final String regex = "[" + EASY_TO_READ_CHARACTERS + "]{4}-[" + EASY_TO_READ_CHARACTERS + "]{4}";
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getUserCode(), "User code is null");
        assertNotNull(response.getDeviceCode(), "Device code is null");
        assertNotNull(response.getInterval(), "Interval is null");
        assertTrue(response.getInterval() > 0, "Interval is null");
        assertNotNull(response.getVerificationUri(), "Verification Uri is null");
        assertNotNull(response.getVerificationUriComplete(), "Verification Uri complete is null");
        assertTrue(response.getVerificationUri().length() > 10, "Invalid verification_uri");
        assertTrue(response.getVerificationUriComplete().length() > 10, "Invalid verification_uri_complete");
        assertNotNull(response.getExpiresIn(), "expires_in is null");
        assertTrue(response.getExpiresIn() > 0, "expires_in contains an invalid value");
        assertTrue(response.getUserCode().matches(regex));
    }

}
