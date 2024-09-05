package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import org.testng.annotations.Test;

public class VariableSurvivalTest extends BaseTest {
    
    @Test
    public void agamaValues() {
        run("io.jans.agama.test.vars_and_rrf.agamaValues", 2);
    }
    
    @Test
    public void javaValues1() {
        run("io.jans.agama.test.vars_and_rrf.javaValues1", 5);
    }
    
    @Test
    public void javaValues2() {
        run("io.jans.agama.test.vars_and_rrf.javaValues2", 3);
    }
    
    private void run(String qname, int submissions) {

        HtmlPage page = launch(qname, null);
        for (int i = 0; i < submissions; i++) {
            page = moveForward(page);
        }
        validateFinishPage(page, true);

    }

    private HtmlPage moveForward(HtmlPage page) {
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        return (HtmlPage) doClick(button);
    }
    
}
