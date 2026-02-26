package io.jans.as.server.authzen.ws.rs;

import io.jans.model.authzen.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * AuthZEN request validator.
 * Validates single evaluation, batch evaluations, and search requests.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class AccessEvaluationValidator {

    @Inject
    private Logger log;

    /**
     * Validate single evaluation request.
     */
    public void validateAccessEvaluationRequest(AccessEvaluationRequest request) {
        if (request == null) {
            throw badRequest("Invalid request. Request is null");
        }

        validateSubject(request.getSubject());
        validateResource(request.getResource());
        validateAction(request.getAction());
    }

    /**
     * Validate batch evaluations request.
     */
    public void validateAccessEvaluationsRequest(AccessEvaluationRequest request) {
        if (request == null) {
            throw badRequest("Invalid batch request. Request is null");
        }

        List<AccessEvaluationRequest> evaluations = request.getEvaluations();
        if (evaluations == null || evaluations.isEmpty()) {
            throw badRequest("Invalid batch request. Evaluations array is empty or not set");
        }

        // Validate options if present
        if (request.getOptions() != null) {
            validateEvaluationOptions(request.getOptions());
        }
    }

    /**
     * Validate evaluation options.
     */
    public void validateEvaluationOptions(EvaluationOptions options) {
        String semantic = options.getEvaluationsSemantic();
        if (StringUtils.isNotBlank(semantic)) {
            if (!EvaluationOptions.EXECUTE_ALL.equals(semantic) &&
                !EvaluationOptions.DENY_ON_FIRST_DENY.equals(semantic) &&
                !EvaluationOptions.PERMIT_ON_FIRST_PERMIT.equals(semantic)) {
                throw badRequest("Invalid evaluations_semantic value: " + semantic +
                        ". Must be one of: execute_all, deny_on_first_deny, permit_on_first_permit");
            }
        }
    }

    /**
     * Validate search subject request.
     */
    public void validateSearchSubjectRequest(SearchSubjectRequest request) {
        if (request == null) {
            throw badRequest("Invalid search subject request. Request is null");
        }

        // Subject type is required, id is ignored per spec
        if (request.getSubject() == null || StringUtils.isBlank(request.getSubject().getType())) {
            throw badRequest("Invalid search subject request. Subject type is required");
        }

        validateResource(request.getResource());
        validateAction(request.getAction());
    }

    /**
     * Validate search resource request.
     */
    public void validateSearchResourceRequest(SearchResourceRequest request) {
        if (request == null) {
            throw badRequest("Invalid search resource request. Request is null");
        }

        validateSubject(request.getSubject());
        validateAction(request.getAction());

        // Resource type is required, id is ignored per spec
        if (request.getResource() == null || StringUtils.isBlank(request.getResource().getType())) {
            throw badRequest("Invalid search resource request. Resource type is required");
        }
    }

    /**
     * Validate search action request.
     * Note: Per spec, action field is omitted for action search.
     */
    public void validateSearchActionRequest(SearchActionRequest request) {
        if (request == null) {
            throw badRequest("Invalid search action request. Request is null");
        }

        validateSubject(request.getSubject());
        validateResource(request.getResource());
    }

    public void validateSubject(Subject subject) {
        if (subject == null) {
            throw badRequest("Invalid subject. Subject is not set");
        }

        if (StringUtils.isBlank(subject.getId())) {
            throw badRequest("Invalid subject. Subject id can't be blank");
        }

        if (StringUtils.isBlank(subject.getType())) {
            throw badRequest("Invalid subject. Subject type can't be blank");
        }
    }

    public void validateAction(Action action) {
        if (action == null) {
            throw badRequest("Invalid action. Action is not set");
        }

        if (StringUtils.isBlank(action.getName())) {
            throw badRequest("Invalid action. Action name can't be blank");
        }
    }

    public void validateResource(Resource resource) {
        if (resource == null) {
            throw badRequest("Invalid resource. Resource is not set");
        }

        if (StringUtils.isBlank(resource.getId())) {
            throw badRequest("Invalid resource. Resource id can't be blank");
        }

        if (StringUtils.isBlank(resource.getType())) {
            throw badRequest("Invalid resource. Resource type can't be blank");
        }
    }

    private WebApplicationException badRequest(String msg) {
        log.trace(msg);
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(msg)
                .type(MediaType.APPLICATION_JSON_TYPE).build());
    }
}
