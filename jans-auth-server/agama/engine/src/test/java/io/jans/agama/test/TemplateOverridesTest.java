package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import org.testng.annotations.Test;

public class TemplateOverridesTest extends BaseTest {
    
    @Test
    public void override() {
        
        HtmlPage page = launch("io.jans.agama.test.ot.flow2", null);
        
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        page = (HtmlPage) doClick(button);
        
        validateFinishPage(page, true);

    }

    @Test
    public void cancellation() {
        
        HtmlPage page = launch("io.jans.agama.test.ot.flow3", null);
        
        //click on the cancellation button
        HtmlButton button = page.getForms().get(0).getButtonByName("_abort");
        page = (HtmlPage) doClick(button);
        
        validateFinishPage(page, true);

    }
    
}
