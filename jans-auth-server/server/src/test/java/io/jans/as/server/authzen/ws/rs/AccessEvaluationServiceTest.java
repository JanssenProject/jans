package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAccessEvaluationService;
import io.jans.model.authzen.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Tests for AccessEvaluationService batch evaluation logic.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationServiceTest {

    @InjectMocks
    private AccessEvaluationService accessEvaluationService;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ExternalAccessEvaluationService externalAccessEvaluationService;

    @Mock
    private AccessEvaluationValidator accessEvaluationValidator;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void mergeWithDefaults_whenEvalHasSubject_shouldUseEvalSubject() {
        AccessEvaluationRequest batchRequest = new AccessEvaluationRequest();
        batchRequest.setSubject(new Subject().setType("user").setId("default-user"));
        batchRequest.setResource(new Resource().setType("resource").setId("default-resource"));

        AccessEvaluationRequest evalRequest = new AccessEvaluationRequest();
        evalRequest.setSubject(new Subject().setType("user").setId("specific-user"));

        AccessEvaluationRequest merged = accessEvaluationService.mergeWithDefaults(evalRequest, batchRequest);

        assertEquals(merged.getSubject().getId(), "specific-user");
        assertEquals(merged.getResource().getId(), "default-resource"); // Uses default
    }

    @Test
    public void mergeWithDefaults_whenEvalHasNoSubject_shouldUseDefaultSubject() {
        AccessEvaluationRequest batchRequest = new AccessEvaluationRequest();
        batchRequest.setSubject(new Subject().setType("user").setId("default-user"));
        batchRequest.setResource(new Resource().setType("resource").setId("default-resource"));
        batchRequest.setAction(new Action().setName("default-action"));

        AccessEvaluationRequest evalRequest = new AccessEvaluationRequest();
        // No subject set

        AccessEvaluationRequest merged = accessEvaluationService.mergeWithDefaults(evalRequest, batchRequest);

        assertEquals(merged.getSubject().getId(), "default-user");
        assertEquals(merged.getResource().getId(), "default-resource");
        assertEquals(merged.getAction().getName(), "default-action");
    }

    @Test
    public void mergeWithDefaults_whenEvalHasAction_shouldUseEvalAction() {
        AccessEvaluationRequest batchRequest = new AccessEvaluationRequest();
        batchRequest.setAction(new Action().setName("default-action"));

        AccessEvaluationRequest evalRequest = new AccessEvaluationRequest();
        evalRequest.setAction(new Action().setName("specific-action"));

        AccessEvaluationRequest merged = accessEvaluationService.mergeWithDefaults(evalRequest, batchRequest);

        assertEquals(merged.getAction().getName(), "specific-action");
    }

    @Test
    public void getEvaluationsSemantic_whenOptionsNull_shouldReturnExecuteAll() {
        String semantic = accessEvaluationService.getEvaluationsSemantic(null);
        assertEquals(semantic, EvaluationOptions.EXECUTE_ALL);
    }

    @Test
    public void getEvaluationsSemantic_whenSemanticBlank_shouldReturnExecuteAll() {
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic("");

        String semantic = accessEvaluationService.getEvaluationsSemantic(options);
        assertEquals(semantic, EvaluationOptions.EXECUTE_ALL);
    }

    @Test
    public void getEvaluationsSemantic_whenSemanticSet_shouldReturnThatSemantic() {
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.DENY_ON_FIRST_DENY);

        String semantic = accessEvaluationService.getEvaluationsSemantic(options);
        assertEquals(semantic, EvaluationOptions.DENY_ON_FIRST_DENY);
    }

    @Test
    public void evaluations_withExecuteAll_shouldEvaluateAllRequests() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.EXECUTE_ALL);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(AccessEvaluationResponse.TRUE)
                .thenReturn(AccessEvaluationResponse.FALSE)
                .thenReturn(AccessEvaluationResponse.TRUE);

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 3);
        assertTrue(response.getEvaluations().get(0).isDecision());
        assertFalse(response.getEvaluations().get(1).isDecision());
        assertTrue(response.getEvaluations().get(2).isDecision());
    }

    @Test
    public void evaluations_withDenyOnFirstDeny_shouldStopOnFirstDeny() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.DENY_ON_FIRST_DENY);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(AccessEvaluationResponse.TRUE)
                .thenReturn(AccessEvaluationResponse.FALSE); // Should stop here

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        // Should have only 2 results, stopped at first deny
        assertEquals(response.getEvaluations().size(), 2);
        assertTrue(response.getEvaluations().get(0).isDecision());
        assertFalse(response.getEvaluations().get(1).isDecision());
    }

    @Test
    public void evaluations_withPermitOnFirstPermit_shouldStopOnFirstPermit() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(AccessEvaluationResponse.TRUE); // Should stop immediately

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        // Should have only 1 result, stopped at first permit
        assertEquals(response.getEvaluations().size(), 1);
        assertTrue(response.getEvaluations().get(0).isDecision());
    }

    private AccessEvaluationRequest createBatchRequest(String semantic) {
        AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setType("user").setId("test-user"));
        request.setResource(new Resource().setType("resource").setId("test-resource"));

        AccessEvaluationRequest eval1 = new AccessEvaluationRequest();
        eval1.setAction(new Action().setName("action1"));
        AccessEvaluationRequest eval2 = new AccessEvaluationRequest();
        eval2.setAction(new Action().setName("action2"));
        AccessEvaluationRequest eval3 = new AccessEvaluationRequest();
        eval3.setAction(new Action().setName("action3"));

        request.setEvaluations(Arrays.asList(eval1, eval2, eval3));

        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(semantic);
        request.setOptions(options);

        return request;
    }
}
