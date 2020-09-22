/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.idgen.ws.rs;


import org.gluu.oxauth.model.common.IdType;
import org.gluu.oxauth.service.common.ExternalIdGeneratorService;
import org.gluu.oxauth.service.common.api.IdGenerator;
import org.gluu.util.StringHelper;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/06/2013
 */
@Stateless
@Named("idGenService")
public class IdGenService implements IdGenerator {

    @Inject
    private InumGenerator inumGenerator;
    
    @Inject
    private ExternalIdGeneratorService externalIdGeneratorService;

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
