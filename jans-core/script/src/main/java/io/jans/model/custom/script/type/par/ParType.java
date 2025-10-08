package io.jans.model.custom.script.type.par;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface ParType extends BaseExternalType {


    // par - io.jans.as.persistence.model.Par
    boolean createPar(Object par, Object context);

    boolean modifyParResponse(Object responseAsJsonObject, Object context);
}
