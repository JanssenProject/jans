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

public class SaySomethingFlowTest extends BaseTest {

    private static final String QNAME = "org.gluu.flow2";
    
    @Test
    public void nothing() {
        HtmlPage page = run(null, null);        
        validateFinishPage(page, false);
    }
    
    @Test
    public void NoOnesomething() {
        HtmlPage page = run(null, "Jans over Gluu");
        validateFinishPage(page, true);
    }
    
    @Test
    public void someOnesomething() {
        HtmlPage page = run("jgomer2001", "I like CE");
        validateFinishPage(page, true);
    }

    private HtmlPage run(String name, String text) {
        
        boolean nameEmpty = name == null;
        HtmlPage page = launch(QNAME, nameEmpty ? null : Collections.singletonMap("val", name));
        
        if (!nameEmpty) assertTextContained(page.getVisibleText(), name);

        HtmlForm form = page.getForms().get(0);
        
        if (text != null) {
            //See f1/index.ftl
            typeInInputWithName(form, "something", text);
        }
        //click on the "Continue" button
        HtmlSubmitInput button = form.getInputByValue("Continue");
        return doClick(button);
        
    }

}