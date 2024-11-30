package io.jans.model.custom.script.type.authzen;

import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.AccessEvaluationResponse;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface AccessEvaluationType extends BaseExternalType {

    AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object context);
}
