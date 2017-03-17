package org.gluu.jsf2.service;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * @author Yuriy Movchan
 * @version 03/17/2017
 */
public class FacesResources {

	@Produces
	@RequestScoped
	public FacesContext getFacesContext() {
		return FacesContext.getCurrentInstance();
	}

	@Produces
	@RequestScoped
	public ExternalContext getExternalContext() {
		FacesContext facesContext = getFacesContext();
		if (facesContext != null) {
			return facesContext.getExternalContext();
		}
		
		return null;
	}

}
