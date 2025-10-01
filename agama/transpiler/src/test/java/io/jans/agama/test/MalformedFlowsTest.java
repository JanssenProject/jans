package io.jans.agama.test;

import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

public class MalformedFlowsTest {
    
    private Logger logger = LogManager.getLogger(getClass());
    
    @Test
    public void test() {

        Map<String, String> map = new TreeMap<>(FlowUtil.sourcesOfFolder("target/test-classes/fail"));
        //Sonar check likes Map.Entry instead of nicer ways to iterate like map.keySet()
        for (Map.Entry<String, String> entry: map.entrySet()) {
            String file = entry.getKey();

            try {
                logger.info("Checking syntax of '{}'", file);
                Transpiler.runSyntaxCheck(entry.getValue());

                fail(file + " is expected to have errors but it passed validation");
            } catch(SyntaxException | TranspilerException e) {
                String msg = e.getMessage();
                logger.info("{}\n", msg);
            }
        }
        logger.info("{} files examined", map.size());
    }

}
