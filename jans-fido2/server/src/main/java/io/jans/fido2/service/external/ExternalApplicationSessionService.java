/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.external;

import io.jans.fido2.service.external.session.SessionEvent;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.session.ApplicationSessionType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Provides factory methods needed to create external application session extension
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalApplicationSessionService extends ExternalScriptService {

    private static final long serialVersionUID = 2316361273036208685L;

    public ExternalApplicationSessionService() {
        super(CustomScriptType.APPLICATION_SESSION);
    }

    public void externalEvent(SessionEvent event) {
        if (!isEnabled()) {
            return;
        }

        for (CustomScriptConfiguration scriptConfiguration : this.customScriptConfigurations) {
            externalEvent(scriptConfiguration, event);
        }
    }

    private void externalEvent(CustomScriptConfiguration scriptConfiguration, SessionEvent event) {
        try {
            log.trace("Executing python 'onEvent' method of script: {}, event: {}", scriptConfiguration.getName(), event);
            event.setScriptConfiguration(scriptConfiguration);
            ApplicationSessionType applicationSessionType = (ApplicationSessionType) scriptConfiguration.getExternalType();
            applicationSessionType.onEvent(event);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
        }
    }


}
