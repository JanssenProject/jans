/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.i18n;

import io.jans.jsf2.i18n.ExtendedResourceBundle;

/**
 * Custom i18n resource loader
 *
 * @author Yuriy Movchan
 * @version 02/23/2018
 */
public class CustomResourceBundle extends ExtendedResourceBundle {

    private static final String BASE_NAME = "oxauth";
    
    @Override
    public String getBaseName() {
        return BASE_NAME;
    }

}
