package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import java.util.Collections;
import java.net.URL;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UidOnlyAuthTest extends BaseTest {

    private static final String QNAME = "io.jans.agama.test.auth.uidOnly";

    @Test
    @Parameters("redirectUri")
    public void randUid(String redirectUri) {

        Page page = start("" + Math.random());
        assertOK(page);

        URL url = page.getUrl();
        //It is assumed errorHandlingMethod=remote in auth-server config, thus
        //the RP handles the error - the built-in AS error page is not shown
        assertTrue(url.toString().startsWith(redirectUri));        
        assertTrue(url.getQuery().toString().contains("error="));

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