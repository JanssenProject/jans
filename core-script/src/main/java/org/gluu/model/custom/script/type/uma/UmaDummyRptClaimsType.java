package org.gluu.model.custom.script.type.uma;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaDummyRptClaimsType implements UmaRptClaimsType {

    @Override
    public boolean modify(Object rptAsJsonObject, Object context) {
        return false;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }
}
