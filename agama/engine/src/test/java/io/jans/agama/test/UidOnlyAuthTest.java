package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UidOnlyAuthTest extends BaseTest {

    private static final String QNAME = "io.jans.agama.test.auth.uidOnly";
    
    @BeforeClass
    public void enableJS() {
        client.getOptions().setJavaScriptEnabled(true);
    }
    
    @Test
    public void randUid() {

        HtmlPage page = start("" + Math.random());
        page = proceed(page);
        assertOK(page);

        assertTrue(page.getUrl().toString().endsWith("error.htm"));
        assertTextContained(page.getVisibleText().toLowerCase(), "failed", "authenticate");

    }
    
    @Test(dependsOnMethods = "randUid", alwaysRun = true)
    @Parameters("redirectUri")
    public void adminUid(String redirectUri) {

        HtmlPage page = start("admin");
        page = proceed(page);
        assertOK(page);
        
        if (!page.getUrl().toString().startsWith(redirectUri)) {
            //check if this is the consent page
            assertTextContained(page.getVisibleText().toLowerCase(), "permission", "allow");
        }

    }

    private HtmlPage start(String uid) {
        
        HtmlPage page = launch(QNAME, Collections.singletonMap("uid", uid));        
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        return doClick(button);

    }
    
    private HtmlPage proceed(HtmlPage page) {

        try {
            //wait for the auto-submitting javascript to execute (see finished.ftlh) and redirections to take place
            Thread.sleep(2000);
            page = (HtmlPage) client.getCurrentWindow().getEnclosedPage();
            
            logger.debug("Landed at {}",page.getUrl());
            return page;
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
        return null;

    }

}