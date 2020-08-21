package org.gluu.oxauthconfigapi.rest.model;


import java.io.Serializable;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.gluu.oxauth.model.common.GrantType;

public class GrantTypes implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
    @NotEmpty
    @Size(min = 1)
	private Set<GrantType> grantTypesSupported;
    
	public Set<GrantType> getGrantTypesSupported() {
		return grantTypesSupported;
	}
	
	
	public void setGrantTypesSupported(Set<GrantType> grantTypesSupported) {
		this.grantTypesSupported = grantTypesSupported;
	}
	
}
