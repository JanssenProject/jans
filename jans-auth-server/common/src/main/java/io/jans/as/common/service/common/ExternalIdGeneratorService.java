/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.id.IdGeneratorType;
import io.jans.service.custom.script.ExternalScriptService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
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
