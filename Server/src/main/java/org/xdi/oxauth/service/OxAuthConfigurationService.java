/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.web.ServletContexts;
import org.xdi.oxauth.model.config.ConfigurationFactory;
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

	public String getCssLocation(){
		if(StringHelper.isEmpty(ConfigurationFactory.instance().getConfiguration().getCssLocation())){
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/stylesheet";
		}else{
			return ConfigurationFactory.instance().getConfiguration().getCssLocation();
		}	
	}
	
	public String getJsLocation(){
		if(StringHelper.isEmpty(ConfigurationFactory.instance().getConfiguration().getJsLocation())){
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/js";
		}else{
			return ConfigurationFactory.instance().getConfiguration().getJsLocation();
		}	
	}
	
	public String getImgLocation(){
		if(StringHelper.isEmpty(ConfigurationFactory.instance().getConfiguration().getImgLocation())){
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/img";
		}else{
			return ConfigurationFactory.instance().getConfiguration().getImgLocation();
		}	
	}
	
}

