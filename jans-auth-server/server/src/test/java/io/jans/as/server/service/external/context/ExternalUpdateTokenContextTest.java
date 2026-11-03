package io.jans.as.server.service.external.context;

import io.jans.as.server.model.common.ExecutionContext;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ExternalUpdateTokenContextTest {

    @Test
    public void putTokenAttribute_whenExecutionContextSet_shouldStoreInExecutionContext() {
        ExecutionContext executionContext = new ExecutionContext();
        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext();
        context.setExecutionContext(executionContext);

        context.putTokenAttribute("ujwt", "value");

        assertEquals(executionContext.getAttribute("ujwt"), "value");
        assertEquals(context.getTokenAttribute("ujwt"), "value");
    }

    @Test
    public void putTokenAttribute_whenExecutionContextIsNull_shouldDoNothing() {
        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext();

        context.putTokenAttribute("ujwt", "value");

        assertNull(context.getTokenAttribute("ujwt"));
    }

    @Test
    public void putTokenAttribute_whenKeyOrValueIsNull_shouldNotStore() {
        ExecutionContext executionContext = new ExecutionContext();
        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext();
        context.setExecutionContext(executionContext);

        context.putTokenAttribute(null, "value");
        context.putTokenAttribute("ujwt", null);

        assertTrue(executionContext.getAttributes().isEmpty());
    }
}
