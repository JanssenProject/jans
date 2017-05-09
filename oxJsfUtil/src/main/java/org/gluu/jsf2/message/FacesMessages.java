package org.gluu.jsf2.message;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 05/06/2017
 */
public class FacesMessages {

	@Inject
	private FacesContext facesContext;

	public void add(Severity severity, String message) {
		facesContext.addMessage(null, new FacesMessage(severity, message, message));
	}

	public void add(Severity severity, String message, Object ... params) {
		
		// TODO: CDI Review
	}

}
