package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface TokenExchangeType extends BaseExternalType {

    ScriptTokenExchangeControl validate(Object context);

    boolean modifyResponse(Object responseAsJsonObject, Object context);
}
