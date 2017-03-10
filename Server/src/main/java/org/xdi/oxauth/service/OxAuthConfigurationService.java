/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import javax.enterprise.context.ApplicationScoped;
import org.jboss.seam.annotations.AutoCreate;
import javax.inject.Inject;
import javax.inject.Named;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.web.ServletContexts;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.util.StringHelper;

/**
 * OxAuthConfigurationService
 *
 * @author Oleksiy Tataryn Date: 08.07.2014
 */
@Stateless
@Named("oxAuthConfigurationService")
@AutoCreate
public class OxAuthConfigurationService {

	@Inject
	private AppConfiguration appConfiguration;

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
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/js";
		} else {
			return appConfiguration.getJsLocation();
		}
	}

	public String getImgLocation() {
		if (StringHelper.isEmpty(appConfiguration.getImgLocation())) {
			String contextPath = ServletContexts.instance().getRequest().getContextPath();
			return contextPath + "/img";
		} else {
			return appConfiguration.getImgLocation();
		}
	}

}
