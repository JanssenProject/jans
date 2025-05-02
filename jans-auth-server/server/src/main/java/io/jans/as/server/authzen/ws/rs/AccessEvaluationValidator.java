package io.jans.as.server.authzen.ws.rs;

import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.Action;
import io.jans.model.authzen.Resource;
import io.jans.model.authzen.Subject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class AccessEvaluationValidator {

    @Inject
    private Logger log;

    public void validateAccessEvaluationRequest(AccessEvaluationRequest request) {
        validateSubject(request.getSubject());
        validateResource(request.getResource());
        validateAction(request.getAction());
    }

    public void validateSubject(Subject subject) {
        if (subject == null) {
            final String msg = "Invalid subject. Subject is not set";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        if (StringUtils.isBlank(subject.getId())) {
            final String msg = "Invalid subject. Subject id can't be blank";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        if (StringUtils.isBlank(subject.getType())) {
            final String msg = "Invalid subject. Subject type can't be blank";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

    public void validateAction(Action action) {
        if (action == null) {
            final String msg = "Invalid action. Action is not set";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        if (StringUtils.isBlank(action.getName())) {
            final String msg = "Invalid action. Action id can't be blank";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

    public void validateResource(Resource resource) {
        if (resource == null) {
            final String msg = "Invalid resource. Resource is not set";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        if (StringUtils.isBlank(resource.getId())) {
            final String msg = "Invalid resource. Resource id can't be blank";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        if (StringUtils.isBlank(resource.getType())) {
            final String msg = "Invalid resource. Resource type can't be blank";
            log.trace(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }
}
