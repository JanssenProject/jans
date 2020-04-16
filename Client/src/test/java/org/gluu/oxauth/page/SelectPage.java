package org.gluu.oxauth.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SelectPage extends AbstractPage {

    public SelectPage(PageConfig config) {
        super(config);
    }

    public WebElement getLoginAsAnotherUserButton() {
        return driver().findElement(By.id("selectForm:loginButton"));
    }

    public LoginPage clickOnLoginAsAnotherUser() {
        final String currentUrl = driver().getCurrentUrl();
        getLoginAsAnotherUserButton().click();
        waitForPageSwitch(driver(), currentUrl);
        return new LoginPage(config);
    }

    public static SelectPage navigate(PageConfig config, String authorizationUrlWithPromptSelectAccount) {
        final SelectPage page = new SelectPage(config);
        page.navigate(authorizationUrlWithPromptSelectAccount);
        return page;
    }
}
