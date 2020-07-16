/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ws.rs.deviceauthz;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.oxauth.page.PageConfig;
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

import static org.testng.Assert.*;

/**
 * Test cases for device authorization page.
 */
public class DeviceAuthzFlowHttpTest extends BaseTest {

    private static final String FORM_USER_CODE_PART_1_ID = "deviceAuthzForm:userCodePart1";
    private static final String FORM_USER_CODE_PART_2_ID = "deviceAuthzForm:userCodePart2";
    private static final String FORM_CONTINUE_BUTTON_ID = "deviceAuthzForm:continueButton";

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

        final String[] userCodeParts = response.getUserCode().split("-");

        // 3. Load device authz page, process user_code and authorization
        AuthorizationResponse authorizationResponse = processDeviceAuthz(userId, userSecret, userCodeParts);

        assertSuccessAuthzResponse(authorizationResponse);

        // 4. Token request
        TokenResponse tokenResponse1 = processTokens(clientId, clientSecret, response.getDeviceCode());

        String refreshToken = tokenResponse1.getRefreshToken();
        String idToken = tokenResponse1.getIdToken();

        // 5. Validate id_token
        verifyIdToken(idToken);

        // 6. Request new access token using the refresh token.
        TokenResponse tokenResponse2 = processNewTokenWithRefreshToken(StringUtils.implode(scopes, " "),
                refreshToken, clientId, clientSecret);

        String accessToken = tokenResponse2.getAccessToken();

        // 7. Request user info
        processUserInfo(accessToken);
    }

    private void processUserInfo(String accessToken) throws UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientExecutor(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse.getClaim("org_name"));
        assertNull(userInfoResponse.getClaim("work_phone"));
    }

    private TokenResponse processNewTokenWithRefreshToken(String scopes, String refreshToken, String clientId,
                                                          String clientSecret) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        tokenClient2.setExecutor(clientExecutor(true));
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scopes, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse2.getScope(), "The scope is null");

        return tokenResponse2;
    }

    private void verifyIdToken(String idToken) throws InvalidJwtException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID), clientExecutor(true));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

        assertTrue(rsaSigner.validate(jwt));
    }

    private TokenResponse processTokens(String clientId, String clientSecret, String deviceCode) {
        TokenRequest tokenRequest = new TokenRequest(GrantType.DEVICE_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);;
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setDeviceCode(deviceCode);

        TokenClient tokenClient1 = newTokenClient(tokenRequest);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        return tokenResponse1;
    }

    private void assertSuccessAuthzResponse(final AuthorizationResponse authorizationResponse) {
        assertNotNull(authorizationResponse.getCode());
        assertNotNull(authorizationResponse.getState());
        assertNull(authorizationResponse.getErrorType());
    }

    private AuthorizationResponse processDeviceAuthz(String userId, String userSecret, String[] userCodeParts) {
        WebDriver currentDriver = initWebDriver(false, true);

        String deviceAuthzPageUrl = deviceAuthzEndpoint.replace("/restv1/device_authorization", "/device_authorization.htm");
        System.out.println("Device authz flow: page to navigate to put user_code:" + deviceAuthzPageUrl);

        navigateToAuhorizationUrl(currentDriver, deviceAuthzPageUrl);

        WebElement userCodePart1 = currentDriver.findElement(By.id(FORM_USER_CODE_PART_1_ID));
        userCodePart1.sendKeys(userCodeParts[0]);

        WebElement userCodePart2 = currentDriver.findElement(By.id(FORM_USER_CODE_PART_2_ID));
        userCodePart2.sendKeys(userCodeParts[1]);

        WebElement continueButton = currentDriver.findElement(By.id(FORM_CONTINUE_BUTTON_ID));
        continueButton.click();

        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);

        if (userSecret != null) {
            final String previousUrl = currentDriver.getCurrentUrl();
            WebElement loginButton = wait.until(d -> currentDriver.findElement(By.id(loginFormLoginButton)));

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

        acceptAuthorization(currentDriver, null);

        String deviceAuthzResponseStr = currentDriver.getCurrentUrl();
        stopWebDriver(false, currentDriver);

        System.out.println("Device authz redirection response url: " + deviceAuthzResponseStr);
        return new AuthorizationResponse(deviceAuthzResponseStr);
    }

}