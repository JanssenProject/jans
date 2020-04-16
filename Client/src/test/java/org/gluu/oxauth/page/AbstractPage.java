package org.gluu.oxauth.page;

import com.google.common.base.Preconditions;
import org.gluu.oxauth.model.common.Holder;
import org.gluu.oxauth.model.util.Util;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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

    public void navigate(String url) {
        try {
            System.out.println("navigate: " + url);
            config.getDriver().navigate().to(URLDecoder.decode(url, Util.UTF8_STRING_ENCODING));
        } catch (UnsupportedEncodingException ex) {
            fail("Failed to decode the URL.");
        }
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

    public String waitForPageSwitch(String previousUrl) {
        return waitForPageSwitch(driver(), previousUrl);
    }

    public static String waitForPageSwitch(WebDriver currentDriver, String previousURL) {
        Holder<String> currentUrl = new Holder<>();
        WebDriverWait wait = new WebDriverWait(currentDriver, PageConfig.WAIT_OPERATION_TIMEOUT);
        wait.until(d -> {
            //System.out.println("Previous url: " + previousURL);
            //System.out.println("Current url: " + d.getCurrentUrl());
            currentUrl.setT(d.getCurrentUrl());
            return !currentUrl.getT().equals(previousURL);
        });
        return currentUrl.getT();
    }
}
