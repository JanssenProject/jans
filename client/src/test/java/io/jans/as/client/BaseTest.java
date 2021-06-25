/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import com.google.common.collect.Maps;
import io.jans.as.client.dev.HostnameVerifierType;
import io.jans.as.client.page.AbstractPage;
import io.jans.as.client.page.PageConfig;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.error.IErrorType;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.as.model.util.Util;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public abstract class BaseTest {

    public static final boolean ENABLE_REDIRECT_TO_LOGIN_PAGE = StringHelper.toBoolean(System.getProperty("gluu.enable-redirect", "false"), false);

    protected HtmlUnitDriver driver;

    protected String authorizationEndpoint;
    protected String authorizationPageEndpoint;
    protected String gluuConfigurationEndpoint;
    protected String tokenEndpoint;
    protected String tokenRevocationEndpoint;
    protected String userInfoEndpoint;
    protected String clientInfoEndpoint;
    protected String checkSessionIFrame;
    protected String endSessionEndpoint;
    protected String jwksUri;
    protected String registrationEndpoint;
    protected String configurationEndpoint;
    protected String idGenEndpoint;
    protected String introspectionEndpoint;
    protected String deviceAuthzEndpoint;
    protected String backchannelAuthenticationEndpoint;
    protected String revokeSessionEndpoint;
    protected String parEndpoint;
    protected Map<String, List<String>> scopeToClaimsMapping;
    protected String issuer;

    protected Map<String, String> allTestKeys = Maps.newHashMap();

    // Form Interaction
    protected String loginFormUsername;
    protected String loginFormPassword;
    protected String loginFormLoginButton;
    private String authorizeFormAllowButton;
    protected String authorizeFormDoNotAllowButton;

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws IOException {
        SecurityProviderUtility.installBCProvider();

        Reporter.log("Invoked init test suite method \n", true);

        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        if (StringHelper.isEmpty(propertiesFile)) {
            propertiesFile = "target/test-classes/testng.properties";
        }

        FileInputStream conf = new FileInputStream(propertiesFile);
        Properties prop = new Properties();
        prop.load(conf);

        Map<String, String> parameters = new HashMap<>();
        for (Entry<Object, Object> entry : prop.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (StringHelper.isEmptyString(key) || StringHelper.isEmptyString(value)) {
                continue;
            }
            parameters.put(key.toString(), value.toString());
        }

        // Overrided test paramters
        context.getSuite().getXmlSuite().setParameters(parameters);
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getTokenRevocationEndpoint() {
        return tokenRevocationEndpoint;
    }

    public void setTokenRevocationEndpoint(String tokenRevocationEndpoint) {
        this.tokenRevocationEndpoint = tokenRevocationEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public String getClientInfoEndpoint() {
        return clientInfoEndpoint;
    }

    public void setClientInfoEndpoint(String clientInfoEndpoint) {
        this.clientInfoEndpoint = clientInfoEndpoint;
    }

    public String getCheckSessionIFrame() {
        return checkSessionIFrame;
    }

    public void setCheckSessionIFrame(String checkSessionIFrame) {
        this.checkSessionIFrame = checkSessionIFrame;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String p_introspectionEndpoint) {
        introspectionEndpoint = p_introspectionEndpoint;
    }

    public String getParEndpoint() {
        return parEndpoint;
    }

    public void setParEndpoint(String parEndpoint) {
        this.parEndpoint = parEndpoint;
    }

    public String getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public String getRevokeSessionEndpoint() {
        return revokeSessionEndpoint;
    }

    public void setRevokeSessionEndpoint(String revokeSessionEndpoint) {
        this.revokeSessionEndpoint = revokeSessionEndpoint;
    }

    public Map<String, List<String>> getScopeToClaimsMapping() {
        return scopeToClaimsMapping;
    }

    public void setScopeToClaimsMapping(Map<String, List<String>> p_scopeToClaimsMapping) {
        scopeToClaimsMapping = p_scopeToClaimsMapping;
    }

    public String getIdGenEndpoint() {
        return idGenEndpoint;
    }

    public void setIdGenEndpoint(String p_idGenEndpoint) {
        idGenEndpoint = p_idGenEndpoint;
    }

    public String getConfigurationEndpoint() {
        return configurationEndpoint;
    }

    public void setConfigurationEndpoint(String configurationEndpoint) {
        this.configurationEndpoint = configurationEndpoint;
    }

    public void startSelenium() {
        //System.setProperty("webdriver.chrome.driver", "/Users/JAVIER/tmp/chromedriver");
        //driver = new ChromeDriver();

        //driver = new SafariDriver();

        //driver = new FirefoxDriver();

        //driver = new InternetExplorerDriver();

        driver = new HtmlUnitDriver(true);
    }

    public void stopSelenium() {
//        driver.close();
        driver.quit();
        driver = null;
    }

    /**
     * The authorization server authenticates the resource owner (via the user-agent)
     * and establishes whether the resource owner grants or denies the client's access request.
     */
    public AuthorizationResponse authenticateResourceOwnerAndGrantAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest, String userId, String userSecret) {
        return authenticateResourceOwnerAndGrantAccess(authorizeUrl, authorizationRequest, userId, userSecret, true);
    }

    /**
     * The authorization server authenticates the resource owner (via the user-agent)
     * and establishes whether the resource owner grants or denies the client's access request.
     */
    public AuthorizationResponse authenticateResourceOwnerAndGrantAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest, String userId, String userSecret, boolean cleanupCookies) {
        return authenticateResourceOwnerAndGrantAccess(authorizeUrl, authorizationRequest, userId, userSecret, cleanupCookies, true);
    }

    /**
     * The authorization server authenticates the resource owner (via the user-agent)
     * and establishes whether the resource owner grants or denies the client's access request.
     */
    public AuthorizationResponse authenticateResourceOwnerAndGrantAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest, String userId, String userSecret,
            boolean cleanupCookies, boolean useNewDriver) {
        return authenticateResourceOwnerAndGrantAccess(authorizeUrl, authorizationRequest, userId, userSecret, cleanupCookies, useNewDriver, 1);
    }

    /**
     * The authorization server authenticates the resource owner (via the user-agent)
     * and establishes whether the resource owner grants or denies the client's access request.
     */
    public AuthorizationResponse authenticateResourceOwnerAndGrantAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest, String userId, String userSecret,
            boolean cleanupCookies, boolean useNewDriver, int authzSteps) {
        WebDriver currentDriver = initWebDriver(useNewDriver, cleanupCookies);

        try {
            AuthorizeClient authorizeClient = processAuthentication(currentDriver, authorizeUrl, authorizationRequest,
                    userId, userSecret);

            int remainAuthzSteps = authzSteps;

            String authorizationResponseStr = null;
            do {
                authorizationResponseStr = acceptAuthorization(currentDriver, authorizationRequest.getRedirectUri());
                remainAuthzSteps--;
            } while (remainAuthzSteps >= 1);

            AuthorizationResponse authorizationResponse = buildAuthorizationResponse(authorizationRequest,
                    currentDriver, authorizeClient, authorizationResponseStr);
            return authorizationResponse;
        } finally {
            stopWebDriver(useNewDriver, currentDriver);
        }
    }

    protected WebDriver initWebDriver(boolean useNewDriver, boolean cleanupCookies) {
        // Allow to run test in multi thread mode
    	HtmlUnitDriver currentDriver;
        if (useNewDriver) {
            currentDriver = new HtmlUnitDriver(true);
        } else {
            startSelenium();
            currentDriver = driver;
            if (cleanupCookies) {
                System.out.println("authenticateResourceOwnerAndGrantAccess: Cleaning cookies");
                deleteAllCookies();
            }
        }

        return currentDriver;
    }

    protected void stopWebDriver(boolean useNewDriver, WebDriver currentDriver) {
        if (useNewDriver) {
            currentDriver.close();
            currentDriver.quit();
        } else {
            stopSelenium();
        }
    }

    protected AuthorizeClient processAuthentication(WebDriver currentDriver, String authorizeUrl,
                                                  AuthorizationRequest authorizationRequest, String userId, String userSecret) {
        String authorizationRequestUrl = authorizeUrl + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(authorizationRequest);

        System.out.println("authenticateResourceOwnerAndGrantAccess: authorizationRequestUrl:" + authorizationRequestUrl);

        navigateToAuhorizationUrl(currentDriver, authorizationRequestUrl);
        if (userSecret != null) {
            final String previousUrl = currentDriver.getCurrentUrl();

			WebElement loginButton = waitForRequredElementLoad(currentDriver, loginFormLoginButton);

			if (userId != null) {
				setWebElementValue(currentDriver, loginFormUsername, userId);
			}

			setWebElementValue(currentDriver, loginFormPassword, userSecret);

			loginButton.click();

			if (ENABLE_REDIRECT_TO_LOGIN_PAGE) {
				waitForPageSwitch(currentDriver, previousUrl);
			}

			if (currentDriver.getPageSource().contains("Failed to authenticate.")) {
				fail("Failed to authenticate user");
			}
        }

        return authorizeClient;
    }

	private void setWebElementValue(WebDriver currentDriver, String elemnetId, String value) {
		WebElement webElement = currentDriver.findElement(By.id(elemnetId));
		webElement.sendKeys(value);

		int remainAttempts = 10;
		do {
            if (value.equals(webElement.getAttribute("value"))) {
            	break;
            }
            
            ((JavascriptExecutor) currentDriver).executeScript("arguments[0].value='" + value + "';", webElement);

            try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            
            remainAttempts--;
		} while (remainAttempts >= 1);
	}

	private WebElement waitForRequredElementLoad(WebDriver currentDriver, String id) {
		Wait<WebDriver> wait = new FluentWait<>(currentDriver)
		        .withTimeout(Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT))
				.pollingEvery(Duration.ofMillis(1000))
		        .ignoring(NoSuchElementException.class);

		WebElement loginButton = wait.until(d -> {
		    return d.findElement(By.id(id));
		});
		return loginButton;
	}

	protected String acceptAuthorization(WebDriver currentDriver, String redirectUri) {
		String authorizationResponseStr = currentDriver.getCurrentUrl();

		// Check for authorization form if client has no persistent authorization
		if (!authorizationResponseStr.contains("#")) {
            WebElement allowButton = waitForRequredElementLoad(currentDriver, authorizeFormAllowButton);

			// We have to use JavaScript because target is link with onclick
			JavascriptExecutor jse = (JavascriptExecutor) currentDriver;
			jse.executeScript("scroll(0, 1000)");

            String previousURL = currentDriver.getCurrentUrl();

			Actions actions = new Actions(currentDriver);
			actions.click(allowButton).perform();

            waitForPageSwitch(currentDriver, previousURL);

            authorizationResponseStr = currentDriver.getCurrentUrl();

            if (redirectUri != null && !authorizationResponseStr.startsWith(redirectUri)) {
                navigateToAuhorizationUrl(currentDriver, authorizationResponseStr);
                authorizationResponseStr = waitForPageSwitch(currentDriver, authorizationResponseStr);
            }

            if (redirectUri == null && !authorizationResponseStr.contains("code=")) { // corner case for redirect_uri = null
                navigateToAuhorizationUrl(currentDriver, authorizationResponseStr);
                authorizationResponseStr = waitForPageSwitch(currentDriver, authorizationResponseStr);
            }
		} else {
			fail("The authorization form was expected to be shown.");
		}

		return authorizationResponseStr;
	}

	public String waitForPageSwitch(String previousUrl) {
        return waitForPageSwitch(driver, previousUrl);
    }

    public static String waitForPageSwitch(WebDriver driver, String previousUrl) {
        return AbstractPage.waitForPageSwitch(driver, previousUrl);
    }

    protected AuthorizationResponse buildAuthorizationResponse(AuthorizationRequest authorizationRequest,
                                                               WebDriver currentDriver,
                                                               String authorizationResponseStr) {
        return buildAuthorizationResponse(authorizationRequest, currentDriver, null, authorizationResponseStr);
    }

    protected AuthorizationResponse buildAuthorizationResponse(AuthorizationRequest authorizationRequest,
                                                             WebDriver currentDriver, @Nullable AuthorizeClient authorizeClient,
                                                             String authorizationResponseStr) {
        final WebDriver.Options options = currentDriver.manage();
        Cookie sessionStateCookie = options.getCookieNamed("session_state");
        Cookie sessionIdCookie = options.getCookieNamed("session_id");

        if (sessionStateCookie != null) {
            System.out.println("authenticateResourceOwnerAndGrantAccess: sessionState:" + sessionStateCookie.getValue());
        }
        if (sessionIdCookie != null) {
            System.out.println("authenticateResourceOwnerAndGrantAccess: sessionId:" + sessionIdCookie.getValue());
        }

        AuthorizationResponse authorizationResponse = new AuthorizationResponse(authorizationResponseStr);
        if (authorizationRequest.getRedirectUri() != null && authorizationRequest.getRedirectUri().equals(authorizationResponseStr)) {
            authorizationResponse.setResponseMode(ResponseMode.FORM_POST);
        }
        if (authorizeClient != null) {
            authorizeClient.setResponse(authorizationResponse);
            showClientUserAgent(authorizeClient);
        }

        return authorizationResponse;
    }

    public AuthorizationResponse authenticateResourceOwnerAndDenyAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest, String userId, String userSecret) {
        String authorizationRequestUrl = authorizeUrl + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(authorizationRequest);

        System.out.println("authenticateResourceOwnerAndDenyAccess: authorizationRequestUrl:" + authorizationRequestUrl);
        startSelenium();
        navigateToAuhorizationUrl(driver, authorizationRequestUrl);

        WebElement usernameElement = driver.findElement(By.id(loginFormUsername));
        WebElement passwordElement = driver.findElement(By.id(loginFormPassword));
        WebElement loginButton = driver.findElement(By.id(loginFormLoginButton));

        if (userId != null) {
            usernameElement.sendKeys(userId);
        }
        passwordElement.sendKeys(userSecret);

        String previousUrl = driver.getCurrentUrl();
        loginButton.click();

        if (ENABLE_REDIRECT_TO_LOGIN_PAGE) {
            waitForPageSwitch(driver, previousUrl);
        }

        String authorizationResponseStr = driver.getCurrentUrl();

        WebElement doNotAllowButton = driver.findElement(By.id(authorizeFormDoNotAllowButton));

        final String previousUrl2 = driver.getCurrentUrl();
        doNotAllowButton.click();
        waitForPageSwitch(driver, previousUrl2);

        authorizationResponseStr = driver.getCurrentUrl();

        Cookie sessionIdCookie = driver.manage().getCookieNamed("session_id");
        String sessionId = null;
        if (sessionIdCookie != null) {
            sessionId = sessionIdCookie.getValue();
        }
        System.out.println("authenticateResourceOwnerAndDenyAccess: sessionId:" + sessionId);

        stopSelenium();

        AuthorizationResponse authorizationResponse = new AuthorizationResponse(authorizationResponseStr);
        if (authorizationRequest.getRedirectUri() != null && authorizationRequest.getRedirectUri().equals(authorizationResponseStr)) {
            authorizationResponse.setResponseMode(ResponseMode.FORM_POST);
        }
        authorizationResponse.setSessionId(sessionId);
        authorizeClient.setResponse(authorizationResponse);
        showClientUserAgent(authorizeClient);

        return authorizationResponse;
    }

    public AuthorizationResponse authorizationRequestAndGrantAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest) {
        String authorizationRequestUrl = authorizeUrl + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(authorizationRequest);

        System.out.println("authorizationRequestAndGrantAccess: authorizationRequestUrl:" + authorizationRequestUrl);
        startSelenium();
        navigateToAuhorizationUrl(driver, authorizationRequestUrl);

        String authorizationResponseStr = driver.getCurrentUrl();

        WebElement allowButton = driver.findElement(By.id(authorizeFormAllowButton));

        final String previousURL = driver.getCurrentUrl();
        allowButton.click();

        waitForPageSwitch(previousURL);

        authorizationResponseStr = driver.getCurrentUrl();

        if (!authorizationResponseStr.startsWith(authorizationRequest.getRedirectUri())) {
            navigateToAuhorizationUrl(driver, authorizationResponseStr);
            authorizationResponseStr = waitForPageSwitch(authorizationResponseStr);
        }

        Cookie sessionStateCookie = driver.manage().getCookieNamed("session_state");
        String sessionState = null;
        if (sessionStateCookie != null) {
            sessionState = sessionStateCookie.getValue();
        }
        System.out.println("authorizationRequestAndGrantAccess: sessionState:" + sessionState);

        stopSelenium();

        AuthorizationResponse authorizationResponse = new AuthorizationResponse(authorizationResponseStr);
        if (authorizationRequest.getRedirectUri() != null && authorizationRequest.getRedirectUri().equals(authorizationResponseStr)) {
            authorizationResponse.setResponseMode(ResponseMode.FORM_POST);
        }
        authorizeClient.setResponse(authorizationResponse);
        showClientUserAgent(authorizeClient);

        return authorizationResponse;
    }

    public AuthorizationResponse authorizationRequestAndDenyAccess(
            String authorizeUrl, AuthorizationRequest authorizationRequest) {
        String authorizationRequestUrl = authorizeUrl + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(authorizationRequest);

        System.out.println("authorizationRequestAndDenyAccess: authorizationRequestUrl:" + authorizationRequestUrl);
        startSelenium();
        navigateToAuhorizationUrl(driver, authorizationRequestUrl);

        WebElement doNotAllowButton = driver.findElement(By.id(authorizeFormDoNotAllowButton));

        final String previousURL = driver.getCurrentUrl();
        doNotAllowButton.click();
        WebDriverWait wait = new WebDriverWait(driver, 1);
        wait.until((WebDriver d) -> (d.getCurrentUrl() != previousURL));

        String authorizationResponseStr = driver.getCurrentUrl();

        Cookie sessionStateCookie = driver.manage().getCookieNamed("session_state");
        String sessionState = null;
        if (sessionStateCookie != null) {
            sessionState = sessionStateCookie.getValue();
        }
        System.out.println("authorizationRequestAndDenyAccess: sessionState:" + sessionState);

        stopSelenium();

        AuthorizationResponse authorizationResponse = new AuthorizationResponse(authorizationResponseStr);
        if (authorizationRequest.getRedirectUri() != null && authorizationRequest.getRedirectUri().equals(authorizationResponseStr)) {
            authorizationResponse.setResponseMode(ResponseMode.FORM_POST);
        }
        authorizeClient.setResponse(authorizationResponse);
        showClientUserAgent(authorizeClient);

        return authorizationResponse;
    }

    /**
     * The authorization server authenticates the resource owner (via the user-agent)
     * No authorization page.
     */
    public AuthorizationResponse authenticateResourceOwner(
            String authorizeUrl, AuthorizationRequest authorizationRequest, String userId, String userSecret, boolean cleanupCookies) {
        String authorizationRequestUrl = authorizeUrl + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(authorizationRequest);

        System.out.println("authenticateResourceOwner: authorizationRequestUrl:" + authorizationRequestUrl);
        startSelenium();
        if (cleanupCookies) {
            System.out.println("authenticateResourceOwner: Cleaning cookies");
            deleteAllCookies();
        }

        navigateToAuhorizationUrl(driver, authorizationRequestUrl);

        if (userSecret != null) {
            if (userId != null) {
                WebElement usernameElement = driver.findElement(By.id(loginFormUsername));
                usernameElement.sendKeys(userId);
            }

            WebElement passwordElement = driver.findElement(By.id(loginFormPassword));
            passwordElement.sendKeys(userSecret);

            WebElement loginButton = driver.findElement(By.id(loginFormLoginButton));

            loginButton.click();

            navigateToAuhorizationUrl(driver, driver.getCurrentUrl());

            new WebDriverWait(driver, PageConfig.WAIT_OPERATION_TIMEOUT)
                    .until(webDriver ->!webDriver.getCurrentUrl().contains("/authorize"));
        }

        String authorizationResponseStr = driver.getCurrentUrl();

        Cookie sessionStateCookie = driver.manage().getCookieNamed("session_state");
        String sessionState = null;
        if (sessionStateCookie != null) {
            sessionState = sessionStateCookie.getValue();
        }
        System.out.println("authenticateResourceOwner: sessionState:" + sessionState + ", url:" + authorizationResponseStr);

        stopSelenium();

        AuthorizationResponse authorizationResponse = new AuthorizationResponse(authorizationResponseStr);
        if (authorizationRequest.getRedirectUri() != null && authorizationRequest.getRedirectUri().equals(authorizationResponseStr)) {
            authorizationResponse.setResponseMode(ResponseMode.FORM_POST);
        }
        authorizeClient.setResponse(authorizationResponse);
        showClientUserAgent(authorizeClient);

        return authorizationResponse;
    }

    /**
     * Try to open login form (via the user-agent)
     */
    public String waitForResourceOwnerAndGrantLoginForm(
            String authorizeUrl, AuthorizationRequest authorizationRequest, boolean cleanupCookies) {
        String authorizationRequestUrl = authorizeUrl + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(authorizationRequest);

        System.out.println("waitForResourceOwnerAndGrantLoginForm: authorizationRequestUrl:" + authorizationRequestUrl);
        startSelenium();
        if (cleanupCookies) {
            System.out.println("waitForResourceOwnerAndGrantLoginForm: Cleaning cookies");
            deleteAllCookies();
        }
        navigateToAuhorizationUrl(driver, authorizationRequestUrl);

        WebElement usernameElement = driver.findElement(By.id(loginFormUsername));
        WebElement passwordElement = driver.findElement(By.id(loginFormPassword));
        WebElement loginButton = driver.findElement(By.id(loginFormLoginButton));

        if ((usernameElement == null) || (passwordElement == null) || (loginButton == null)) {
            return null;
        }

        Cookie sessionStateCookie = driver.manage().getCookieNamed("session_state");
        String sessionState = null;
        if (sessionStateCookie != null) {
            sessionState = sessionStateCookie.getValue();
        }
        System.out.println("waitForResourceOwnerAndGrantLoginForm: sessionState:" + sessionState);

        stopSelenium();

        showClientUserAgent(authorizeClient);

        return sessionState;
    }

    /**
     * Try to open login form (via the user-agent)
     */
    public String waitForResourceOwnerAndGrantLoginForm(
            String authorizeUrl, AuthorizationRequest authorizationRequest) {
        return waitForResourceOwnerAndGrantLoginForm(authorizeUrl, authorizationRequest, true);
    }

    private void deleteAllCookies() {
        try {
            driver.manage().deleteAllCookies();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeTest
    public void discovery(ITestContext context) throws Exception {
        // Load Form Interaction
        loginFormUsername = context.getCurrentXmlTest().getParameter("loginFormUsername");
        loginFormPassword = context.getCurrentXmlTest().getParameter("loginFormPassword");
        loginFormLoginButton = context.getCurrentXmlTest().getParameter("loginFormLoginButton");
        authorizeFormAllowButton = context.getCurrentXmlTest().getParameter("authorizeFormAllowButton");
        authorizeFormDoNotAllowButton = context.getCurrentXmlTest().getParameter("authorizeFormDoNotAllowButton");
        allTestKeys = Maps.newHashMap(context.getCurrentXmlTest().getAllParameters());

        String resource = context.getCurrentXmlTest().getParameter("swdResource");

        if (StringUtils.isNotBlank(resource)) {

            showTitle("OpenID Connect Discovery");

            OpenIdConnectDiscoveryClient openIdConnectDiscoveryClient = new OpenIdConnectDiscoveryClient(resource);
            OpenIdConnectDiscoveryResponse openIdConnectDiscoveryResponse = openIdConnectDiscoveryClient.exec(clientExecutor(true));

            showClient(openIdConnectDiscoveryClient);
            assertEquals(openIdConnectDiscoveryResponse.getStatus(), 200, "Unexpected response code");
            assertNotNull(openIdConnectDiscoveryResponse.getSubject());
            assertTrue(openIdConnectDiscoveryResponse.getLinks().size() > 0);

            configurationEndpoint = openIdConnectDiscoveryResponse.getLinks().get(0).getHref() +
                    "/.well-known/openid-configuration";

            System.out.println("OpenID Connect Configuration");

            OpenIdConfigurationClient client = new OpenIdConfigurationClient(configurationEndpoint);
            client.setExecutor(clientExecutor(true));
            OpenIdConfigurationResponse response = client.execOpenIdConfiguration();

            showClient(client);
            assertEquals(response.getStatus(), 200, "Unexpected response code");
            assertNotNull(response.getIssuer(), "The issuer is null");
            assertNotNull(response.getAuthorizationEndpoint(), "The authorizationEndpoint is null");
            assertNotNull(response.getTokenEndpoint(), "The tokenEndpoint is null");
            assertNotNull(response.getRevocationEndpoint(), "The revocationEndpoint is null");
            assertNotNull(response.getUserInfoEndpoint(), "The userInfoEndPoint is null");
            assertNotNull(response.getJwksUri(), "The jwksUri is null");
            assertNotNull(response.getRegistrationEndpoint(), "The registrationEndpoint is null");

            assertTrue(response.getScopesSupported().size() > 0, "The scopesSupported is empty");
            assertTrue(response.getScopeToClaimsMapping().size() > 0, "The scope to claims mapping is empty");
            assertTrue(response.getResponseTypesSupported().size() > 0, "The responseTypesSupported is empty");
            assertTrue(response.getGrantTypesSupported().size() > 0, "The grantTypesSupported is empty");
            assertTrue(response.getAcrValuesSupported().size() >= 0, "The acrValuesSupported is empty");
            assertTrue(response.getSubjectTypesSupported().size() > 0, "The subjectTypesSupported is empty");
            assertTrue(response.getIdTokenSigningAlgValuesSupported().size() > 0, "The idTokenSigningAlgValuesSupported is empty");
            assertTrue(response.getRequestObjectSigningAlgValuesSupported().size() > 0, "The requestObjectSigningAlgValuesSupported is empty");
            assertTrue(response.getTokenEndpointAuthMethodsSupported().size() > 0, "The tokenEndpointAuthMethodsSupported is empty");
            assertTrue(response.getClaimsSupported().size() > 0, "The claimsSupported is empty");

            authorizationEndpoint = response.getAuthorizationEndpoint();
            tokenEndpoint = response.getTokenEndpoint();
            tokenRevocationEndpoint = response.getRevocationEndpoint();
            userInfoEndpoint = response.getUserInfoEndpoint();
            clientInfoEndpoint = response.getClientInfoEndpoint();
            checkSessionIFrame = response.getCheckSessionIFrame();
            endSessionEndpoint = response.getEndSessionEndpoint();
            jwksUri = response.getJwksUri();
            registrationEndpoint = response.getRegistrationEndpoint();
            idGenEndpoint = response.getIdGenerationEndpoint();
            introspectionEndpoint = response.getIntrospectionEndpoint();
            parEndpoint = response.getParEndpoint();
            deviceAuthzEndpoint = response.getDeviceAuthzEndpoint();
            backchannelAuthenticationEndpoint = response.getBackchannelAuthenticationEndpoint();
            revokeSessionEndpoint = response.getSessionRevocationEndpoint();
            scopeToClaimsMapping = response.getScopeToClaimsMapping();
            gluuConfigurationEndpoint = determineGluuConfigurationEndpoint(openIdConnectDiscoveryResponse.getLinks().get(0).getHref());
            issuer = response.getIssuer();
        } else {
            showTitle("Loading configuration endpoints from properties file");

            authorizationEndpoint = context.getCurrentXmlTest().getParameter("authorizationEndpoint");
            tokenEndpoint = context.getCurrentXmlTest().getParameter("tokenEndpoint");
            tokenRevocationEndpoint = context.getCurrentXmlTest().getParameter("tokenRevocationEndpoint");
            userInfoEndpoint = context.getCurrentXmlTest().getParameter("userInfoEndpoint");
            clientInfoEndpoint = context.getCurrentXmlTest().getParameter("clientInfoEndpoint");
            checkSessionIFrame = context.getCurrentXmlTest().getParameter("checkSessionIFrame");
            endSessionEndpoint = context.getCurrentXmlTest().getParameter("endSessionEndpoint");
            jwksUri = context.getCurrentXmlTest().getParameter("jwksUri");
            registrationEndpoint = context.getCurrentXmlTest().getParameter("registrationEndpoint");
            configurationEndpoint = context.getCurrentXmlTest().getParameter("configurationEndpoint");
            idGenEndpoint = context.getCurrentXmlTest().getParameter("idGenEndpoint");
            introspectionEndpoint = context.getCurrentXmlTest().getParameter("introspectionEndpoint");
            parEndpoint = context.getCurrentXmlTest().getParameter("parEndpoint");
            backchannelAuthenticationEndpoint = context.getCurrentXmlTest().getParameter("backchannelAuthenticationEndpoint");
            revokeSessionEndpoint = context.getCurrentXmlTest().getParameter("revokeSessionEndpoint");
            scopeToClaimsMapping = new HashMap<String, List<String>>();
            issuer = context.getCurrentXmlTest().getParameter("issuer");
        }

        authorizationPageEndpoint = determineAuthorizationPageEndpoint(authorizationEndpoint);
    }

    private String determineAuthorizationPageEndpoint(String authorizationEndpoint) {
        return authorizationEndpoint.replace("/restv1/authorize.htm", "/authorize");
    }

    private String determineGluuConfigurationEndpoint(String host) {
        return host + "/jans-auth/restv1/gluu-configuration";
    }

    public void showTitle(String title) {
        String testTitle = "TEST: " + title;

        System.out.println("#######################################################");
        System.out.println(testTitle);
        System.out.println("#######################################################");
    }

    public static void showClient(BaseClient client) {
        ClientUtils.showClient(client);
    }

    public static void showClient(BaseClient client, CookieStore cookieStore) {
        ClientUtils.showClient(client, cookieStore);
    }

    public static void showClientUserAgent(BaseClient client) {
        ClientUtils.showClientUserAgent(client);
    }

    public static void assertErrorResponse(BaseResponseWithErrors p_response, IErrorType p_errorType) {
        assertEquals(p_response.getStatus(), 400, "Unexpected response code. Entity: " + p_response.getEntity());
        assertNotNull(p_response.getEntity(), "The entity is null");
        assertEquals(p_response.getErrorType(), p_errorType);
        assertTrue(StringUtils.isNotBlank(p_response.getErrorDescription()));
    }

    public static CloseableHttpClient createHttpClient() {
        return createHttpClient(HostnameVerifierType.DEFAULT);
    }

	public static CloseableHttpClient createHttpClient(HostnameVerifierType p_verifierType) {
        if (p_verifierType == HostnameVerifierType.ALLOW_ALL) {
            return HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
        }

        return HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
        		.build();
    }

    public static ClientExecutor clientExecutor() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        return clientExecutor(false);
    }

    public static ClientExecutor clientExecutor(boolean trustAll) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        if (trustAll) {
            return new ApacheHttpClient4Executor(createHttpClientTrustAll());
        }
        return ClientRequest.getDefaultExecutor();
    }

    public static ClientHttpEngine clientEngine() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        return clientEngine(false);
    }

    public static ClientHttpEngine clientEngine(boolean trustAll) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        if (trustAll) {
            return new ApacheHttpClient4Engine(createAcceptSelfSignedCertificateClient());
        }
        return new ApacheHttpClient4Engine(createClient());
    }

    public static HttpClient createClient() {
	    return createClient(null);
	}

    public static HttpClient createAcceptSelfSignedCertificateClient()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLConnectionSocketFactory connectionFactory = createAcceptSelfSignedSocketFactory();

	    return createClient(connectionFactory);
    }

	private static HttpClient createClient(SSLConnectionSocketFactory connectionFactory) {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		HttpClientBuilder httClientBuilder = HttpClients.custom();
		if (connectionFactory != null) {
			httClientBuilder = httClientBuilder.setSSLSocketFactory(connectionFactory);
		}

		HttpClient httpClient = httClientBuilder
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
	    		.setConnectionManager(cm).build();
	    cm.setMaxTotal(200); // Increase max total connection to 200
	    cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

	    return httpClient;
	}

	private static SSLConnectionSocketFactory createAcceptSelfSignedSocketFactory()
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		// Use the TrustSelfSignedStrategy to allow Self Signed Certificates
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();

        // We can optionally disable hostname verification.
        // If you don't want to further weaken the security, you don't have to include this.
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

        // Create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
        // and allow all hosts verifier.
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

        return connectionFactory;
	}

	public static CloseableHttpClient createHttpClientTrustAll()
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				return true;
			}
		}).build();
		SSLConnectionSocketFactory sslContextFactory = new SSLConnectionSocketFactory(sslContext);
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				.setSSLSocketFactory(sslContextFactory)
				.setRedirectStrategy(new LaxRedirectStrategy()).build();

		return httpclient;
	}

	protected void navigateToAuhorizationUrl(WebDriver driver, String authorizationRequestUrl) {
		try {
			driver.navigate().to(URLDecoder.decode(authorizationRequestUrl, Util.UTF8_STRING_ENCODING));
		} catch (UnsupportedEncodingException ex) {
            fail("Failed to decode the authorization URL.");
		}
	}

	private ClientExecutor getClientExecutor() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return clientExecutor(true);
    }

	protected RegisterClient newRegisterClient(RegisterRequest request) {
        try {
            final RegisterClient client = new RegisterClient(registrationEndpoint);
            client.setRequest(request);
            client.setExecutor(getClientExecutor());
            return client;
        } catch (Exception e) {
            throw new AssertionError("Failed to create register client");
        }
    }

    protected RevokeSessionClient newRevokeSessionClient(RevokeSessionRequest request) {
        try {
            final RevokeSessionClient client = new RevokeSessionClient(revokeSessionEndpoint);
            client.setRequest(request);
            client.setExecutor(getClientExecutor());
            return client;
        } catch (Exception e) {
            throw new AssertionError("Failed to create register client");
        }
    }

    protected UserInfoClient newUserInfoClient(UserInfoRequest request) {
        try {
            final UserInfoClient client = new UserInfoClient(userInfoEndpoint);
            client.setRequest(request);
            client.setExecutor(getClientExecutor());
            return client;
        } catch (Exception e) {
            throw new AssertionError("Failed to create userinfo client");
        }
    }

    protected UserInfoResponse requestUserInfo(String accessToken) {
        try {
            final UserInfoClient client = new UserInfoClient(userInfoEndpoint);
            client.setExecutor(getClientExecutor());
            final UserInfoResponse userInfoResponse = client.execUserInfo(accessToken);
            showClient(client);
            return userInfoResponse;
        } catch (Exception e) {
            throw new AssertionError("Failed to request userinfo");
        }
    }

    protected AuthorizeClient newAuthorizeClient(AuthorizationRequest request) {
        try {
            final AuthorizeClient client = new AuthorizeClient(authorizationEndpoint);
            client.setRequest(request);
            client.setExecutor(getClientExecutor());
            return client;
        } catch (Exception e) {
            throw new AssertionError("Failed to create authorize client");
        }
    }

    protected TokenClient newTokenClient(TokenRequest request) {
        try {
            final TokenClient client = new TokenClient(tokenEndpoint);
            client.setRequest(request);
            client.setExecutor(getClientExecutor());
            return client;
        } catch (Exception e) {
            throw new AssertionError("Failed to create token client");
        }
    }

    protected PageConfig newPageConfig(WebDriver driver) {
        PageConfig config = new PageConfig(driver);
        config.getTestKeys().putAll(allTestKeys);
        return config;
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static void output(String str) {
        System.out.println(str); // switch to logger?
    }

    public static AbstractCryptoProvider createCryptoProviderWithAllowedNone() throws Exception {
        return new AuthCryptoProvider(null, null, null, false);
    }
}
