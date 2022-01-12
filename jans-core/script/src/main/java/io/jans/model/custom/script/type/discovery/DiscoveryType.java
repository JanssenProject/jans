package io.jans.model.custom.script.type.discovery;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface DiscoveryType extends BaseExternalType {

    boolean modifyResponse(Object responseAsJsonObject, Object context);
}
