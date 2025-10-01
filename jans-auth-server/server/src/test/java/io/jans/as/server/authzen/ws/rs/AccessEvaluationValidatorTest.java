package io.jans.as.server.authzen.ws.rs;

import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.Action;
import io.jans.model.authzen.Resource;
import io.jans.model.authzen.Subject;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationValidatorTest {

    @InjectMocks
    private AccessEvaluationValidator accessEvaluationValidator;

    @Mock
    private Logger log;

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
    public void validateAccessEvaluationRequest_withActionWithoutName_shouldNotRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setId("456").setType("account"));
        request.setAction(new Action());

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withResourceWithoutId_shouldNotRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setType("account"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccessEvaluationRequest_withResourceWithoutType_shouldNotRaiseError() {
        final AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setSubject(new Subject().setId("23").setType("user"));
        request.setResource(new Resource().setType("account"));
        request.setAction(new Action().setName("can_read"));

        accessEvaluationValidator.validateAccessEvaluationRequest(request);
    }
}
