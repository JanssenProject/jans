package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

public class SessionId implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int sessionIdUnusedLifetime;
	private int sessionIdUnauthenticatedUnusedLifetime;
	private Integer sessionIdLifetime ;
	private Boolean sessionIdEnabled;
	private Boolean changeSessionIdOnAuthentication;
	private Boolean sessionIdRequestParameterEnabled;
	private Boolean sessionIdPersistOnPromptNone;
	private Integer serverSessionIdLifetime;
		
	public int getSessionIdUnusedLifetime() {
		return sessionIdUnusedLifetime;
	}

	public void setSessionIdUnusedLifetime(int sessionIdUnusedLifetime) {
		this.sessionIdUnusedLifetime = sessionIdUnusedLifetime;
	}

	public int getSessionIdUnauthenticatedUnusedLifetime() {
		return sessionIdUnauthenticatedUnusedLifetime;
	}
	
	public void setSessionIdUnauthenticatedUnusedLifetime(int sessionIdUnauthenticatedUnusedLifetime) {
		this.sessionIdUnauthenticatedUnusedLifetime = sessionIdUnauthenticatedUnusedLifetime;
	}
	
	public Integer getSessionIdLifetime() {
		return sessionIdLifetime;
	}
	
	public void setSessionIdLifetime(Integer sessionIdLifetime) {
		this.sessionIdLifetime = sessionIdLifetime;
	}
	
	public Boolean getSessionIdEnabled() {
		return sessionIdEnabled;
	}
	
	public void setSessionIdEnabled(Boolean sessionIdEnabled) {
		this.sessionIdEnabled = sessionIdEnabled;
	}
	
	public Boolean getChangeSessionIdOnAuthentication() {
		return changeSessionIdOnAuthentication;
	}
	
	public void setChangeSessionIdOnAuthentication(Boolean changeSessionIdOnAuthentication) {
		this.changeSessionIdOnAuthentication = changeSessionIdOnAuthentication;
	}
	
	public Boolean getSessionIdRequestParameterEnabled() {
		return sessionIdRequestParameterEnabled;
	}
	
	public void setSessionIdRequestParameterEnabled(Boolean sessionIdRequestParameterEnabled) {
		this.sessionIdRequestParameterEnabled = sessionIdRequestParameterEnabled;
	}
	
	public Boolean getSessionIdPersistOnPromptNone() {
		return sessionIdPersistOnPromptNone;
	}
	
	public void setSessionIdPersistOnPromptNone(Boolean sessionIdPersistOnPromptNone) {
		this.sessionIdPersistOnPromptNone = sessionIdPersistOnPromptNone;
	}
	
	public Integer getServerSessionIdLifetime() {
		return serverSessionIdLifetime;
	}
	
	public void setServerSessionIdLifetime(Integer serverSessionIdLifetime) {
		this.serverSessionIdLifetime = serverSessionIdLifetime;
	}

}

