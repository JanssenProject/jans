package io.jans.agama.test;

import org.testng.annotations.Test;

public class CrashedFlowsTest extends BaseTest {
    
    private static final String QNAME = "io.jans.agama.test.broken";
    
    @Test
    public void subflows() {
        
        for (int i = 0; i < 3; i++) { 
            assertServerError(launch(QNAME + ".flow" + (i+1), null, false));
        }

    }
    
    @Test
    public void parent1() {
        validateFinishPage(launch(QNAME + ".parent", null), true);
    }

    @Test
    public void parent2() {
        validateFinishPage(launch(QNAME + ".sub.parent", null), true);
    }

}
