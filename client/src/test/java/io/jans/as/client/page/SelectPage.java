/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.page;

import io.jans.as.client.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SelectPage extends AbstractPage {

    public SelectPage(PageConfig config) {
        super(config);
    }

    public static SelectPage navigate(PageConfig config, String authorizationUrlWithPromptSelectAccount) {
        final SelectPage page = new SelectPage(config);
        page.navigate(authorizationUrlWithPromptSelectAccount);
        return page;
    }

    public WebElement getLoginAsAnotherUserButton() {
        return driver().findElement(By.id("selectForm:loginButton"));
    }

    public List<WebElement> getAccountButtons() {
        return driver().findElements(By.tagName("a")).stream().filter(webElement -> {
            final String onclick = webElement.getAttribute("onclick");
            return onclick != null && onclick.contains("accountButtons");
        }).collect(Collectors.toList());
    }

    public WebElement getAccountButton(String name) {
        return getAccountButtons().stream().filter(webElement -> webElement.getText().equals(name)).findFirst().get();
    }

    public LoginPage clickOnLoginAsAnotherUser() {
        final WebDriver driver = driver();

        output("Removed session_id");
        driver.manage().deleteCookieNamed("session_id"); // emulate browser

        final String previousUrl = driver.getCurrentUrl();
        output("Clicked Login as another user button");
        getLoginAsAnotherUserButton().click();
        waitForPageSwitch(previousUrl);

        navigate(driver.getCurrentUrl());
        if (BaseTest.ENABLE_REDIRECT_TO_LOGIN_PAGE) {
            new WebDriverWait(driver, PageConfig.WAIT_OPERATION_TIMEOUT)
                    .until((WebDriver d) -> !d.getCurrentUrl().contains("/authorize"));
        }
        return new LoginPage(config);
    }

    public SelectPage switchAccount(WebElement element) {
        output("Switching account to: " + element.getText());
        final String url = driver().getCurrentUrl();
        element.click();
        waitForPageSwitch(url);
        return this;
    }
}
