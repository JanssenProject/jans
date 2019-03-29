/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.custom;

import java.io.UnsupportedEncodingException;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Stateless
@Named
@ApplicationScoped
public class CustomScriptService extends AbstractCustomScriptService {
	
	@Inject
	private StaticConfiguration staticConfiguration;

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScripts();
    }

    public String base64Decode(String encoded) throws IllegalArgumentException, UnsupportedEncodingException {
        byte[] decoded = Base64Util.base64urldecode(encoded);
        return new String(decoded, "UTF-8");
    }

}
