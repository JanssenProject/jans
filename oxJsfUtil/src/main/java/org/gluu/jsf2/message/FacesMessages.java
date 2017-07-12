package org.gluu.jsf2.message;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 05/06/2017
 */
@RequestScoped
public class FacesMessages implements Serializable {

	private static final long serialVersionUID = -6408439483194578659L;

	@Inject
	private FacesContext facesContext;

	@Inject
	private ExternalContext externalContext;

	public void add(Severity severity, String message) {
		facesContext.addMessage(null, new FacesMessage(severity, message, message));
	}

	public void add(String clientId, Severity severity, String message) {
		facesContext.addMessage(clientId, new FacesMessage(severity, message, message));
	}

	public void add(Severity severity, String message, Object ... params) {
		// TODO: CDI Review. Add params to message
		add(severity, message);
		externalContext.getFlash().setKeepMessages(true);
	}

	public void setKeepMessages() {
		externalContext.getFlash().setKeepMessages(true);
	}

}
