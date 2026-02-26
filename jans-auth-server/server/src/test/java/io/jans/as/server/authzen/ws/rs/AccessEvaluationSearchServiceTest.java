package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAccessEvaluationService;
import io.jans.model.authzen.*;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for AccessEvaluationSearchService.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationSearchServiceTest {

    @InjectMocks
    private AccessEvaluationSearchService accessEvaluationSearchService;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ExternalAccessEvaluationService externalAccessEvaluationService;

    @Mock
    private AccessEvaluationValidator accessEvaluationValidator;

    @BeforeMethod
    public void setUp() {
        lenient().doNothing().when(errorResponseFactory).validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);
        // By default, validation passes (lenient to avoid unnecessary stubbing errors)
        lenient().doNothing().when(accessEvaluationValidator).validateSearchSubjectRequest(any());
        lenient().doNothing().when(accessEvaluationValidator).validateSearchResourceRequest(any());
        lenient().doNothing().when(accessEvaluationValidator).validateSearchActionRequest(any());
    }

    private WebApplicationException badRequestException(String message) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(message).build());
    }

    // ========== searchSubject tests ==========

    @Test
    public void searchSubject_withValidRequest_shouldReturnResults() {
        SearchSubjectRequest request = createValidSearchSubjectRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        List<Subject> subjects = Arrays.asList(
                new Subject().setId("user1").setType("user"),
                new Subject().setId("user2").setType("user")
        );
        SearchResponse<Subject> expectedResponse = new SearchResponse<>(subjects, new PageResponse("", 2, 2));

        when(externalAccessEvaluationService.externalSearchSubject(any(), any())).thenReturn(expectedResponse);

        SearchResponse<Subject> response = accessEvaluationSearchService.searchSubject(request, context);

        assertNotNull(response);
        assertEquals(response.getResults().size(), 2);
        assertEquals(response.getResults().get(0).getId(), "user1");
        assertEquals(response.getResults().get(1).getId(), "user2");
    }

    @Test
    public void searchSubject_whenExternalServiceReturnsNull_shouldReturnEmptyResponse() {
        SearchSubjectRequest request = createValidSearchSubjectRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        when(externalAccessEvaluationService.externalSearchSubject(any(), any())).thenReturn(null);

        SearchResponse<Subject> response = accessEvaluationSearchService.searchSubject(request, context);

        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());
        assertNotNull(response.getPage());
        assertEquals(response.getPage().getCount(), Integer.valueOf(0));
        assertEquals(response.getPage().getTotal(), Integer.valueOf(0));
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchSubject_withNullRequest_shouldThrowException() {
        doThrow(badRequestException("Request is null"))
                .when(accessEvaluationValidator).validateSearchSubjectRequest(null);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchSubject(null, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchSubject_withMissingSubjectType_shouldThrowException() {
        SearchSubjectRequest request = new SearchSubjectRequest()
                .setSubject(new Subject().setId("ignored"))  // id is ignored, type is required
                .setResource(new Resource().setId("doc1").setType("document"))
                .setAction(new Action().setName("read"));

        doThrow(badRequestException("Subject type is required"))
                .when(accessEvaluationValidator).validateSearchSubjectRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchSubject(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchSubject_withMissingResource_shouldThrowException() {
        SearchSubjectRequest request = new SearchSubjectRequest()
                .setSubject(new Subject().setType("user"))
                .setAction(new Action().setName("read"));

        doThrow(badRequestException("Resource is required"))
                .when(accessEvaluationValidator).validateSearchSubjectRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchSubject(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchSubject_withMissingAction_shouldThrowException() {
        SearchSubjectRequest request = new SearchSubjectRequest()
                .setSubject(new Subject().setType("user"))
                .setResource(new Resource().setId("doc1").setType("document"));

        doThrow(badRequestException("Action is required"))
                .when(accessEvaluationValidator).validateSearchSubjectRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchSubject(request, context);
    }

    // ========== searchResource tests ==========

    @Test
    public void searchResource_withValidRequest_shouldReturnResults() {
        SearchResourceRequest request = createValidSearchResourceRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        List<Resource> resources = Arrays.asList(
                new Resource().setId("doc1").setType("document"),
                new Resource().setId("doc2").setType("document")
        );
        SearchResponse<Resource> expectedResponse = new SearchResponse<>(resources, new PageResponse("", 2, 2));

        when(externalAccessEvaluationService.externalSearchResource(any(), any())).thenReturn(expectedResponse);

        SearchResponse<Resource> response = accessEvaluationSearchService.searchResource(request, context);

        assertNotNull(response);
        assertEquals(response.getResults().size(), 2);
        assertEquals(response.getResults().get(0).getId(), "doc1");
        assertEquals(response.getResults().get(1).getId(), "doc2");
    }

    @Test
    public void searchResource_whenExternalServiceReturnsNull_shouldReturnEmptyResponse() {
        SearchResourceRequest request = createValidSearchResourceRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        when(externalAccessEvaluationService.externalSearchResource(any(), any())).thenReturn(null);

        SearchResponse<Resource> response = accessEvaluationSearchService.searchResource(request, context);

        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());
        assertNotNull(response.getPage());
        assertEquals(response.getPage().getCount(), Integer.valueOf(0));
        assertEquals(response.getPage().getTotal(), Integer.valueOf(0));
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchResource_withNullRequest_shouldThrowException() {
        doThrow(badRequestException("Request is null"))
                .when(accessEvaluationValidator).validateSearchResourceRequest(null);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchResource(null, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchResource_withMissingSubject_shouldThrowException() {
        SearchResourceRequest request = new SearchResourceRequest()
                .setResource(new Resource().setType("document"))
                .setAction(new Action().setName("read"));

        doThrow(badRequestException("Subject is required"))
                .when(accessEvaluationValidator).validateSearchResourceRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchResource(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchResource_withMissingResourceType_shouldThrowException() {
        SearchResourceRequest request = new SearchResourceRequest()
                .setSubject(new Subject().setId("user1").setType("user"))
                .setResource(new Resource().setId("ignored"))  // id is ignored, type is required
                .setAction(new Action().setName("read"));

        doThrow(badRequestException("Resource type is required"))
                .when(accessEvaluationValidator).validateSearchResourceRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchResource(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchResource_withMissingAction_shouldThrowException() {
        SearchResourceRequest request = new SearchResourceRequest()
                .setSubject(new Subject().setId("user1").setType("user"))
                .setResource(new Resource().setType("document"));

        doThrow(badRequestException("Action is required"))
                .when(accessEvaluationValidator).validateSearchResourceRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchResource(request, context);
    }

    // ========== searchAction tests ==========

    @Test
    public void searchAction_withValidRequest_shouldReturnResults() {
        SearchActionRequest request = createValidSearchActionRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        List<Action> actions = Arrays.asList(
                new Action().setName("read"),
                new Action().setName("write"),
                new Action().setName("delete")
        );
        SearchResponse<Action> expectedResponse = new SearchResponse<>(actions, new PageResponse("", 3, 3));

        when(externalAccessEvaluationService.externalSearchAction(any(), any())).thenReturn(expectedResponse);

        SearchResponse<Action> response = accessEvaluationSearchService.searchAction(request, context);

        assertNotNull(response);
        assertEquals(response.getResults().size(), 3);
        assertEquals(response.getResults().get(0).getName(), "read");
        assertEquals(response.getResults().get(1).getName(), "write");
        assertEquals(response.getResults().get(2).getName(), "delete");
    }

    @Test
    public void searchAction_whenExternalServiceReturnsNull_shouldReturnEmptyResponse() {
        SearchActionRequest request = createValidSearchActionRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        when(externalAccessEvaluationService.externalSearchAction(any(), any())).thenReturn(null);

        SearchResponse<Action> response = accessEvaluationSearchService.searchAction(request, context);

        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());
        assertNotNull(response.getPage());
        assertEquals(response.getPage().getCount(), Integer.valueOf(0));
        assertEquals(response.getPage().getTotal(), Integer.valueOf(0));
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchAction_withNullRequest_shouldThrowException() {
        doThrow(badRequestException("Request is null"))
                .when(accessEvaluationValidator).validateSearchActionRequest(null);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchAction(null, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchAction_withMissingSubject_shouldThrowException() {
        SearchActionRequest request = new SearchActionRequest()
                .setResource(new Resource().setId("doc1").setType("document"));

        doThrow(badRequestException("Subject is required"))
                .when(accessEvaluationValidator).validateSearchActionRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchAction(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchAction_withMissingResource_shouldThrowException() {
        SearchActionRequest request = new SearchActionRequest()
                .setSubject(new Subject().setId("user1").setType("user"));

        doThrow(badRequestException("Resource is required"))
                .when(accessEvaluationValidator).validateSearchActionRequest(request);

        ExecutionContext context = new ExecutionContext(null, null);
        accessEvaluationSearchService.searchAction(request, context);
    }

    // ========== Feature flag tests ==========

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchSubject_whenFeatureDisabled_shouldThrowException() {
        doThrow(new WebApplicationException()).when(errorResponseFactory)
                .validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        SearchSubjectRequest request = createValidSearchSubjectRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        accessEvaluationSearchService.searchSubject(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchResource_whenFeatureDisabled_shouldThrowException() {
        doThrow(new WebApplicationException()).when(errorResponseFactory)
                .validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        SearchResourceRequest request = createValidSearchResourceRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        accessEvaluationSearchService.searchResource(request, context);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void searchAction_whenFeatureDisabled_shouldThrowException() {
        doThrow(new WebApplicationException()).when(errorResponseFactory)
                .validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        SearchActionRequest request = createValidSearchActionRequest();
        ExecutionContext context = new ExecutionContext(null, null);

        accessEvaluationSearchService.searchAction(request, context);
    }

    // ========== Helper methods ==========

    private SearchSubjectRequest createValidSearchSubjectRequest() {
        return new SearchSubjectRequest()
                .setSubject(new Subject().setType("user"))  // id is ignored for search
                .setResource(new Resource().setId("doc1").setType("document"))
                .setAction(new Action().setName("read"));
    }

    private SearchResourceRequest createValidSearchResourceRequest() {
        return new SearchResourceRequest()
                .setSubject(new Subject().setId("user1").setType("user"))
                .setResource(new Resource().setType("document"))  // id is ignored for search
                .setAction(new Action().setName("read"));
    }

    private SearchActionRequest createValidSearchActionRequest() {
        return new SearchActionRequest()
                .setSubject(new Subject().setId("user1").setType("user"))
                .setResource(new Resource().setId("doc1").setType("document"));
    }
}
