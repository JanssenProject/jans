package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.testng.annotations.Test;

import java.util.Collections;

public class InvocationsTest extends BaseTest {

    @Test
    public void run1() {        
        run("io.jans.agama.test.invocations");
    }

    @Test
    public void run2() {        
        run("io.jans.agama.test.invocations2");
    }

    @Test
    public void run3() {        
        run("io.jans.agama.test.invocations3");
    }

    public void run(String fqname) {
        
        HtmlPage page = launch(fqname, Collections.emptyMap());
        validateFinishPage(page, true);
    }
    
}