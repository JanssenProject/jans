package org.gluu.jsf2.service;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

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
