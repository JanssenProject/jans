package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class InexistentFlowTest extends BaseTest {
    
    @Test
    public void test() {
        HtmlPage page = launch("flow" + Math.random(), null);
        assertFalse(page.getUrl().toString().endsWith(".fls"));
    }

}
