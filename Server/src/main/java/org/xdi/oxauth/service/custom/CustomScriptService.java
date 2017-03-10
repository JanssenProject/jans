/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom;

import javax.enterprise.context.ApplicationScoped;
import org.jboss.seam.annotations.AutoCreate;
import javax.inject.Inject;
import javax.inject.Named;
import org.jboss.seam.annotations.Scope;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Stateless
@Named("customScriptService")
@AutoCreate
public class CustomScriptService extends AbstractCustomScriptService{
	
	@Inject
	private StaticConf staticConfiguration;

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScripts();
    }

}
