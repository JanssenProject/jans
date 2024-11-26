/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.page;

import org.apache.commons.lang3.StringUtils;
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

        getUsernameField().sendKeys(username);
    }

    public void enterPassword(String userSecret) {
        if (StringUtils.isBlank(userSecret)) {
            return;
        }

        getPasswordField().sendKeys(userSecret);
    }
}
