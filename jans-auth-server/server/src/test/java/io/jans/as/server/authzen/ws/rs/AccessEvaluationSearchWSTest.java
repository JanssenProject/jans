package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.model.authzen.SearchActionRequest;
import io.jans.model.authzen.SearchResourceRequest;
import io.jans.model.authzen.SearchSubjectRequest;
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
 * Tests for AccessEvaluationSearchWS.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationSearchWSTest {

    @InjectMocks
    private AccessEvaluationSearchWS accessEvaluationSearchWS;

    @Mock
    private Logger log;

    @Mock
    private AccessEvaluationSearchService accessEvaluationSearchService;

    @Mock
    private AccessEvaluationService accessEvaluationService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    // ========== readSearchSubjectRequest Tests ==========

    @Test
    public void readSearchSubjectRequest_withValidRequest_shouldParseSuccessfully() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc-123\"}," +
                "\"action\": {\"name\": \"read\"}" +
                "}";

        SearchSubjectRequest request = accessEvaluationSearchWS.readSearchSubjectRequest(json);

        assertNotNull(request);
        assertEquals("user", request.getSubject().getType());
        assertEquals("document", request.getResource().getType());
        assertEquals("doc-123", request.getResource().getId());
        assertEquals("read", request.getAction().getName());
    }

    @Test
    public void readSearchSubjectRequest_withContext_shouldParseContext() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc-123\"}," +
                "\"action\": {\"name\": \"read\"}," +
                "\"context\": {\"ip\": \"192.168.1.1\"}" +
                "}";

        SearchSubjectRequest request = accessEvaluationSearchWS.readSearchSubjectRequest(json);

        assertNotNull(request);
        assertNotNull(request.getContext());
        assertEquals("192.168.1.1", request.getContext().get("ip"));
    }

    @Test
    public void readSearchSubjectRequest_withPagination_shouldParsePage() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc-123\"}," +
                "\"action\": {\"name\": \"read\"}," +
                "\"page\": {\"size\": 10, \"token\": \"next-page-token\"}" +
                "}";

        SearchSubjectRequest request = accessEvaluationSearchWS.readSearchSubjectRequest(json);

        assertNotNull(request);
        assertNotNull(request.getPage());
    }

    @Test
    public void readSearchSubjectRequest_withInvalidJson_shouldThrowBadRequest() {
        try {
            accessEvaluationSearchWS.readSearchSubjectRequest("{invalid json}");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
            return;
        }

        fail("400 WebApplicationException was not thrown.");
    }

    // ========== readSearchResourceRequest Tests ==========

    @Test
    public void readSearchResourceRequest_withValidRequest_shouldParseSuccessfully() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"user-123\"}," +
                "\"resource\": {\"type\": \"document\"}," +
                "\"action\": {\"name\": \"read\"}" +
                "}";

        SearchResourceRequest request = accessEvaluationSearchWS.readSearchResourceRequest(json);

        assertNotNull(request);
        assertEquals("user", request.getSubject().getType());
        assertEquals("user-123", request.getSubject().getId());
        assertEquals("document", request.getResource().getType());
        assertEquals("read", request.getAction().getName());
    }

    @Test
    public void readSearchResourceRequest_withSubjectProperties_shouldParseProperties() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"user-123\", \"properties\": {\"role\": \"admin\"}}," +
                "\"resource\": {\"type\": \"document\"}," +
                "\"action\": {\"name\": \"read\"}" +
                "}";

        SearchResourceRequest request = accessEvaluationSearchWS.readSearchResourceRequest(json);

        assertNotNull(request);
        assertNotNull(request.getSubject().getProperties());
    }

    @Test
    public void readSearchResourceRequest_withInvalidJson_shouldThrowBadRequest() {
        try {
            accessEvaluationSearchWS.readSearchResourceRequest("{not valid json");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
            return;
        }

        fail("400 WebApplicationException was not thrown.");
    }

    // ========== readSearchActionRequest Tests ==========

    @Test
    public void readSearchActionRequest_withValidRequest_shouldParseSuccessfully() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"user-123\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc-456\"}" +
                "}";

        SearchActionRequest request = accessEvaluationSearchWS.readSearchActionRequest(json);

        assertNotNull(request);
        assertEquals("user", request.getSubject().getType());
        assertEquals("user-123", request.getSubject().getId());
        assertEquals("document", request.getResource().getType());
        assertEquals("doc-456", request.getResource().getId());
    }

    @Test
    public void readSearchActionRequest_withContext_shouldParseContext() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"user-123\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc-456\"}," +
                "\"context\": {\"timestamp\": \"2024-01-01T00:00:00Z\"}" +
                "}";

        SearchActionRequest request = accessEvaluationSearchWS.readSearchActionRequest(json);

        assertNotNull(request);
        assertNotNull(request.getContext());
    }

    @Test
    public void readSearchActionRequest_withPagination_shouldParsePage() {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"user-123\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc-456\"}," +
                "\"page\": {\"size\": 25}" +
                "}";

        SearchActionRequest request = accessEvaluationSearchWS.readSearchActionRequest(json);

        assertNotNull(request);
        assertNotNull(request.getPage());
    }

    @Test
    public void readSearchActionRequest_withInvalidJson_shouldThrowBadRequest() {
        try {
            accessEvaluationSearchWS.readSearchActionRequest("[]");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
            return;
        }

        fail("400 WebApplicationException was not thrown.");
    }

    // ========== Empty/Minimal Request Tests ==========

    @Test
    public void readSearchSubjectRequest_withEmptyObject_shouldParseSuccessfully() {
        SearchSubjectRequest request = accessEvaluationSearchWS.readSearchSubjectRequest("{}");
        assertNotNull(request);
        assertNull(request.getSubject());
        assertNull(request.getResource());
        assertNull(request.getAction());
    }

    @Test
    public void readSearchResourceRequest_withEmptyObject_shouldParseSuccessfully() {
        SearchResourceRequest request = accessEvaluationSearchWS.readSearchResourceRequest("{}");
        assertNotNull(request);
        assertNull(request.getSubject());
        assertNull(request.getResource());
        assertNull(request.getAction());
    }

    @Test
    public void readSearchActionRequest_withEmptyObject_shouldParseSuccessfully() {
        SearchActionRequest request = accessEvaluationSearchWS.readSearchActionRequest("{}");
        assertNotNull(request);
        assertNull(request.getSubject());
        assertNull(request.getResource());
    }
}
