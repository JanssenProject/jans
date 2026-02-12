package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAccessEvaluationService;
import io.jans.model.authzen.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.util.ArrayList;

/**
 * AuthZEN Search Service.
 * Handles search operations for subjects, resources, and actions.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class AccessEvaluationSearchService {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ExternalAccessEvaluationService externalAccessEvaluationService;

    @Inject
    private AccessEvaluationValidator accessEvaluationValidator;

    /**
     * Search for subjects authorized for a given action on a resource.
     */
    public SearchResponse<Subject> searchSubject(SearchSubjectRequest request, ExecutionContext executionContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        accessEvaluationValidator.validateSearchSubjectRequest(request);

        SearchResponse<Subject> response = externalAccessEvaluationService.externalSearchSubject(request, executionContext);
        if (response == null) {
            response = createEmptyResponse();
        }

        log.debug("Search Subject response {}", response);
        return response;
    }

    /**
     * Search for resources a subject is authorized to access.
     */
    public SearchResponse<Resource> searchResource(SearchResourceRequest request, ExecutionContext executionContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        accessEvaluationValidator.validateSearchResourceRequest(request);

        SearchResponse<Resource> response = externalAccessEvaluationService.externalSearchResource(request, executionContext);
        if (response == null) {
            response = createEmptyResponse();
        }

        log.debug("Search Resource response {}", response);
        return response;
    }

    /**
     * Search for actions a subject is authorized to perform on a resource.
     */
    public SearchResponse<Action> searchAction(SearchActionRequest request, ExecutionContext executionContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        accessEvaluationValidator.validateSearchActionRequest(request);

        SearchResponse<Action> response = externalAccessEvaluationService.externalSearchAction(request, executionContext);
        if (response == null) {
            response = createEmptyResponse();
        }

        log.debug("Search Action response {}", response);
        return response;
    }

    /**
     * Create an empty search response with empty results and pagination.
     */
    private <T> SearchResponse<T> createEmptyResponse() {
        SearchResponse<T> response = new SearchResponse<>();
        response.setResults(new ArrayList<>());
        response.setPage(new PageResponse("", 0, 0));
        return response;
    }
}
