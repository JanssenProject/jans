/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.web.ServletContexts;
import org.xdi.oxauth.model.configuration.Configuration;
import org.xdi.util.StringHelper;

/**
 * OxAuthConfigurationService
 *
 * @author Oleksiy Tataryn Date: 08.07.2014
 */
@Scope(ScopeType.STATELESS)
@Name("oxAuthConfigurationService")
@AutoCreate
public class OxAuthConfigurationService {

	@In(value = "#{configurationFactory.configuration}")
	private Configuration configuration;

	public String getCssLocation() {
		if (StringHelper.isEmpty(configuration.getCssLocation())) {
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/stylesheet";
		} else {
			return configuration.getCssLocation();
		}
	}

	public String getJsLocation() {
		if (StringHelper.isEmpty(configuration.getJsLocation())) {
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/js";
		} else {
			return configuration.getJsLocation();
		}
	}

	public String getImgLocation() {
		if (StringHelper.isEmpty(configuration.getImgLocation())) {
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/img";
		} else {
			return configuration.getImgLocation();
		}
	}

}
