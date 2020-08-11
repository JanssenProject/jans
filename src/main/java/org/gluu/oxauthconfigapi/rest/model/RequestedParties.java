package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class RequestedParties  implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min=1)
	private String name;
	
	@NotEmpty
	@Size(min=1)
	private List<String> domains;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getDomains() {
		return domains;
	}

	public void setDomains(List<String> domains) {
		this.domains = domains;
	}
}
