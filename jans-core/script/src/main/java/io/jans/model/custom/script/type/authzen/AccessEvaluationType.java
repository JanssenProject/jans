package io.jans.model.custom.script.type.authzen;

import io.jans.model.authzen.*;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * AuthZEN Access Evaluation script interface.
 * Provides methods for evaluation and search operations.
 *
 * @author Yuriy Z
 */
public interface AccessEvaluationType extends BaseExternalType {

    /**
     * Single evaluation.
     */
    AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object context);

    /**
     * Search for authorized subjects.
     * @return SearchResponse with Subject results, or null if not implemented
     */
    SearchResponse<Subject> searchSubject(SearchSubjectRequest request, Object context);

    /**
     * Search for authorized resources.
     * @return SearchResponse with Resource results, or null if not implemented
     */
    SearchResponse<Resource> searchResource(SearchResourceRequest request, Object context);

    /**
     * Search for authorized actions.
     * @return SearchResponse with Action results, or null if not implemented
     */
    SearchResponse<Action> searchAction(SearchActionRequest request, Object context);
}
