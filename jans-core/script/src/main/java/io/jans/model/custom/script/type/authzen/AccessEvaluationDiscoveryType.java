package io.jans.model.custom.script.type.authzen;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface AccessEvaluationDiscoveryType extends BaseExternalType {

    boolean modifyResponse(Object responseAsJsonObject, Object context);
}
