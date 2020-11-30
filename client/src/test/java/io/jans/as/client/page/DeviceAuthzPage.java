/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.page;

import org.openqa.selenium.WebElement;

public class DeviceAuthzPage extends AbstractPage {

    private static final String FORM_USER_CODE_PART_1_ID = "deviceAuthzForm:userCodePart1";
    private static final String FORM_USER_CODE_PART_2_ID = "deviceAuthzForm:userCodePart2";
    private static final String FORM_CONTINUE_BUTTON_ID = "deviceAuthzForm:continueButton";

    public DeviceAuthzPage(PageConfig config) {
        super(config);
    }

    public void fillUserCode(String userCode) {
        final String[] userCodeParts = userCode.split("-");

        WebElement userCodePart1 = elementByElementId(FORM_USER_CODE_PART_1_ID);
        userCodePart1.sendKeys(userCodeParts[0]);

        WebElement userCodePart2 = elementByElementId(FORM_USER_CODE_PART_2_ID);
        userCodePart2.sendKeys(userCodeParts[1]);
    }

    public void clickContinueButton() {
        WebElement continueButton = elementByElementId(FORM_CONTINUE_BUTTON_ID);
        continueButton.click();
    }
}
