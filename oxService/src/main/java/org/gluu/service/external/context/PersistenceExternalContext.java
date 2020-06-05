/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.service.external.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Holds object required in persistence scope custom scripts 
 * 
 * @author Yuriy Movchan  Date: 06/05/2020
 */

public class PersistenceExternalContext extends ExternalScriptContext {

    public PersistenceExternalContext(HttpServletRequest httpRequest) {
		super(httpRequest, null);
	}

	public PersistenceExternalContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		super(httpRequest, httpResponse);
	}

}
