/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.custom;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.util.Base64Util;
import io.jans.service.custom.script.AbstractCustomScriptService;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import java.nio.charset.StandardCharsets;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class CustomScriptService extends AbstractCustomScriptService {

    @Inject
    private StaticConfiguration staticConfiguration;

    private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScripts();
    }

    public String base64Decode(String encoded) throws IllegalArgumentException {
        byte[] decoded = Base64Util.base64urldecode(encoded);
        return new String(decoded, StandardCharsets.UTF_8);
    }

}
