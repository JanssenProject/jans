package org.gluu.oxauth.page;

import org.apache.commons.lang.StringUtils;
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
