/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.idgen.ws.rs;


import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.service.external.ExternalIdGeneratorService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/06/2013
 */

@Scope(ScopeType.STATELESS)
@Name("idGenService")
@AutoCreate
public class IdGenService implements IdGenerator {

    public static final String PYTHON_CLASS_NAME = "PythonExternalIdGenerator";

    @Logger
    private Log log;

    @In
    private InumGenerator inumGenerator;
    
    @In
    private ExternalIdGeneratorService externalIdGeneratorService;

    public static IdGenService instance() {
        return ServerUtil.instance(IdGenService.class);
    }

    public String generateId(IdType p_idType, String p_idPrefix) {
        return generateId(p_idType.getType(), p_idPrefix);
    }

    @Override
    public String generateId(String p_idType, String p_idPrefix) {
    	if (externalIdGeneratorService.isEnabled()) {
    		final String generatedId = externalIdGeneratorService.executeExternalDefaultGenerateIdMethod("oxauth", p_idType, p_idPrefix);

    		if (StringHelper.isNotEmpty(generatedId)) {
    			return generatedId;
    		}
    	}
    	
    	return inumGenerator.generateId(p_idType, p_idPrefix);
    }

}
