/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Scope(ScopeType.STATELESS)
@Name("customScriptService")
@AutoCreate
public class CustomScriptService extends AbstractCustomScriptService{

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return ConfigurationFactory.getBaseDn().getScripts();
    }

}
