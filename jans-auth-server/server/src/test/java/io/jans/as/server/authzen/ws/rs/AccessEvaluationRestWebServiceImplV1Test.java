package io.jans.as.server.authzen.ws.rs;

import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.as.model.error.ErrorResponseFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationRestWebServiceImplV1Test {

    @InjectMocks
    private AccessEvaluationRestWebServiceImplV1 accessEvaluationRestWebServiceImplV1;

    @Mock
    private Logger log;

    @Mock
    private AccessEvaluationService accessEvaluationService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void readRequest_withValidRequest_shouldNotRaiseException() {
        final AccessEvaluationRequest request = accessEvaluationRestWebServiceImplV1.readRequest("{\"subject\": {\"id\": \"subject-id\"}}");
        assertEquals("subject-id", request.getSubject().getId());
    }

    @Test
    public void readRequest_withInvalidRequest_shouldRaiseException() {
        try {
            accessEvaluationRestWebServiceImplV1.readRequest("{invalid json}");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
            return;
        }

        fail("400 WebApplicationException was not thrown.");
    }
}
