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

public class SaySomething2FlowTest extends BaseTest {

    private static final String QNAME = "org.gluu.flow1";
    
    @Test
    public void something() {
        HtmlPage page = run("Rosamaria Montibeller");  
        validateFinishPage(page, true);
    }
    
    @Test
    public void nothing() { 
        HtmlPage page = run(null);
        validateFinishPage(page, false);
    }

    private HtmlPage run(String text) {
        
        HtmlPage page = launch(QNAME, null);

        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        page = doClick(button);
        
        assertOK(page);
        assertTextContained(page.getVisibleText(), "Gluu"); //see f1/*.ftl

        HtmlForm form = page.getForms().get(0);
        if (text != null) {
            //See f1/index.ftl
            typeInInputWithName(form, "something", text);
        }

        //click on the "Continue" button
        button = form.getInputByValue("Continue");
        return doClick(button);
        
    }

}
