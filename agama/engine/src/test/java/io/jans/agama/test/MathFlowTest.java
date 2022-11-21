package io.jans.agama.test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MathFlowTest extends BaseTest {

    private static final String QNAME = "io.jans.agama.test.math";

    @Test
    public void runEmpty() {
        HtmlPage page = launch(QNAME, Collections.singletonMap("numbers",Collections.emptyList()), false);
        logger.info("Landed at {}", page.getUrl());
        assertServerError(page);
    }
    
    @Test
    public void runRandom() {
        
        int len = (int) (1 + Math.random() * 10);
        List<Integer> list = new ArrayList<>();
        
        for (int i = 0; i < len; i++) {
            list.add(1 + (int) (Math.random() * 100));
        }
        run(list);
        
    }
    
    @Test
    public void runFixed1() {
        run(Arrays.asList(30, 42, 70, 105));
    }
    
    @Test
    public void runFixed2() {
        run(Arrays.asList(6, 12, 22, 27));
    }
    
    @Test
    public void runFixed3() {
        run(Arrays.asList(1, 1, 1));
    }
            
    private void run(List<Integer> list) {
        
        HtmlPage page = launch(QNAME, Collections.singletonMap("numbers", list));
        logger.info("Landed at {}", page.getUrl());
        validateFinishPage(page, true);
        
    }
    
}
