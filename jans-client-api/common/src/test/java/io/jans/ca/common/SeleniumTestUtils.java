package io.jans.ca.common;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.model.common.Holder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.util.Util;
import org.apache.commons.collections.CollectionUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Lists;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testng.Assert.fail;

public class SeleniumTestUtils {

    private static int WAIT_OPERATION_TIMEOUT = 60;
    private static final Logger LOG = LoggerFactory.getLogger(SeleniumTestUtils.class);

    public static AuthorizationResponse authorizeClient(
            String opHost, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce, List<String> responseTypes, List<String> scopes) {
        WebDriver currentDriver = initWebDriver(true, true);

        loginGluuServer(currentDriver, opHost, userId, userSecret, clientId, redirectUrls, state, nonce, responseTypes, scopes);
        AuthorizationResponse authorizationResponse = acceptAuthorization(currentDriver);

        currentDriver.close();
        currentDriver.quit();
        return authorizationResponse;
    }

    private static void loginGluuServer(
            WebDriver driver, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce, List<String> responseTypes, List<String> scopes) {
        //navigate to opHost
        String authorizationUrl = getAuthorizationUrl(opHost, clientId, redirectUrls, state, nonce, responseTypes, scopes);

        driver.navigate().to(authorizationUrl);

        LOG.info("Login page loaded. The current url is: " + authorizationUrl);

        WebElement loginButton = waitForRequredElementLoad(driver, "loginForm:loginButton");
        if (userId != null) {
            setWebElementValue(driver, "loginForm:username", userId);
        }
        setWebElementValue(driver, "loginForm:password", userSecret);

        loginButton.click();
        waitForPageSwitch(driver, authorizationUrl);

        if (driver.getPageSource().contains("Failed to authenticate.")) {
            fail("Failed to authenticate user");
        }
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    private static void setWebElementValue(WebDriver currentDriver, String elemnetId, String value) {
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

    private static WebElement waitForRequredElementLoad(WebDriver currentDriver, String id) {
        Wait<WebDriver> wait = new FluentWait<>(currentDriver)
                .withTimeout(Duration.ofSeconds(WAIT_OPERATION_TIMEOUT))
                .pollingEvery(Duration.ofMillis(1000))
                .ignoring(NoSuchElementException.class);

        WebElement loginButton = wait.until(d -> {
            return d.findElement(By.id(id));
        });
        return loginButton;
    }

    private static AuthorizationResponse acceptAuthorization(WebDriver driver) {
        String authorizationResponseStr = driver.getCurrentUrl();
        AuthorizationResponse authorizationResponse = null;
        // Check for authorization form if client has no persistent authorization
        if (!authorizationResponseStr.contains("#")) {
            WebElement allowButton = waitForRequredElementLoad(driver, "authorizeForm:allowButton");

            // We have to use JavaScript because target is link with onclick
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("scroll(0, 1000)");

            Actions actions = new Actions(driver);
            actions.click(allowButton).perform();

            authorizationResponseStr = driver.getCurrentUrl();
            authorizationResponse = new AuthorizationResponse(authorizationResponseStr);

            LOG.info("Authorization Response url is: " + driver.getCurrentUrl());
        } else {
            fail("The authorization form was expected to be shown.");
        }
        return authorizationResponse;
    }

    private static WebDriver initWebDriver(boolean enableJavascript, boolean cleanupCookies) {
        WebDriver webDriver = new HtmlUnitDriver(enableJavascript);
        try {
            if (cleanupCookies) {
                webDriver.manage().deleteAllCookies();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return webDriver;
    }

    private static String getAuthorizationUrl(String opHost, String clientId, String redirectUrls, String state, String nonce, List<String> responseTypes, List<String> scopes) {
        try {
            if(CollectionUtils.isEmpty(responseTypes)) {
                responseTypes = Lists.newArrayList("code", "id_token", "token");
            }

            if(CollectionUtils.isEmpty(scopes)) {
                scopes = Lists.newArrayList("openid", "profile", "jans_client_api", "uma_protection");
            }
            List<ResponseType> resTypes = responseTypes.stream().map(item -> ResponseType.fromString(item)).collect(Collectors.toList());
            AuthorizationRequest authorizationRequest = new AuthorizationRequest(resTypes, clientId, scopes, redirectUrls.split(" ")[0], nonce);
            authorizationRequest.setResponseTypes(responseTypes.stream().map(item -> ResponseType.fromString(item)).collect(Collectors.toList()));
            authorizationRequest.setState(state);

            return URLDecoder.decode(opHost + "/jans-auth/restv1/authorize?" +authorizationRequest.getQueryString(), Util.UTF8_STRING_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            fail("Failed to decode the authorization URL.");
            return null;
        }
    }

    private static String waitForPageSwitch(WebDriver currentDriver, String previousURL) {
        Holder<String> currentUrl = new Holder<>();
        WebDriverWait wait = new WebDriverWait(currentDriver, Duration.ofSeconds(WAIT_OPERATION_TIMEOUT));
        wait.until(d -> {
            currentUrl.setT(d.getCurrentUrl());
            return !currentUrl.getT().equals(previousURL);
        });
        return currentUrl.getT();
    }
}
