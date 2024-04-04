package io.jans.model.custom.script.type.health;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface HealthCheckType extends BaseExternalType {

    /**
     * @param context script context
     * @return health check response
     */
    String healthCheck(Object context);
}
