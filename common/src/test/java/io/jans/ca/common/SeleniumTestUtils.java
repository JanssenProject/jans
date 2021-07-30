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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.testng.Assert.fail;

public class SeleniumTestUtils {

    private static int WAIT_OPERATION_TIMEOUT = 60;
    private static final Logger LOG = LoggerFactory.getLogger(SeleniumTestUtils.class);

    public static AuthorizationResponse authorizeClient(
            String opHost, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce, List<String> responseTypes, List<String> scopes) {
        WebDriver driver = initWebDriver(true, true);

        loginGluuServer(driver, opHost, userId, userSecret, clientId, redirectUrls, state, nonce, responseTypes, scopes);
        AuthorizationResponse authorizationResponse = acceptAuthorization(driver);

        driver.quit();
        return authorizationResponse;
    }

    private static void loginGluuServer(
            WebDriver driver, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce, List<String> responseTypes, List<String> scopes) {
        //navigate to opHost
        driver.navigate().to(getAuthorizationUrl(opHost, clientId, redirectUrls, state, nonce, responseTypes, scopes));
        //driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofSeconds(WAIT_OPERATION_TIMEOUT))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);
        WebElement loginButton = wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver d) {
                //System.out.println(d.getCurrentUrl());
                //System.out.println(d.getPageSource());
                return d.findElement(By.id("loginForm:loginButton"));
            }
        });

        LOG.info("Login page loaded. The current url is: " + driver.getCurrentUrl());
        //username field
        WebElement usernameElement = driver.findElement(By.id("loginForm:username"));
        usernameElement.sendKeys(userId);
        //password field
        WebElement passwordElement = driver.findElement(By.id("loginForm:password"));
        passwordElement.sendKeys(userSecret);
        //click on login button

        loginButton.click();

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

    }

    private static AuthorizationResponse acceptAuthorization(WebDriver driver) {
        String authorizationResponseStr = driver.getCurrentUrl();
        AuthorizationResponse authorizationResponse = null;
        // Check for authorization form if client has no persistent authorization
        if (!authorizationResponseStr.contains("#")) {
            Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                    .withTimeout(Duration.ofSeconds(WAIT_OPERATION_TIMEOUT))
                    .pollingEvery(Duration.ofMillis(500))
                    .ignoring(NoSuchElementException.class);

            WebElement allowButton = wait.until(new Function<WebDriver, WebElement>() {
                public WebElement apply(WebDriver d) {
                    //System.out.println(d.getCurrentUrl());
                    //System.out.println(d.getPageSource());
                    return d.findElement(By.id("authorizeForm:allowButton"));
                }
            });

            // We have to use JavaScript because target is link with onclick
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("scroll(0, 1000)");

            String previousURL = driver.getCurrentUrl();

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
        WebDriver currentDriver = new HtmlUnitDriver();
        ((HtmlUnitDriver) currentDriver).setJavascriptEnabled(enableJavascript);
        try {
            if (cleanupCookies) {
                currentDriver.manage().deleteAllCookies();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentDriver;
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

            /*return URLDecoder.decode(opHost + "/oxauth/restv1/authorize?" +
                    "response_type=code+id_token+token" +
                    "&state=" + state +
                    "&nonce=" + nonce +
                    "&client_id=" + clientId +
                    "&redirect_uri=" + redirectUrls.split(" ")[0] +
                    "&scope=openid+profile+oxd+uma_protection", Util.UTF8_STRING_ENCODING);*/
        } catch (UnsupportedEncodingException ex) {
            fail("Failed to decode the authorization URL.");
            return null;
        }
    }

    private static String waitForPageSwitch(WebDriver currentDriver, String previousURL) {
        Holder<String> currentUrl = new Holder<>();
        WebDriverWait wait = new WebDriverWait(currentDriver, WAIT_OPERATION_TIMEOUT);
        wait.until(d -> {
            //System.out.println("Previous url: " + previousURL);
            //System.out.println("Current url: " + d.getCurrentUrl());
            currentUrl.setT(d.getCurrentUrl());
            return !currentUrl.getT().equals(previousURL);
        });
        return currentUrl.getT();
    }
}
