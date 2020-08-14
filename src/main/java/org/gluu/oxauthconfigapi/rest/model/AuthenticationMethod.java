package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AuthenticationMethod implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min=1)
	private String defaultAcr;
	
	@NotBlank
	@Size(min=1)
	private String oxtrustAcr;
	
	public String getDefaultAcr() {
		return defaultAcr;
	}
	
	public void setDefaultAcr(String defaultAcr) {
		this.defaultAcr = defaultAcr;
	}
	
	public String getOxtrustAcr() {
		return oxtrustAcr;
	}
	
	public void setOxtrustAcr(String oxtrustAcr) {
		this.oxtrustAcr = oxtrustAcr;
	}
}
