package org.xdi.oxauth.util;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which provides external resources for injection
 *
 * @author Yuriy Movchan Date: 14/03/2017
 */
public class Resources {

	@Produces
	public Logger getLogger(InjectionPoint ip) {
		Class<?> clazz = ip.getMember().getDeclaringClass();

		return LoggerFactory.getLogger(clazz);
	}

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
