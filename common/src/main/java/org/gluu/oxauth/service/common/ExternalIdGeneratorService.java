package org.gluu.oxauth.service.common;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.id.IdGeneratorType;
import org.gluu.service.custom.script.ExternalScriptService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Map;

/**
 * @author gasmyr on 9/17/20.
 */
@ApplicationScoped
@Named("externalIdGeneratorService")
public class ExternalIdGeneratorService extends ExternalScriptService {

    private static final long serialVersionUID = 1727751544454591273L;

    public ExternalIdGeneratorService() {
        super(CustomScriptType.ID_GENERATOR);
    }

    public String executeExternalGenerateIdMethod(CustomScriptConfiguration customScriptConfiguration, String appId, String idType, String idPrefix) {
        try {
            log.debug("Executing python 'generateId' method");
            IdGeneratorType externalType = (IdGeneratorType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalType.generateId(appId, idType, idPrefix, configurationAttributes);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return null;
    }

    public String executeExternalDefaultGenerateIdMethod(String appId, String idType, String idPrefix) {
        return executeExternalGenerateIdMethod(this.defaultExternalCustomScript, appId, idType, idPrefix);
    }

}
