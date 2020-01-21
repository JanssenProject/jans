package org.gluu.oxd.common;

import org.gluu.oxauth.model.common.Holder;
import org.gluu.oxauth.model.util.Util;
import org.openqa.selenium.*;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.testng.Assert.fail;

public class SeleniumUtils {

    private static int WAIT_OPERATION_TIMEOUT = 60;
    private static final Logger LOG = LoggerFactory.getLogger(SeleniumUtils.class);

    public static void authorizeClient(String opHost, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce) {
        WebDriver driver = initWebDriver(true, true);

        loginGluuServer(driver, opHost, userId, userSecret, clientId, redirectUrls, state, nonce);
        acceptAuthorization(driver);

        driver.quit();
    }

    public static void loginGluuServer(WebDriver driver, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce) {
        //navigate to opHost
        driver.navigate().to(getAuthorizationUrl(opHost, clientId, redirectUrls, state, nonce));
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

    private static void acceptAuthorization(WebDriver driver) {
        String authorizationResponseStr = driver.getCurrentUrl();

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

            waitForPageSwitch(driver, previousURL);

            authorizationResponseStr = driver.getCurrentUrl();
            LOG.info("Authorization Response url is: " + driver.getCurrentUrl());
        } else {
            fail("The authorization form was expected to be shown.");
        }
    }

    private static WebDriver initWebDriver(boolean enableJavascript, boolean cleanupCookies) {
        HtmlUnitDriver currentDriver = new HtmlUnitDriver(enableJavascript);
        try {
            if (cleanupCookies) {
                currentDriver.manage().deleteAllCookies();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentDriver;
    }

    private static String getAuthorizationUrl(String opHost, String clientId, String redirectUrls, String state, String nonce) {
        try {
            return URLDecoder.decode(opHost + "/oxauth/restv1/authorize?" +
                    "response_type=code+id_token+token" +
                    "&state=" + state +
                    "&nonce=" + nonce +
                    "&client_id=" + clientId +
                    "&redirect_uri=" + redirectUrls.split(" ")[0] +
                    "&scope=openid+profile+oxd+uma_protection", Util.UTF8_STRING_ENCODING);
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
