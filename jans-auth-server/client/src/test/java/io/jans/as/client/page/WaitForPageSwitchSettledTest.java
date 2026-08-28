/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Offline regression tests for {@link AbstractPage#waitForPageSwitchSettled}. No server needed:
 * a scripted driver replays the post-consent redirect chain
 * (JSF postback -> restv1/authorize -> redirect_uri) with controlled timing.
 */
public class WaitForPageSwitchSettledTest {

    private static final String PREVIOUS_URL = "https://server.example/jans-auth/authorize.htm?sid=1";
    private static final String INTERMEDIATE_URL = "https://server.example/jans-auth/restv1/authorize?sid=1&prompt=none";
    private static final String FINAL_URL_WITH_CODE = "https://server.example/jans-auth/device_authorization.htm?code=abc123&state=xyz";
    private static final String FINAL_URL_NO_PARAMS = "https://rp.example.org/postback.htm";

    /**
     * The chain pauses on an intermediate hop longer than the WebDriverWait poll interval (500ms),
     * so consecutive polls see the same intermediate URL. The wait must NOT return it: re-navigating
     * to a mid-chain URL replays already-consumed authorization state (the device flow answers
     * "Request already processed" and the test hangs until timeout).
     */
    @Test
    public void waitForPageSwitchSettled_whenSecondHopIsDelayed_shouldReturnFinalUrl() {
        WebDriver driverWithSlowSecondHop = new ScriptedUrlDriver(
                new long[]{0, AbstractPage.SETTLE_QUIET_PERIOD_MILLIS - 500},
                new String[]{INTERMEDIATE_URL, FINAL_URL_WITH_CODE});

        final String settledUrl = AbstractPage.waitForPageSwitchSettled(driverWithSlowSecondHop, PREVIOUS_URL);

        assertEquals(settledUrl, FINAL_URL_WITH_CODE);
    }

    /**
     * A final URL carrying authorization response parameters is a terminal condition:
     * no quiet-period delay is paid on the normal path.
     */
    @Test
    public void waitForPageSwitchSettled_whenUrlHasResponseParams_shouldReturnWithoutQuietPeriod() {
        WebDriver driver = new ScriptedUrlDriver(new long[]{0}, new String[]{FINAL_URL_WITH_CODE});

        final long start = System.currentTimeMillis();
        final String settledUrl = AbstractPage.waitForPageSwitchSettled(driver, PREVIOUS_URL);
        final long elapsed = System.currentTimeMillis() - start;

        assertEquals(settledUrl, FINAL_URL_WITH_CODE);
        assertTrue(elapsed < AbstractPage.SETTLE_QUIET_PERIOD_MILLIS,
                "Terminal URL must be returned without waiting for the quiet period, took " + elapsed + "ms");
    }

    /**
     * A URL matching the expected redirect uri is a terminal condition even without response parameters.
     */
    @Test
    public void waitForPageSwitchSettled_whenUrlMatchesRedirectUri_shouldReturnWithoutQuietPeriod() {
        WebDriver driver = new ScriptedUrlDriver(new long[]{0}, new String[]{FINAL_URL_NO_PARAMS});

        final long start = System.currentTimeMillis();
        final String settledUrl = AbstractPage.waitForPageSwitchSettled(driver, PREVIOUS_URL, FINAL_URL_NO_PARAMS);
        final long elapsed = System.currentTimeMillis() - start;

        assertEquals(settledUrl, FINAL_URL_NO_PARAMS);
        assertTrue(elapsed < AbstractPage.SETTLE_QUIET_PERIOD_MILLIS,
                "Redirect uri match must be returned without waiting for the quiet period, took " + elapsed + "ms");
    }

    /**
     * Fallback for flows whose final page has neither response parameters nor a known redirect uri:
     * return only after the URL kept still for the whole quiet period.
     */
    @Test
    public void waitForPageSwitchSettled_whenNoTerminalCondition_shouldWaitForQuietPeriod() {
        WebDriver driver = new ScriptedUrlDriver(new long[]{0}, new String[]{FINAL_URL_NO_PARAMS});

        final long start = System.currentTimeMillis();
        final String settledUrl = AbstractPage.waitForPageSwitchSettled(driver, PREVIOUS_URL);
        final long elapsed = System.currentTimeMillis() - start;

        assertEquals(settledUrl, FINAL_URL_NO_PARAMS);
        assertTrue(elapsed >= AbstractPage.SETTLE_QUIET_PERIOD_MILLIS,
                "URL without terminal condition must be held for the quiet period, took " + elapsed + "ms");
    }

    /**
     * Driver stub returning a URL scripted by elapsed time; only getCurrentUrl is used by the wait.
     */
    private static final class ScriptedUrlDriver implements WebDriver {

        private final long start = System.currentTimeMillis();
        private final long[] fromMillis;
        private final String[] urls;

        private ScriptedUrlDriver(long[] fromMillis, String[] urls) {
            this.fromMillis = fromMillis;
            this.urls = urls;
        }

        @Override
        public String getCurrentUrl() {
            final long elapsed = System.currentTimeMillis() - start;
            for (int i = urls.length - 1; i >= 0; i--) {
                if (elapsed >= fromMillis[i]) {
                    return urls[i];
                }
            }
            return urls[0];
        }

        @Override
        public void get(String url) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getTitle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<WebElement> findElements(By by) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WebElement findElement(By by) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPageSource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            // nothing to release
        }

        @Override
        public void quit() {
            // nothing to release
        }

        @Override
        public Set<String> getWindowHandles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWindowHandle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TargetLocator switchTo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Navigation navigate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Options manage() {
            throw new UnsupportedOperationException();
        }
    }
}
