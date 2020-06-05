package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;
/**
 * @author Puja Sharma
 *
 */
public class ResponseType implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@NotEmpty
	private String code;
	private String displayName;

	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}	
	
}
