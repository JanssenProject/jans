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

@org.testng.annotations.Ignore
public class UidOnlyAuthTest extends BaseTest {

    private static final String QNAME = "io.jans.agama.test.auth.uidOnly";
    
    @BeforeClass
    public void enableJS() {
        client.getOptions().setJavaScriptEnabled(true);
    }
    
    @Test
    public void randUid() {
        HtmlPage page = run("" + Math.random());        
        validateFinishPage(page, false);
    }

    private HtmlPage run(String uid) {
        
        HtmlPage page = launch(QNAME, Collections.singletonMap("uid", uid));        
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        return doClick(button);

    }

}