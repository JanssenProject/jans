package io.jans.as.server.service;
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

/**
 * OxAuthConfigurationService
 *
 * @author Oleksiy Tataryn Date: 08.07.2014
 */
@ApplicationScoped
@Named
public class WebConfigurationService {

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
