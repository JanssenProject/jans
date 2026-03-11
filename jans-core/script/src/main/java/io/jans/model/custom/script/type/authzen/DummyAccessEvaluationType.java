package io.jans.model.custom.script.type.authzen;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.authzen.*;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * Dummy implementation of AccessEvaluationType for default behavior.
 *
 * @author Yuriy Z
 */
public class DummyAccessEvaluationType implements AccessEvaluationType {

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }

    @Override
    public AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object context) {
        return AccessEvaluationResponse.FALSE;
    }

    @Override
    public SearchResponse<Subject> searchSubject(SearchSubjectRequest request, Object context) {
        return null; // Not implemented by default
    }

    @Override
    public SearchResponse<Resource> searchResource(SearchResourceRequest request, Object context) {
        return null; // Not implemented by default
    }

    @Override
    public SearchResponse<Action> searchAction(SearchActionRequest request, Object context) {
        return null; // Not implemented by default
    }
}
