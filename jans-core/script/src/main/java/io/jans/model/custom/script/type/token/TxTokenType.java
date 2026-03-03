package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

public interface TxTokenType extends BaseExternalType {

    int getTxTokenLifetimeInSeconds(Object context);

    boolean modifyTokenPayload(Object jsonWebResponse, Object context);

    boolean modifyResponse(Object responseAsJsonObject, Object context);
}
