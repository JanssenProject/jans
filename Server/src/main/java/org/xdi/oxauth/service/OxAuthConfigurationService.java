/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.gluu.util.StringHelper;
import org.xdi.oxauth.model.configuration.AppConfiguration;

/**
 * OxAuthConfigurationService
 *
 * @author Oleksiy Tataryn Date: 08.07.2014
 */
@ApplicationScoped
@Named
@Deprecated //TODO: We don't need this class
public class OxAuthConfigurationService {

	@Inject
	private AppConfiguration appConfiguration;
	
	@Inject
	private ServletContext context;

	public String getCssLocation() {
		if (StringHelper.isEmpty(appConfiguration.getCssLocation())) {
			FacesContext ctx = FacesContext.getCurrentInstance();
			if (ctx == null) {
				return "";
			}
			String contextPath = ctx.getExternalContext().getRequestContextPath();
			return contextPath + "/stylesheet";
		} else {
			return appConfiguration.getCssLocation();
		}
	}

	public String getJsLocation() {
		if (StringHelper.isEmpty(appConfiguration.getJsLocation())) {
			String contextPath = context.getContextPath();
			return contextPath + "/js";
		} else {
			return appConfiguration.getJsLocation();
		}
	}

	public String getImgLocation() {
		if (StringHelper.isEmpty(appConfiguration.getImgLocation())) {
			String contextPath = context.getContextPath();
			return contextPath + "/img";
		} else {
			return appConfiguration.getImgLocation();
		}
	}

}
