package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface TokenExchangeType extends BaseExternalType {

    boolean modifyResponse(Object context);

    void validate(Object context);

    boolean skipBuiltinValidation(Object context);
}
