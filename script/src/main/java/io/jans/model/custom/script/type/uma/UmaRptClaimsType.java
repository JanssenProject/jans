package io.jans.model.custom.script.type.uma;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface UmaRptClaimsType extends BaseExternalType {

    boolean modify(Object rptAsJsonObject, Object context);
}
