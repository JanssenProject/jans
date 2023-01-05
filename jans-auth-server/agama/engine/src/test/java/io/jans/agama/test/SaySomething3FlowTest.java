package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SaySomething3FlowTest extends BaseTest {

    private static final String QNAME = "org.gluu.flow3";

    @Test
    public void test() {
        HtmlPage page = launch(QNAME, null);

        //click on the "Go" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Go");
        page = doClick(button);
        
        assertOK(page);
        assertTextContained(page.getVisibleText(), "Agama"); //see me/myindex.ftl and f1/index2.ftl
        
        button = page.getForms().get(0).getInputByValue("Continue");
        page = doClick(button);
        
        validateFinishPage(page, false);

    }
    
}