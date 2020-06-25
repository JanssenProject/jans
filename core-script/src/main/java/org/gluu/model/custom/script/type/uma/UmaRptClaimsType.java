package org.gluu.model.custom.script.type.uma;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface UmaRptClaimsType extends BaseExternalType {

    boolean modify(Object rptAsJsonObject, Object context);
}
