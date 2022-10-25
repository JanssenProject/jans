package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import java.util.Collections;

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

        start("" + Math.random());
        HtmlPage page = (HtmlPage) currentPageAfter(2000);

        assertOK(page);
        assertTrue(page.getUrl().toString().endsWith("error.htm"));
        assertTextContained(page.getVisibleText().toLowerCase(), "failed", "authenticate");

    }

    @Test(dependsOnMethods = "randUid", alwaysRun = true)
    @Parameters("redirectUri")
    public void adminUid(String redirectUri) {

        start("admin");
        Page page = currentPageAfter(2000);

        assertOK(page);
        assertTrue(page.getUrl().toString().startsWith(redirectUri));

    }

    private HtmlPage start(String uid) {
        
        HtmlPage page = launch(QNAME, Collections.singletonMap("uid", uid));        
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        return doClick(button);

    }
    
    private Page currentPageAfter(long wait) {

        try {
            //wait for the auto-submitting javascript to execute (see finished.ftlh) and redirections to take place
            Thread.sleep(wait);
            Page p = client.getCurrentWindow().getEnclosedPage();
            
            logger.debug("Landed at {}", p.getUrl());
            return p;
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
        return null;

    }

}