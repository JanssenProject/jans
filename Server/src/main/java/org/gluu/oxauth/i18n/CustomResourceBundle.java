package org.gluu.oxauth.i18n;

import org.gluu.jsf2.i18n.ExtendedResourceBundle;

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
