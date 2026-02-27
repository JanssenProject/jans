package io.jans.as.server.authzen.ws.rs;

import io.jans.model.authzen.*;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * Tests for AccessEvaluationValidator.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationValidatorTest {

    @InjectMocks
    private AccessEvaluationValidator accessEvaluationValidator;

    @Mock
    private Logger log;

    // ========== Single Evaluation Tests ==========

    @Test
    public void validateAccessEvaluationRequest_withValidRequest_shouldNotRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setId("456").setType("account"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withoutSubject_shouldRaiseError() {
        accessEvaluationValidator.validateAccessEvaluationRequest(new AccessEvaluationRequest());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withSubjectWithoutId_shouldRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setType("user"));
        request.setResource(new Resource().setId("456").setType("account"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(new AccessEvaluationRequest());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withSubjectWithoutType_shouldRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23"));
        request.setResource(new Resource().setId("456").setType("account"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(new AccessEvaluationRequest());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withActionWithoutName_shouldRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setId("456").setType("account"));
        request.setAction(new Action());

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withResourceWithoutId_shouldRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setType("account"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withResourceWithoutType_shouldRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setId("456"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }

    // ========== Batch Evaluations Tests ==========

    @Test
    public void validateAccessEvaluationsRequest_withValidBatchRequest_shouldNotRaiseError() {
        AccessEvaluationRequest request = new AccessEvaluationRequest();
        AccessEvaluationRequest eval = new AccessEvaluationRequest();
        eval.setSubject(new Subject().setId("1").setType("user"));
        eval.setResource(new Resource().setId("2").setType("resource"));
        eval.setAction(new Action().setName("read"));
        request.setEvaluations(Collections.singletonList(eval));

        accessEvaluationValidator.validateAccessEvaluationsRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationsRequest_withNullRequest_shouldRaiseError() {
        accessEvaluationValidator.validateAccessEvaluationsRequest(null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationsRequest_withEmptyEvaluations_shouldRaiseError() {
        AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setEvaluations(Collections.emptyList());

        accessEvaluationValidator.validateAccessEvaluationsRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationsRequest_withNullEvaluations_shouldRaiseError() {
        AccessEvaluationRequest request = new AccessEvaluationRequest();
        // evaluations is null

        accessEvaluationValidator.validateAccessEvaluationsRequest(request);
    }

    @Test
    public void validateEvaluationOptions_withValidExecuteAll_shouldNotRaiseError() {
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.EXECUTE_ALL);

        accessEvaluationValidator.validateEvaluationOptions(options);
    }

    @Test
    public void validateEvaluationOptions_withValidDenyOnFirstDeny_shouldNotRaiseError() {
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.DENY_ON_FIRST_DENY);

        accessEvaluationValidator.validateEvaluationOptions(options);
    }

    @Test
    public void validateEvaluationOptions_withValidPermitOnFirstPermit_shouldNotRaiseError() {
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        accessEvaluationValidator.validateEvaluationOptions(options);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateEvaluationOptions_withInvalidSemantic_shouldRaiseError() {
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic("invalid_semantic");

        accessEvaluationValidator.validateEvaluationOptions(options);
    }

    // ========== Search Subject Tests ==========

    @Test
    public void validateSearchSubjectRequest_withValidRequest_shouldNotRaiseError() {
        SearchSubjectRequest request = new SearchSubjectRequest();
        request.setSubject(new Subject().setType("user")); // id not required for search
        request.setResource(new Resource().setId("1").setType("resource"));
        request.setAction(new Action().setName("read"));

        accessEvaluationValidator.validateSearchSubjectRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSearchSubjectRequest_withoutSubjectType_shouldRaiseError() {
        SearchSubjectRequest request = new SearchSubjectRequest();
        request.setSubject(new Subject()); // No type
        request.setResource(new Resource().setId("1").setType("resource"));
        request.setAction(new Action().setName("read"));

        accessEvaluationValidator.validateSearchSubjectRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSearchSubjectRequest_withNullRequest_shouldRaiseError() {
        accessEvaluationValidator.validateSearchSubjectRequest(null);
    }

    // ========== Search Resource Tests ==========

    @Test
    public void validateSearchResourceRequest_withValidRequest_shouldNotRaiseError() {
        SearchResourceRequest request = new SearchResourceRequest();
        request.setSubject(new Subject().setId("1").setType("user"));
        request.setResource(new Resource().setType("document")); // id not required for search
        request.setAction(new Action().setName("read"));

        accessEvaluationValidator.validateSearchResourceRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSearchResourceRequest_withoutResourceType_shouldRaiseError() {
        SearchResourceRequest request = new SearchResourceRequest();
        request.setSubject(new Subject().setId("1").setType("user"));
        request.setResource(new Resource()); // No type
        request.setAction(new Action().setName("read"));

        accessEvaluationValidator.validateSearchResourceRequest(request);
    }

    // ========== Search Action Tests ==========

    @Test
    public void validateSearchActionRequest_withValidRequest_shouldNotRaiseError() {
        SearchActionRequest request = new SearchActionRequest();
        request.setSubject(new Subject().setId("1").setType("user"));
        request.setResource(new Resource().setId("1").setType("resource"));
        // No action field per spec

        accessEvaluationValidator.validateSearchActionRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSearchActionRequest_withNullRequest_shouldRaiseError() {
        accessEvaluationValidator.validateSearchActionRequest(null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSearchActionRequest_withoutSubject_shouldRaiseError() {
        SearchActionRequest request = new SearchActionRequest();
        request.setResource(new Resource().setId("1").setType("resource"));

        accessEvaluationValidator.validateSearchActionRequest(request);
    }
}
