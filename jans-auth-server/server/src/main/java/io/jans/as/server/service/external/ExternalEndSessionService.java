/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.server.service.external.context.EndSessionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.logout.EndSessionType;
import io.jans.service.custom.script.ExternalScriptService;
import org.apache.commons.lang.StringUtils;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalEndSessionService extends ExternalScriptService {

    public ExternalEndSessionService() {
        super(CustomScriptType.END_SESSION);
    }

    public String getFrontchannelHtml(EndSessionContext context) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            log.trace("There is no any external interception script defined (getFrontchannelHtml).");
            return "";
        }

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            final String html = getFrontchannelHtml(script, context);
            if (StringUtils.isNotBlank(html)) {
                return html;
            }
        }

        return null;
    }

    private String getFrontchannelHtml(CustomScriptConfiguration scriptConf, EndSessionContext context) {
        try {
            log.trace("Executing external 'getFrontchannelHtml' method, script name: {}, context: {}", scriptConf.getName(), context);
            EndSessionType script = (EndSessionType) scriptConf.getExternalType();
            context.setScript(scriptConf);

            final String html = script.getFrontchannelHtml(context);
            log.trace("Finished external 'getFrontchannelHtml' method, script name: {}, context {}, html: {}", scriptConf.getName(), context, html);

            return html;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConf.getCustomScript(), ex);
            return null;
        }
    }
}
