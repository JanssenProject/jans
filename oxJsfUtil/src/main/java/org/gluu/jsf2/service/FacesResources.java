package org.gluu.jsf2.service;

import javax.enterprise.context.Dependent;
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

}
