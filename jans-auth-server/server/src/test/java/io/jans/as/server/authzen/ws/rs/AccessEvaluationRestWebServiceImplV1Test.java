package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.model.authzen.AccessEvaluationRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Tests for AccessEvaluationRestWebServiceImplV1.
 *
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

    // ========== Single Evaluation Tests ==========

    @Test
    public void readEvaluationRequest_withValidRequest_shouldNotRaiseException() {
        final AccessEvaluationRequest request = accessEvaluationRestWebServiceImplV1.readEvaluationRequest("{\"subject\": {\"id\": \"subject-id\"}}");
        assertEquals("subject-id", request.getSubject().getId());
    }

    @Test
    public void readEvaluationRequest_withInvalidRequest_shouldRaiseException() {
        try {
            accessEvaluationRestWebServiceImplV1.readEvaluationRequest("{invalid json}");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
            return;
        }

        fail("400 WebApplicationException was not thrown.");
    }

    @Test
    public void readEvaluationRequest_withFullRequest_shouldParseAllFields() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"123\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"456\"}," +
                "\"action\": {\"name\": \"read\"}" +
                "}";

        AccessEvaluationRequest request = accessEvaluationRestWebServiceImplV1.readEvaluationRequest(json);

        assertEquals("user", request.getSubject().getType());
        assertEquals("123", request.getSubject().getId());
        assertEquals("document", request.getResource().getType());
        assertEquals("456", request.getResource().getId());
        assertEquals("read", request.getAction().getName());
    }

    // ========== Batch Evaluations Tests ==========

    @Test
    public void readEvaluationRequest_withBatchRequest_shouldParseBatchFields() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"default-user\"}," +
                "\"evaluations\": [" +
                "  {\"action\": {\"name\": \"read\"}}," +
                "  {\"action\": {\"name\": \"write\"}}" +
                "]" +
                "}";

        AccessEvaluationRequest request = accessEvaluationRestWebServiceImplV1.readEvaluationRequest(json);

        assertNotNull(request.getSubject());
        assertEquals("default-user", request.getSubject().getId());
        assertNotNull(request.getEvaluations());
        assertEquals(2, request.getEvaluations().size());
    }

    @Test
    public void readEvaluationRequest_withOptions_shouldParseOptions() {
        String json = "{" +
                "\"evaluations\": [{\"subject\": {\"type\": \"u\", \"id\": \"1\"}, \"resource\": {\"type\": \"r\", \"id\": \"1\"}, \"action\": {\"name\": \"a\"}}]," +
                "\"options\": {\"evaluations_semantic\": \"deny_on_first_deny\"}" +
                "}";

        AccessEvaluationRequest request = accessEvaluationRestWebServiceImplV1.readEvaluationRequest(json);

        assertNotNull(request.getOptions());
        assertEquals("deny_on_first_deny", request.getOptions().getEvaluationsSemantic());
    }
}
