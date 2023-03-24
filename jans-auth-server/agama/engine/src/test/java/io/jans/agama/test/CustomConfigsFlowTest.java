package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.WebResponse;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CustomConfigsFlowTest extends BaseTest {

    private static final String QNAME = "io.jans.agama.test.showConfig";

    @Test
    public void withTimeout() {
        
        //Waiting 10 seconds is enough due to time skew (see FlowService#ensureTimeNotExceeded)
        HtmlPage page = launchAndWait(10);
        //Flow should have timed out now - see flow impl
        //The page currently shown may correspond to the Agama timeout template or to
        //the mismatch (not found) page in case the cleaner job already wiped the flow execution 
        
        int status = page.getWebResponse().getStatusCode();
        String text = page.getVisibleText().toLowerCase();

        if (status == WebResponse.OK) {
            //See timeout.ftlh
            assertTextContained(text, "took", "more", "expected");
        } else if (status == WebResponse.NOT_FOUND) {
            //See mismatch.ftlh
            assertTextContained(text, "not", "found");
        } else {
            fail("Unexpected status code " + status);
        }

    }
    
    @Test
    public void noTimeout() {
        HtmlPage page = launchAndWait(2);
        validateFinishPage(page, false); 
    }

    private HtmlPage launchAndWait(int wait) {

        HtmlPage page = launch(QNAME, null);
        try {
            Thread.sleep(1000L * wait);
        } catch (InterruptedException e) {
            fail(e.getMessage(), e);
        }
        
        //click on the "Continue" button
        HtmlSubmitInput button = page.getForms().get(0).getInputByValue("Continue");
        return doClick(button);
        
    }

}