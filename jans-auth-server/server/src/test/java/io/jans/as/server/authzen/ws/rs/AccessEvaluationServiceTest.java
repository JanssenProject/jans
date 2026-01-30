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
 * Test coverage for evaluation semantics:
 * +---------------------------+----------------------+---------+
 * | Semantic                  | Scenario             | Results |
 * +---------------------------+----------------------+---------+
 * | permit_on_first_permit    | 1st is permit        | 1       |
 * | permit_on_first_permit    | deny, deny, permit   | 3       |
 * | permit_on_first_permit    | all deny             | 3       |
 * | deny_on_first_deny        | 1st is deny          | 1       |
 * | deny_on_first_deny        | permit, deny         | 2       |
 * | deny_on_first_deny        | all permit           | 3       |
 * | execute_all               | any                  | 3       |
 * +---------------------------+----------------------+---------+
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

    // ==================== mergeWithDefaults Tests ====================

    @Test
    public void mergeWithDefaults_whenEvalHasSubject_shouldUseEvalSubject() {
        AccessEvaluationRequest batchRequest = new AccessEvaluationRequest();
        batchRequest.setSubject(new Subject().setType("user").setId("default-user"));
        batchRequest.setResource(new Resource().setType("resource").setId("default-resource"));

        AccessEvaluationRequest evalRequest = new AccessEvaluationRequest();
        evalRequest.setSubject(new Subject().setType("user").setId("specific-user"));

        AccessEvaluationRequest merged = accessEvaluationService.mergeWithDefaults(evalRequest, batchRequest);

        assertEquals(merged.getSubject().getId(), "specific-user");
        assertEquals(merged.getResource().getId(), "default-resource");
    }

    @Test
    public void mergeWithDefaults_whenEvalHasNoSubject_shouldUseDefaultSubject() {
        AccessEvaluationRequest batchRequest = new AccessEvaluationRequest();
        batchRequest.setSubject(new Subject().setType("user").setId("default-user"));
        batchRequest.setResource(new Resource().setType("resource").setId("default-resource"));
        batchRequest.setAction(new Action().setName("default-action"));

        AccessEvaluationRequest evalRequest = new AccessEvaluationRequest();

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

    // ==================== getEvaluationsSemantic Tests ====================

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

    // ==================== EXECUTE_ALL Tests ====================

    @Test
    public void executeAll_mixedResults_returns3Results() {
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

        // No reason should be added for execute_all
        for (AccessEvaluationResponse evalResponse : response.getEvaluations()) {
            if (evalResponse.getContext() != null) {
                assertNull(evalResponse.getContext().getReason());
            }
        }
    }

    // ==================== PERMIT_ON_FIRST_PERMIT Tests ====================

    @Test
    public void permitOnFirstPermit_firstIsPermit_returns1Result() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(new AccessEvaluationResponse(true, null))
                .thenReturn(new AccessEvaluationResponse(true, null))
                .thenReturn(new AccessEvaluationResponse(true, null));

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 1);
        assertTrue(response.getEvaluations().get(0).isDecision());

        assertNotNull(response.getEvaluations().get(0).getContext());
        assertEquals(response.getEvaluations().get(0).getContext().getReason(), EvaluationOptions.PERMIT_ON_FIRST_PERMIT);
    }

    @Test
    public void permitOnFirstPermit_denyDenyPermit_returns3Results() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(new AccessEvaluationResponse(false, null))
                .thenReturn(new AccessEvaluationResponse(false, null))
                .thenReturn(new AccessEvaluationResponse(true, null));

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 3);
        assertFalse(response.getEvaluations().get(0).isDecision());
        assertFalse(response.getEvaluations().get(1).isDecision());
        assertTrue(response.getEvaluations().get(2).isDecision());

        // Only last response should have reason
        assertNull(response.getEvaluations().get(0).getContext());
        assertNull(response.getEvaluations().get(1).getContext());
        assertNotNull(response.getEvaluations().get(2).getContext());
        assertEquals(response.getEvaluations().get(2).getContext().getReason(), EvaluationOptions.PERMIT_ON_FIRST_PERMIT);
    }

    @Test
    public void permitOnFirstPermit_allDeny_returns3Results() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(new AccessEvaluationResponse(false, null))
                .thenReturn(new AccessEvaluationResponse(false, null))
                .thenReturn(new AccessEvaluationResponse(false, null));

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 3);
        assertFalse(response.getEvaluations().get(0).isDecision());
        assertFalse(response.getEvaluations().get(1).isDecision());
        assertFalse(response.getEvaluations().get(2).isDecision());

        // No reason - no short-circuit happened
        for (AccessEvaluationResponse evalResponse : response.getEvaluations()) {
            if (evalResponse.getContext() != null) {
                assertNull(evalResponse.getContext().getReason());
            }
        }
    }

    // ==================== DENY_ON_FIRST_DENY Tests ====================

    @Test
    public void denyOnFirstDeny_firstIsDeny_returns1Result() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.DENY_ON_FIRST_DENY);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(new AccessEvaluationResponse(false, null))
                .thenReturn(new AccessEvaluationResponse(false, null))
                .thenReturn(new AccessEvaluationResponse(false, null));

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 1);
        assertFalse(response.getEvaluations().get(0).isDecision());

        assertNotNull(response.getEvaluations().get(0).getContext());
        assertEquals(response.getEvaluations().get(0).getContext().getReason(), EvaluationOptions.DENY_ON_FIRST_DENY);
    }

    @Test
    public void denyOnFirstDeny_permitDeny_returns2Results() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.DENY_ON_FIRST_DENY);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(new AccessEvaluationResponse(true, null))
                .thenReturn(new AccessEvaluationResponse(false, null));

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 2);
        assertTrue(response.getEvaluations().get(0).isDecision());
        assertFalse(response.getEvaluations().get(1).isDecision());

        // Only last response should have reason
        assertNull(response.getEvaluations().get(0).getContext());
        assertNotNull(response.getEvaluations().get(1).getContext());
        assertEquals(response.getEvaluations().get(1).getContext().getReason(), EvaluationOptions.DENY_ON_FIRST_DENY);
    }

    @Test
    public void denyOnFirstDeny_allPermit_returns3Results() {
        AccessEvaluationRequest batchRequest = createBatchRequest(EvaluationOptions.DENY_ON_FIRST_DENY);

        when(externalAccessEvaluationService.externalEvaluate(any(), any()))
                .thenReturn(new AccessEvaluationResponse(true, null))
                .thenReturn(new AccessEvaluationResponse(true, null))
                .thenReturn(new AccessEvaluationResponse(true, null));

        ExecutionContext context = new ExecutionContext(null, null);
        AccessEvaluationsResponse response = accessEvaluationService.evaluations(batchRequest, context);

        assertEquals(response.getEvaluations().size(), 3);
        assertTrue(response.getEvaluations().get(0).isDecision());
        assertTrue(response.getEvaluations().get(1).isDecision());
        assertTrue(response.getEvaluations().get(2).isDecision());

        // No reason - no short-circuit happened
        for (AccessEvaluationResponse evalResponse : response.getEvaluations()) {
            if (evalResponse.getContext() != null) {
                assertNull(evalResponse.getContext().getReason());
            }
        }
    }

    // ==================== addShortCircuitReason Tests ====================

    @Test
    public void addShortCircuitReason_contextIsNull_createsContextWithReason() {
        AccessEvaluationResponse response = new AccessEvaluationResponse(false, null);
        assertNull(response.getContext());

        accessEvaluationService.addShortCircuitReason(response, EvaluationOptions.DENY_ON_FIRST_DENY);

        assertNotNull(response.getContext());
        assertEquals(response.getContext().getReason(), EvaluationOptions.DENY_ON_FIRST_DENY);
    }

    @Test
    public void addShortCircuitReason_contextExists_preservesExistingDataAndAddsReason() {
        AccessEvaluationResponseContext existingContext = new AccessEvaluationResponseContext();
        existingContext.setId("existing-id");
        AccessEvaluationResponse response = new AccessEvaluationResponse(true, existingContext);

        accessEvaluationService.addShortCircuitReason(response, EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        assertNotNull(response.getContext());
        assertEquals(response.getContext().getId(), "existing-id");
        assertEquals(response.getContext().getReason(), EvaluationOptions.PERMIT_ON_FIRST_PERMIT);
    }

    // ==================== Helper Methods ====================

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
