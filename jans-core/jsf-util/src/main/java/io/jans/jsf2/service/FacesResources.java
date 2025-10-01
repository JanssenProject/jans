/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.service;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

/**
 * @author Yuriy Movchan
 * @version 03/17/2017
 */
@Dependent
public class FacesResources {

    @Produces
    @Dependent
    public FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    @Produces
    @Dependent
    public ExternalContext getExternalContext() {
        FacesContext facesContext = getFacesContext();
        if (facesContext != null) {
            return facesContext.getExternalContext();
        }

        return null;
    }

    @Produces
    @Dependent
    public ViewHandler getViewHandler() {
        FacesContext facesContext = getFacesContext();
        if (facesContext != null) {
            return facesContext.getApplication().getViewHandler();
        }

        return null;
    }

}
