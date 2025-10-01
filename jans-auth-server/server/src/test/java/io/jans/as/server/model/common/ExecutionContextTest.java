package io.jans.as.server.model.common;

import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import jakarta.faces.context.ExternalContext;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
public class ExecutionContextTest {

    @Test
    public void of_whenExternalContextIsNull_shouldNotFail() {
        assertNotNull(ExecutionContext.of((ExternalContext) null));
        assertNotNull(ExecutionContext.of((AuthzRequest) null));
    }
}
