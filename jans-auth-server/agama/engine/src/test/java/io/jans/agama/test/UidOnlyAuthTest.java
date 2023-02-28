package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import java.util.Collections;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UidOnlyAuthTest extends BaseTest {

    private static final String QNAME = "io.jans.agama.test.auth.uidOnly";

    @Test
    public void randUid() {

        HtmlPage page = (HtmlPage) start("" + Math.random());
        assertOK(page);
        assertTrue(page.getUrl().toString().endsWith("error.htm"));
        assertTextContained(page.getVisibleText().toLowerCase(), "failed", "authenticate");

    }

    @Test(dependsOnMethods = "randUid", alwaysRun = true)
    @Parameters("redirectUri")
    public void adminUid(String redirectUri) {

        Page page = start("admin");
        assertOK(page);
        assertTrue(page.getUrl().toString().startsWith(redirectUri));

    }

    private Page start(String uid) {
        
        HtmlPage page = launch(QNAME, Collections.singletonMap("uid", uid));        
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");

        page = (HtmlPage) doClick(button);
        //click on the "Continue" button (at finish template)
        button = page.getForms().get(0).getInputByValue("Continue");
        return doClick(button);

    }

}