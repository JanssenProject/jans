/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.page;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

/**
 * @author Yuriy Zabrovarnyy
 */
public class LoginPage extends AbstractPage {

    public LoginPage(PageConfig config) {
        super(config);
    }

    public WebElement getUsernameField() {
        return elementById("loginFormUsername");
    }

    public WebElement getPasswordField() {
        return elementById("loginFormPassword");
    }

    public WebElement getLoginButton() {
        return elementById("loginFormLoginButton");
    }

    public void enterUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return;
        }

        setFieldValue(getUsernameField(), username);
    }

    public void enterPassword(String userSecret) {
        if (StringUtils.isBlank(userSecret)) {
            return;
        }

        setFieldValue(getPasswordField(), userSecret);
    }

    // The page's document.ready handler clears both fields (remember-me logic in login.xhtml),
    // racing with sendKeys. Verify the value took and re-set it until it survives.
    private void setFieldValue(WebElement field, String value) {
        field.sendKeys(value);

        int remainAttempts = 10;
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (value.equals(field.getAttribute("value"))) {
                return;
            }

            ((JavascriptExecutor) driver()).executeScript("arguments[0].value=arguments[1];", field, value);

            remainAttempts--;
        } while (remainAttempts >= 1);
    }
}
