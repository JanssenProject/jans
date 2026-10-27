/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.page;

import com.google.common.base.Preconditions;
import io.jans.as.model.common.Holder;
import io.jans.as.model.util.Util;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.Set;

import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AbstractPage implements Page {

    protected PageConfig config;

    public AbstractPage(PageConfig config) {
        Preconditions.checkNotNull(config);
        this.config = config;
    }

    public static String waitForPageSwitch(WebDriver currentDriver, String previousURL) {
        Holder<String> currentUrl = new Holder<>();
        WebDriverWait wait = new WebDriverWait(currentDriver, Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT));
        wait.until((WebDriver d) -> {
            currentUrl.setT(d.getCurrentUrl());
            return !currentUrl.getT().equals(previousURL);
        });
        return currentUrl.getT();
    }

    // Quiet period the URL must stay unchanged before a page without authorization response
    // parameters is treated as final. Only the fallback path pays it: flows whose final URL carries
    // response parameters (or matches the expected redirect uri) return immediately. Must exceed a
    // slow single hop of the redirect chain and stay well below WAIT_OPERATION_TIMEOUT.
    public static final long SETTLE_QUIET_PERIOD_MILLIS = 1000;

    // HtmlUnit follows a redirect chain (JSF postback -> restv1/authorize -> redirect_uri) in a
    // background JS thread, so the first URL change may be an intermediate hop. Terminal conditions:
    // the URL carries authorization response parameters, or starts with the expected redirect uri.
    // Fallback for flows without either: the URL kept still for SETTLE_QUIET_PERIOD_MILLIS — a single
    // pair of identical reads is not enough, a slow hop can hold the URL longer than one poll interval.
    public static String waitForPageSwitchSettled(WebDriver currentDriver, String previousURL) {
        return waitForPageSwitchSettled(currentDriver, previousURL, null);
    }

    public static String waitForPageSwitchSettled(WebDriver currentDriver, String previousURL, String expectedRedirectUri) {
        Holder<String> lastSeenUrl = new Holder<>();
        Holder<Long> lastChangeTime = new Holder<>();
        WebDriverWait wait = new WebDriverWait(currentDriver, Duration.ofSeconds(PageConfig.WAIT_OPERATION_TIMEOUT));
        wait.until((WebDriver d) -> {
            final String url = d.getCurrentUrl();
            if (!url.equals(lastSeenUrl.getT())) {
                lastSeenUrl.setT(url);
                lastChangeTime.setT(System.currentTimeMillis());
            }
            if (url.equals(previousURL)) {
                return false;
            }
            final boolean hasResponseParams = (url.contains("code=") && !url.contains("user_code"))
                    || url.contains("access_token=") || url.contains("id_token=") || url.contains("error=");
            final boolean atRedirectUri = expectedRedirectUri != null && url.startsWith(expectedRedirectUri);
            final boolean quiet = System.currentTimeMillis() - lastChangeTime.getT() >= SETTLE_QUIET_PERIOD_MILLIS;
            return hasResponseParams || atRedirectUri || quiet;
        });
        return lastSeenUrl.getT();
    }

    public static void output(String str) {
        System.out.println(str); // switch to logger?
    }

    public void navigate(String url) {
        try {
            final WebDriver driver = config.getDriver();
            output("Navigate URL: " + url);
            //printCookies();
            driver.navigate().to(URLDecoder.decode(url, Util.UTF8_STRING_ENCODING));
        } catch (UnsupportedEncodingException ex) {
            fail("Failed to decode the URL.");
        }
    }

    public void printCookies() {
        final Set<Cookie> cookies = driver().manage().getCookies();
        if (cookies == null || cookies.isEmpty()) {
            output("Cookies: no cookies");
            return;
        }

        output("Cookies: ");
        cookies.forEach(cookie -> System.out.println("        " + cookie));
    }

    public WebDriver driver() {
        return config.getDriver();
    }

    public String config(String key) {
        return config.value(key);
    }

    public WebElement elementById(String id) {
        return driver().findElement(By.id(config(id)));
    }

    public WebElement elementByElementId(String elementId) {
        return driver().findElement(By.id(elementId));
    }

    public String waitForPageSwitch(String previousUrl) {
        return waitForPageSwitch(driver(), previousUrl);
    }
}
